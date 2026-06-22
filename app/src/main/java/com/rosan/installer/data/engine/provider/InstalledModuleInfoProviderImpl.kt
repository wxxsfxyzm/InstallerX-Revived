// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.provider

import com.rosan.installer.domain.device.model.ShizukuMode
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.engine.model.packageinfo.InstalledModuleInfo
import com.rosan.installer.domain.engine.provider.InstalledModuleInfoProvider
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.config.ConfigModel
import com.rosan.installer.domain.settings.model.preferences.RootMode
import com.rosan.installer.framework.privileged.util.SHELL_ROOT
import com.rosan.installer.framework.privileged.util.SU_ARGS
import com.rosan.installer.framework.privileged.util.UserServiceUidMode
import com.rosan.installer.framework.privileged.util.useUserService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import timber.log.Timber

class InstalledModuleInfoProviderImpl(
    private val capabilityProvider: DeviceCapabilityProvider,
    private val json: Json
) : InstalledModuleInfoProvider {
    override suspend fun list(config: ConfigModel, rootMode: RootMode): List<InstalledModuleInfo> =
        withContext(Dispatchers.IO) {
            val command = moduleListCommand(rootMode) ?: return@withContext emptyList()

            val output = try {
                when (config.authorizer) {
                    Authorizer.Root,
                    Authorizer.Customize -> executeLocal(config, command)

                    Authorizer.Shizuku -> executeShizukuRoot(config, command)

                    else -> null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.d(e, "Module list query failed")
                null
            }

            output?.let { parseModuleList(it) }.orEmpty()
        }

    private fun moduleListCommand(rootMode: RootMode): Array<String>? =
        when (rootMode) {
            RootMode.KernelSU -> arrayOf("ksud", "module", "list")
            RootMode.APatch -> arrayOf("apd", "module", "list")
            RootMode.Magisk,
            RootMode.None -> null
        }

    private fun executeLocal(config: ConfigModel, command: Array<String>): String? {
        val shellParts = if (config.authorizer == Authorizer.Customize && config.customizeAuthorizer.isNotBlank()) {
            config.customizeAuthorizer.trim().split("\\s+".toRegex())
        } else {
            listOf(SHELL_ROOT, SU_ARGS)
        }

        val escapedCommand = command.joinToString(" ") { "'" + it.replace("'", "'\\''") + "'" }
        val process = ProcessBuilder(shellParts + listOf("-c", escapedCommand))
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()
        return output.takeIf { exitCode == 0 }
    }

    private fun executeShizukuRoot(config: ConfigModel, command: Array<String>): String? {
        if (capabilityProvider.shizukuModeFlow.value != ShizukuMode.ROOT ||
            !capabilityProvider.shizukuAuthorizedFlow.value
        ) {
            return null
        }

        var result: String? = null
        useUserService(
            isSystemApp = capabilityProvider.isSystemApp,
            authorizer = config.authorizer,
            uidMode = UserServiceUidMode.SystemIfRoot
        ) { userService ->
            result = userService.privileged.execArr(command)
        }
        return result
    }

    private fun parseModuleList(raw: String): List<InstalledModuleInfo> =
        try {
            json.decodeFromString<List<InstalledModuleInfoDto>>(raw)
                .mapNotNull { it.toDomain() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.d(e, "Failed to parse module list")
            emptyList()
        }

    @Serializable
    private data class InstalledModuleInfoDto(
        val id: String? = null,
        val name: String? = null,
        val version: String? = null,
        @Serializable(with = NullableLongStringSerializer::class)
        val versionCode: Long? = null,
        val author: String? = null,
        val description: String? = null,
        @Serializable(with = NullableBooleanStringSerializer::class)
        val enabled: Boolean? = null,
        @Serializable(with = NullableBooleanStringSerializer::class)
        val remove: Boolean? = null,
        @Serializable(with = NullableBooleanStringSerializer::class)
        val update: Boolean? = null,
        @Serializable(with = NullableBooleanStringSerializer::class)
        val web: Boolean? = null,
        @Serializable(with = NullableBooleanStringSerializer::class)
        val action: Boolean? = null,
        @Serializable(with = NullableBooleanStringSerializer::class)
        val mount: Boolean? = null,
        val updateJson: String? = null
    ) {
        fun toDomain(): InstalledModuleInfo? {
            val moduleId = id?.takeIf { it.isNotBlank() } ?: return null
            return InstalledModuleInfo(
                id = moduleId,
                name = name,
                version = version,
                versionCode = versionCode,
                author = author,
                description = description,
                enabled = enabled,
                remove = remove,
                update = update,
                web = web,
                action = action,
                mount = mount,
                updateJson = updateJson
            )
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private object NullableLongStringSerializer : KSerializer<Long?> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("NullableLongString", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): Long? {
            val primitive = decoder.jsonPrimitiveOrNull() ?: return null
            return primitive.longOrNull ?: primitive.contentOrNull?.toLongOrNull()
        }

        override fun serialize(encoder: Encoder, value: Long?) {
            if (value == null) encoder.encodeNull() else encoder.encodeLong(value)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private object NullableBooleanStringSerializer : KSerializer<Boolean?> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("NullableBooleanString", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): Boolean? {
            val primitive = decoder.jsonPrimitiveOrNull() ?: return null
            return primitive.booleanOrNull ?: when (primitive.contentOrNull?.lowercase()) {
                "true" -> true
                "false" -> false
                else -> null
            }
        }

        override fun serialize(encoder: Encoder, value: Boolean?) {
            if (value == null) encoder.encodeNull() else encoder.encodeBoolean(value)
        }
    }

}

private fun Decoder.jsonPrimitiveOrNull(): JsonPrimitive? =
    try {
        (this as? JsonDecoder)?.decodeJsonElement() as? JsonPrimitive
    } catch (_: SerializationException) {
        null
    }
