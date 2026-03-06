// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.data.engine.repository

import android.content.Context
import com.rosan.installer.core.reflection.ReflectionProvider
import com.rosan.installer.data.engine.executor.appInstaller.DhizukuInstallerRepoImpl
import com.rosan.installer.data.engine.executor.appInstaller.NoneInstallerRepoImpl
import com.rosan.installer.data.engine.executor.appInstaller.ProcessInstallerRepoImpl
import com.rosan.installer.data.engine.executor.appInstaller.ShizukuInstallerRepoImpl
import com.rosan.installer.data.engine.executor.appInstaller.SystemInstallerRepoImpl
import com.rosan.installer.data.privileged.model.exception.ShizukuNotWorkException

import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.engine.model.InstallEntity
import com.rosan.installer.domain.engine.model.InstallExtraInfoEntity
import com.rosan.installer.domain.engine.repository.InstallerRepository
import com.rosan.installer.domain.privileged.provider.PostInstallTaskProvider
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.ConfigModel

class InstallerRepositoryImpl(
    private val context: Context,
    private val reflect: ReflectionProvider,
    private val deviceCapabilityProvider: DeviceCapabilityProvider,
    private val postInstallTaskProvider: PostInstallTaskProvider
) : InstallerRepository {
    override suspend fun doInstallWork(
        config: ConfigModel,
        entities: List<InstallEntity>,
        extra: InstallExtraInfoEntity,
        blacklist: List<String>,
        sharedUserIdBlacklist: List<String>,
        sharedUserIdExemption: List<String>
    ) = executeWithRepo(config) { repo ->
        repo.doInstallWork(
            config,
            entities,
            extra,
            blacklist,
            sharedUserIdBlacklist,
            sharedUserIdExemption
        )
    }

    override suspend fun doUninstallWork(
        config: ConfigModel,
        packageName: String,
        extra: InstallExtraInfoEntity,
    ) = executeWithRepo(config) { repo ->
        repo.doUninstallWork(config, packageName, extra)
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
        action: suspend (InstallerRepository) -> T
    ): T {
        val repo = resolveRepo(config)

        try {
            return action(repo)
        } catch (e: IllegalStateException) {
            // Check if Shizuku service connection is lost
            if (repo is ShizukuInstallerRepoImpl && e.message?.contains("binder haven't been received") == true) {
                throw ShizukuNotWorkException("Shizuku service connection lost during operation.", e)
            }
            // Throw other exceptions as-is
            throw e
        }
    }

    /**
     * Resolve the InstallerRepo based on the provided 
     */
    private fun resolveRepo(config: ConfigModel): InstallerRepository {
        return when (config.authorizer) {
            Authorizer.Shizuku -> ShizukuInstallerRepoImpl(context, reflect, deviceCapabilityProvider, postInstallTaskProvider)
            Authorizer.Dhizuku -> DhizukuInstallerRepoImpl(context, reflect, deviceCapabilityProvider, postInstallTaskProvider)
            Authorizer.None -> {
                if (deviceCapabilityProvider.isSystemApp) {
                    SystemInstallerRepoImpl(context, reflect, deviceCapabilityProvider, postInstallTaskProvider)
                } else {
                    NoneInstallerRepoImpl(context, reflect)
                }
            }

            else -> ProcessInstallerRepoImpl(context, reflect, deviceCapabilityProvider, postInstallTaskProvider)
        }
    }
}