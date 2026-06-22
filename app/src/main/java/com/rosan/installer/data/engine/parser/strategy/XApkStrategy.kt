// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.parser.strategy

import android.graphics.drawable.Drawable
import com.rosan.installer.data.engine.parser.ApkParser
import com.rosan.installer.data.engine.parser.parseSplitMetadata
import com.rosan.installer.data.engine.signature.PendingApkSignatureAnalyzer
import com.rosan.installer.domain.engine.model.AnalyseExtraEntity
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.settings.model.config.ConfigModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.util.zip.ZipFile

class XApkStrategy(
    private val json: Json,
    // Inject ApkParser to handle fallback analysis for Base APK
    private val apkParser: ApkParser,
    private val pendingApkSignatureAnalyzer: PendingApkSignatureAnalyzer
) : AnalysisStrategy {

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun analyze(
        config: ConfigModel,
        data: DataEntity,
        zipFile: ZipFile?,
        extra: AnalyseExtraEntity
    ): List<AppEntity> {
        requireNotNull(zipFile)
        require(data is DataEntity.FileEntity)

        // 1. Parse Manifest
        val manifestEntry = zipFile.getEntry("manifest.json") ?: return emptyList()
        val manifest = withContext(Dispatchers.IO) {
            zipFile.getInputStream(manifestEntry)
        }.use {
            json.decodeFromStream<Manifest>(it)
        }

        // 2. Load Icon (if available)
        val icon = zipFile.getEntry("icon.png")?.let {
            Drawable.createFromStream(zipFile.getInputStream(it), it.name)
        }

        // 3. Map Splits to Entities
        return manifest.splits.flatMap { split ->
            val entryName = split.name
            // Construct the virtual data entity for the entry
            val entryData = DataEntity.ZipFileEntity(entryName, data)

            val file = File(entryName)
            when (file.extension) {
                "apk" -> {
                    val splitName = if (split.splitName == "base") null else split.splitName

                    val entity = if (!splitName.isNullOrEmpty()) {
                        val metadata = splitName.parseSplitMetadata()

                        AppEntity.SplitEntity(
                            packageName = manifest.packageName,
                            data = entryData,
                            splitName = splitName,
                            targetSdk = manifest.targetSdk,
                            minSdk = manifest.minSdk,
                            arch = null,
                            sourceType = extra.dataType,
                            type = metadata.type,
                            filterType = metadata.filterType,
                            configValue = metadata.configValue,
                            signatureInfo = if (extra.checkAppSignature) {
                                pendingApkSignatureAnalyzer.analyze(entryData, extra.cacheDirectory)
                            } else {
                                null
                            }
                        )
                    } else {
                        // Handle Base APK
                        var resolvedLabel = manifest.label
                        var resolvedIcon = icon
                        val signatureInfo = if (extra.checkAppSignature) {
                            pendingApkSignatureAnalyzer.analyze(entryData, extra.cacheDirectory)
                        } else {
                            null
                        }

                        // Fallback: If label is missing from JSON, parse the Base APK fully to extract it
                        if (resolvedLabel.isNullOrBlank()) {
                            val baseEntry = zipFile.getEntry(entryName)
                            if (baseEntry != null) {
                                val parsedEntities = apkParser.parseZipEntryFull(zipFile, baseEntry, data, extra)
                                val parsedBase = parsedEntities.firstOrNull { it is AppEntity.BaseEntity } as? AppEntity.BaseEntity

                                if (parsedBase != null) {
                                    resolvedLabel = parsedBase.label
                                    // Optionally fallback the icon as well if missing from zip root
                                    if (resolvedIcon == null) {
                                        resolvedIcon = parsedBase.icon
                                    }
                                }
                            }
                        }

                        AppEntity.BaseEntity(
                            packageName = manifest.packageName,
                            sharedUserId = null,
                            data = entryData,
                            versionCode = manifest.versionCode,
                            versionName = manifest.versionName,
                            label = resolvedLabel,
                            icon = resolvedIcon,
                            targetSdk = manifest.targetSdk,
                            minSdk = manifest.minSdk,
                            sourceType = extra.dataType,
                            signatureHash = signatureInfo?.primarySha256,
                            signatureInfo = signatureInfo
                        )
                    }
                    listOf(entity)
                }

                "dm" -> {
                    val dmName = file.nameWithoutExtension
                    if (dmName.isNotEmpty()) {
                        listOf(
                            AppEntity.DexMetadataEntity(
                                packageName = manifest.packageName,
                                data = entryData,
                                dmName = dmName,
                                targetSdk = manifest.targetSdk,
                                minSdk = manifest.minSdk,
                                sourceType = extra.dataType
                            )
                        )
                    } else emptyList()
                }

                else -> emptyList()
            }
        }
    }

    @Serializable
    private data class Manifest(
        @SerialName("package_name") val packageName: String,
        @SerialName("version_code") @Serializable(with = FlexibleXapkVersionCodeSerializer::class) val versionCode: Long,
        // Assign default value null to prevent MissingFieldException when the key is missing in JSON
        @SerialName("version_name") val versionNameStr: String? = null,
        // Assign default value null to fix the crash
        @SerialName("name") val label: String? = null,
        @SerialName("split_apks") val splits: List<Split>,
        @SerialName("min_sdk_version") val minSdk: String? = null,
        @SerialName("target_sdk_version") val targetSdk: String? = null,
    ) {
        val versionName: String = versionNameStr ?: ""

        @Serializable
        data class Split(
            @SerialName("file") val name: String,
            @SerialName("id") val splitName: String
        )
    }
}

private object FlexibleXapkVersionCodeSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleXapkVersionCode", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Long) =
        encoder.encodeLong(value)

    override fun deserialize(decoder: Decoder): Long {
        return try {
            decoder.decodeLong()
        } catch (_: Exception) {
            try {
                decoder.decodeString().toLong()
            } catch (e: Exception) {
                throw SerializationException("Expected string or number for XAPK version_code", e)
            }
        }
    }
}
