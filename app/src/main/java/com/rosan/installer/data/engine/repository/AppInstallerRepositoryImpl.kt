// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.data.engine.repository

import android.content.Context
import com.rosan.installer.core.reflection.ReflectionProvider
import com.rosan.installer.data.engine.executor.appInstaller.DhizukuAppInstallerRepoImpl
import com.rosan.installer.data.engine.executor.appInstaller.NoneAppInstallerRepoImpl
import com.rosan.installer.data.engine.executor.appInstaller.ProcessAppInstallerRepoImpl
import com.rosan.installer.data.engine.executor.appInstaller.ShizukuAppInstallerRepoImpl
import com.rosan.installer.data.engine.executor.appInstaller.SystemAppInstallerRepoImpl
import com.rosan.installer.data.privileged.exception.ShizukuNotWorkException
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.engine.model.InstallEntity
import com.rosan.installer.domain.engine.repository.AppInstallerRepository
import com.rosan.installer.domain.privileged.provider.PostInstallTaskProvider
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.ConfigModel

class AppInstallerRepositoryImpl(
    private val context: Context,
    private val reflect: ReflectionProvider,
    private val deviceCapabilityProvider: DeviceCapabilityProvider,
    private val postInstallTaskProvider: PostInstallTaskProvider
) : AppInstallerRepository {
    override suspend fun doInstallWork(
        config: ConfigModel,
        entities: List<InstallEntity>,
        blacklist: List<String>,
        sharedUserIdBlacklist: List<String>,
        sharedUserIdExemption: List<String>
    ) = executeWithRepo(config) { repo ->
        repo.doInstallWork(
            config,
            entities,
            blacklist,
            sharedUserIdBlacklist,
            sharedUserIdExemption
        )
    }

    override suspend fun doUninstallWork(
        config: ConfigModel,
        packageName: String
    ) = executeWithRepo(config) { repo ->
        repo.doUninstallWork(config, packageName)
    }

    override suspend fun approveSession(
        config: ConfigModel,
        sessionId: Int,
        granted: Boolean
    ) = executeWithRepo(config) { repo ->
        repo.approveSession(config, sessionId, granted)
    }

    /**
     * Execute an action with the InstallerRepo based on the provided 
     */
    private suspend fun <T> executeWithRepo(
        config: ConfigModel,
        action: suspend (AppInstallerRepository) -> T
    ): T {
        val repo = resolveRepo(config)

        try {
            return action(repo)
        } catch (e: IllegalStateException) {
            // Check if Shizuku service connection is lost
            if (repo is ShizukuAppInstallerRepoImpl && e.message?.contains("binder haven't been received") == true) {
                throw ShizukuNotWorkException("Shizuku service connection lost during operation.", e)
            }
            // Throw other exceptions as-is
            throw e
        }
    }

    /**
     * Resolve the InstallerRepo based on the provided 
     */
    private fun resolveRepo(config: ConfigModel): AppInstallerRepository {
        return when (config.authorizer) {
            Authorizer.Shizuku -> ShizukuAppInstallerRepoImpl(context, reflect, deviceCapabilityProvider, postInstallTaskProvider)
            Authorizer.Dhizuku -> DhizukuAppInstallerRepoImpl(context, reflect, deviceCapabilityProvider, postInstallTaskProvider)
            Authorizer.None -> {
                if (deviceCapabilityProvider.isSystemApp) {
                    SystemAppInstallerRepoImpl(context, reflect, deviceCapabilityProvider, postInstallTaskProvider)
                } else {
                    NoneAppInstallerRepoImpl(context, reflect, postInstallTaskProvider)
                }
            }

            else -> ProcessAppInstallerRepoImpl(context, reflect, deviceCapabilityProvider, postInstallTaskProvider)
        }
    }
}