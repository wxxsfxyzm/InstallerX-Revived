// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.data.engine.repository

import android.content.Context
import com.rosan.installer.core.reflection.ReflectionProvider
import com.rosan.installer.data.engine.executor.appinstaller.DhizukuAppInstallerRepoImpl
import com.rosan.installer.data.engine.executor.appinstaller.NoneAppInstallerRepoImpl
import com.rosan.installer.data.engine.executor.appinstaller.ProcessAppInstallerRepoImpl
import com.rosan.installer.data.engine.executor.appinstaller.ShizukuAppInstallerRepoImpl
import com.rosan.installer.data.engine.executor.appinstaller.SystemAppInstallerRepoImpl
import com.rosan.installer.data.engine.policy.PlatformInstallPolicyChecker
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.engine.model.install.InstallEntity
import com.rosan.installer.domain.engine.model.install.InstallMetadata
import com.rosan.installer.domain.engine.repository.AppInstallerRepository
import com.rosan.installer.domain.privileged.exception.PrivilegedException
import com.rosan.installer.domain.privileged.model.PrivilegedErrorType
import com.rosan.installer.domain.privileged.provider.PostInstallTaskProvider
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.config.ConfigModel
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.settings.repository.BooleanSetting
import kotlinx.coroutines.flow.first
import timber.log.Timber

class AppInstallerRepositoryImpl(
    private val context: Context,
    private val reflect: ReflectionProvider,
    private val deviceCapabilityProvider: DeviceCapabilityProvider,
    private val appSettingsRepo: AppSettingsRepository,
    private val postInstallTaskProvider: PostInstallTaskProvider,
    private val platformInstallPolicyChecker: PlatformInstallPolicyChecker
) : AppInstallerRepository {
    override suspend fun resolveInstallerPackageName(config: ConfigModel): String? =
        executeWithRepo(config) { repo ->
            repo.resolveInstallerPackageName(config)
        }

    override suspend fun doInstallWork(
        config: ConfigModel,
        entities: List<InstallEntity>,
        metadata: InstallMetadata,
        respectPlatformInstallPolicy: Boolean,
        blacklist: List<String>,
        sharedUserIdBlacklist: List<String>,
        sharedUserIdExemption: List<String>
    ) = executeWithRepo(config) { repo ->
        val requestedRespectPlatformInstallPolicy = respectPlatformInstallPolicy ||
                appSettingsRepo.getBoolean(BooleanSetting.LabRespectPlatformInstallPolicy).first()
        Timber.tag(TAG).d(
            "doInstallWork: respectPlatformPolicy=%s, requestedByCaller=%s, authorizer=%s, source=%s, sourceUid=%s, confidence=%s",
            requestedRespectPlatformInstallPolicy,
            respectPlatformInstallPolicy,
            config.authorizer,
            config.initiatorPackageName,
            config.installSourceUid,
            config.installSourceConfidence
        )
        if (requestedRespectPlatformInstallPolicy) {
            if (canCheckPlatformInstallPolicy(config)) {
                Timber.tag(TAG).d("Running platform install policy checker.")
                platformInstallPolicyChecker.check(config)
            } else {
                Timber.tag(TAG).d("Skipping platform policy checker for non-privileged install path.")
            }
        }
        repo.doInstallWork(
            config,
            entities,
            metadata,
            requestedRespectPlatformInstallPolicy,
            blacklist,
            sharedUserIdBlacklist,
            sharedUserIdExemption
        )
    }

    private fun canCheckPlatformInstallPolicy(config: ConfigModel): Boolean =
        config.authorizer != Authorizer.None || deviceCapabilityProvider.isSystemApp

    private companion object {
        const val TAG = "AppInstallerRepository"
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
            throw when (repo) {
                is ShizukuAppInstallerRepoImpl if e.message?.contains("binder haven't been received") == true ->
                    PrivilegedException(
                        errorType = PrivilegedErrorType.SHIZUKU_NOT_WORK,
                        message = "Shizuku service connection lost during operation.",
                        cause = e
                    )

                is DhizukuAppInstallerRepoImpl if e.message?.contains("KoinApplication has not been started") == true ->
                    PrivilegedException(
                        errorType = PrivilegedErrorType.DHIZUKU_NOT_WORK,
                        message = "Dhizuku service connection lost during operation.",
                        cause = e
                    )

                is ProcessAppInstallerRepoImpl if e.message?.contains("Failed to initialize AppProcess for Hook Mode") == true ->
                    PrivilegedException(
                        errorType = if (config.authorizer == Authorizer.Root) {
                            PrivilegedErrorType.ROOT_NOT_WORK
                        } else {
                            PrivilegedErrorType.APP_PROCESS_NOT_WORK
                        },
                        message = "AppProcess hook initialization failed during operation.",
                        cause = e
                    )

                else -> e
            }
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
