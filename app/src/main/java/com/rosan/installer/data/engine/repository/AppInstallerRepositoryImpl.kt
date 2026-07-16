// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.data.engine.repository

import android.content.Context
import android.os.Build
import com.rosan.installer.core.env.AppConfig
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
import com.rosan.installer.domain.engine.model.install.shouldAutoDeleteSource
import com.rosan.installer.domain.engine.model.install.sourcePath
import com.rosan.installer.domain.engine.repository.AppInstallerRepository
import com.rosan.installer.domain.packageupdate.model.PendingSourceDeletion
import com.rosan.installer.domain.packageupdate.repository.SelfUpdateRecoveryRepository
import com.rosan.installer.domain.privileged.exception.PrivilegedException
import com.rosan.installer.domain.privileged.model.PrivilegedErrorType
import com.rosan.installer.domain.privileged.provider.PostInstallTaskProvider
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.config.ConfigModel
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.settings.repository.BooleanSetting
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.io.File

class AppInstallerRepositoryImpl(
    private val context: Context,
    private val reflect: ReflectionProvider,
    private val appSettingsRepo: AppSettingsRepository,
    private val deviceCapabilityProvider: DeviceCapabilityProvider,
    private val postInstallTaskProvider: PostInstallTaskProvider,
    private val platformInstallPolicyChecker: PlatformInstallPolicyChecker,
    private val selfUpdateRecoveryRepository: SelfUpdateRecoveryRepository
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
        persistSelfUpdateSourceDeletion(config, entities, metadata)

        val requestedRespectPlatformInstallPolicy =
            AppConfig.isRespectPlatformInstallPolicyAvailable &&
                    (respectPlatformInstallPolicy ||
                            appSettingsRepo.getBoolean(BooleanSetting.LabRespectPlatformInstallPolicy).first())
        val canCheckPlatformInstallPolicy = canCheckPlatformInstallPolicy(config)
        val effectiveRespectPlatformInstallPolicy =
            requestedRespectPlatformInstallPolicy && canCheckPlatformInstallPolicy
        Timber.tag(TAG).d(
            "doInstallWork: respectPlatformPolicy=%s, requestedByCaller=%s, requestedEffective=%s, authorizer=%s, source=%s, sourceUid=%s, confidence=%s",
            effectiveRespectPlatformInstallPolicy,
            respectPlatformInstallPolicy,
            requestedRespectPlatformInstallPolicy,
            config.authorizer,
            config.initiatorPackageName,
            config.installSourceUid,
            config.installSourceConfidence
        )
        if (requestedRespectPlatformInstallPolicy) {
            if (canCheckPlatformInstallPolicy) {
                Timber.tag(TAG).d("Running platform install policy checker.")
                platformInstallPolicyChecker.check(config)
            } else {
                Timber.tag(TAG).d(
                    "Skipping platform policy checker: authorizer=%s, isSystemApp=%s",
                    config.authorizer,
                    deviceCapabilityProvider.isSystemApp
                )
            }
        }
        repo.doInstallWork(
            config,
            entities,
            metadata,
            effectiveRespectPlatformInstallPolicy,
            blacklist,
            sharedUserIdBlacklist,
            sharedUserIdExemption
        )
    }

    private suspend fun persistSelfUpdateSourceDeletion(
        config: ConfigModel,
        entities: List<InstallEntity>,
        metadata: InstallMetadata
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN) return
        if (entities.firstOrNull()?.packageName != context.packageName) return
        val sessionId = metadata.operationSessionKey ?: return

        val shouldDelete = config.shouldAutoDeleteSource(entities.firstOrNull()?.sourceType)
        val deletePaths = if (shouldDelete) {
            // This is the normal post-install delete input. Keep only absolute filesystem paths;
            // Android 17 recovery must not persist a transient content URI or a URI grant.
            entities.sourcePath()
                .filter { File(it).isAbsolute }
                .distinct()
        } else {
            emptyList()
        }
        val sourceDeletion = deletePaths.takeIf { it.isNotEmpty() }?.let { paths ->
            PendingSourceDeletion(
                paths = paths,
                authorizer = config.authorizer,
                customizeAuthorizer = config.customizeAuthorizer
            )
        }

        try {
            selfUpdateRecoveryRepository.updateSourceDeletion(sessionId, sourceDeletion)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            // Failure to persist optional cleanup must not prevent the package update itself.
            Timber.w(error, "Failed to persist Android 17 self-update source deletion.")
        }
    }

    private fun canCheckPlatformInstallPolicy(config: ConfigModel): Boolean =
        when (config.authorizer) {
            Authorizer.Root,
            Authorizer.Shizuku,
            Authorizer.Customize -> true

            Authorizer.None -> deviceCapabilityProvider.isSystemApp

            Authorizer.Dhizuku,
            Authorizer.Global -> false
        }

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
    private fun resolveRepo(config: ConfigModel) =
        when (config.authorizer) {
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
