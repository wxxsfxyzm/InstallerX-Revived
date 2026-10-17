// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.usecase

import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.engine.model.install.SessionMode
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.packageinfo.PackageAnalysisResult
import com.rosan.installer.domain.engine.model.packageinfo.PackageIdentityStatus
import com.rosan.installer.domain.engine.model.packageinfo.SignatureMatchStatus
import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.engine.model.source.DataType
import com.rosan.installer.domain.engine.repository.AppInstallerRepository
import com.rosan.installer.domain.engine.repository.ModuleInstallerRepository
import com.rosan.installer.domain.history.repository.OperationHistoryRepository
import com.rosan.installer.domain.history.usecase.RecordOperationHistoryUseCase
import com.rosan.installer.domain.privileged.model.PostInstallTaskInfo
import com.rosan.installer.domain.privileged.provider.PostInstallTaskProvider
import com.rosan.installer.domain.session.model.ProgressEntity
import com.rosan.installer.domain.session.model.SelectInstallEntity
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.config.ConfigModel
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import java.io.IOException
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

class ModuleSourceDeletionTest {
    @Test
    fun `successful installation deletes original archive before reporting success`() = runTest {
        val fixture = Fixture()
        val progress = fixture.install(config())

        assertEquals(listOf("installed", "delete", "success"), fixture.events)
        assertEquals(listOf("/original/module.zip"), fixture.tasks.single().deletePaths)
        assertTrue(fixture.tasks.single().enableAutoDelete)
        assertFalse(fixture.tasks.single().enableDexopt)
        assertEquals(ProgressEntity.InstallSuccess, progress.last())
    }

    @Test
    fun `deletion requires both profile switches`() = runTest {
        for ((autoDelete, autoDeleteZip) in listOf(false to false, false to true, true to false)) {
            val fixture = Fixture()
            fixture.install(config().copy(autoDelete = autoDelete, autoDeleteZip = autoDeleteZip))
            assertTrue(fixture.tasks.isEmpty())
        }
    }

    @Test
    fun `failed or cancelled installation retains original archive`() = runTest {
        for (failure in listOf(IOException("install failed"), CancellationException("cancelled"))) {
            val fixture = Fixture(installFailure = failure)
            assertFailsWith<Exception> { fixture.install(config()) }
            assertTrue(fixture.tasks.isEmpty())
            assertFalse("success" in fixture.events)
        }
    }

    @Test
    fun `deletion failure does not turn successful installation into failure`() = runTest {
        val fixture = Fixture(deleteFailure = IOException("delete failed"))
        assertEquals(ProgressEntity.InstallSuccess, fixture.install(config()).last())
    }

    @Test
    fun `cancellation during deletion propagates`() = runTest {
        val fixture = Fixture(deleteFailure = CancellationException("cancelled"))
        assertFailsWith<CancellationException> { fixture.install(config()) }
        assertFalse("success" in fixture.events)
    }

    private fun config() = ConfigModel.default.copy(
        authorizer = Authorizer.Root,
        customizeAuthorizer = "custom",
        autoDelete = true,
        autoDeleteZip = true,
    )

    private class Fixture(
        private val installFailure: Exception? = null,
        private val deleteFailure: Exception? = null,
    ) {
        val events = mutableListOf<String>()
        val tasks = mutableListOf<PostInstallTaskInfo>()

        suspend fun install(config: ConfigModel): List<ProgressEntity> {
            val module = AppEntity.ModuleEntity(
                id = "module",
                name = "Module",
                version = "1",
                versionCode = 1,
                author = "",
                description = "",
                data = DataEntity.FileEntity("/cache/materialized.zip").apply {
                    source = DataEntity.FileEntity("/original/module.zip")
                },
                sourceType = DataType.MODULE_ZIP,
            )
            val useCase = ProcessInstallationUseCase(
                appSettingsRepo = stub<AppSettingsRepository> { method ->
                    when (method) {
                        "getBoolean" -> flowOf(false)
                        "getString" -> flowOf("")
                        else -> error("Unexpected settings call: $method")
                    }
                },
                appInstaller = stub<AppInstallerRepository>(),
                moduleInstaller = stub<ModuleInstallerRepository> {
                    flow {
                        emit("Installing")
                        installFailure?.let { throw it }
                        events.add("installed")
                    }
                },
                capabilityProvider = stub<DeviceCapabilityProvider> { false },
                recordOperationHistory = RecordOperationHistoryUseCase(stub<OperationHistoryRepository>()),
                postInstallTaskProvider = object : PostInstallTaskProvider {
                    override suspend fun executeTasks(authorizer: Authorizer, customizeAuthorizer: String, info: PostInstallTaskInfo) {
                        assertEquals(config.authorizer, authorizer)
                        assertEquals(config.customizeAuthorizer, customizeAuthorizer)
                        events.add("delete")
                        tasks.add(info)
                        deleteFailure?.let { throw it }
                    }
                },
            )
            val result = PackageAnalysisResult(
                packageName = module.packageName,
                sessionMode = SessionMode.Single,
                appEntities = listOf(SelectInstallEntity(module, selected = true)),
                installedAppInfo = null,
                signatureCheckPerformed = false,
                signatureMatchStatus = SignatureMatchStatus.NOT_INSTALLED,
                identityStatus = PackageIdentityStatus.NOT_APPLICABLE,
            )
            return useCase(config, listOf(result)).toList().also { progress ->
                if (progress.lastOrNull() == ProgressEntity.InstallSuccess) events.add("success")
            }
        }
    }

    private companion object {
        @Suppress("UNCHECKED_CAST")
        inline fun <reified T> stub(crossinline handler: (String) -> Any? = { error("Unexpected call: $it") }): T = Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
            handler(method.name)
        } as T
    }
}
