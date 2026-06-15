// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.usecase

import com.rosan.installer.core.bitmask.removeFlag
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.exception.InstallException
import com.rosan.installer.domain.engine.model.source.DataType
import com.rosan.installer.domain.engine.model.install.InstallEntity
import com.rosan.installer.domain.engine.model.install.InstallMetadata
import com.rosan.installer.domain.engine.model.error.InstallErrorType
import com.rosan.installer.domain.engine.model.install.InstallOption
import com.rosan.installer.domain.engine.model.install.sourcePath
import com.rosan.installer.domain.engine.model.packageinfo.PackageAnalysisResult
import com.rosan.installer.domain.engine.model.packageinfo.PackageSignatureAnalysis
import com.rosan.installer.domain.engine.model.packageinfo.SignatureMatchStatus
import com.rosan.installer.domain.engine.model.packageinfo.analyzePackageSignatureMatch
import com.rosan.installer.domain.engine.model.packageinfo.analyzePackageSignatureSelection
import com.rosan.installer.domain.engine.provider.InstalledPackageSignatureProvider
import com.rosan.installer.domain.engine.repository.AppInstallerRepository
import com.rosan.installer.domain.engine.repository.ModuleInstallerRepository
import com.rosan.installer.domain.history.model.InstallMethod
import com.rosan.installer.domain.history.model.OperationHistoryModel
import com.rosan.installer.domain.history.model.OperationStatus
import com.rosan.installer.domain.history.model.OperationType
import com.rosan.installer.domain.history.usecase.RecordOperationHistoryUseCase
import com.rosan.installer.domain.history.usecase.VersionChangeResolver
import com.rosan.installer.domain.history.usecase.historyErrorSummary
import com.rosan.installer.domain.history.usecase.historyErrorType
import com.rosan.installer.domain.privileged.exception.PrivilegedException
import com.rosan.installer.domain.session.model.ProgressEntity
import com.rosan.installer.domain.session.model.SelectInstallEntity
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.config.ConfigModel
import com.rosan.installer.domain.settings.model.preferences.RootMode
import com.rosan.installer.domain.settings.model.preferences.SmartAuthorizerPreferences
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.domain.settings.repository.NamedPackageListSetting
import com.rosan.installer.domain.settings.repository.SharedUidListSetting
import com.rosan.installer.domain.settings.repository.StringSetting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import timber.log.Timber

/**
 * UseCase responsible for handling the complex installation logic (both standard apps and modules).
 * It emits progress updates through a Flow, allowing the presentation/session layer to remain agnostic
 * of the underlying implementation details.
 */
class ProcessInstallationUseCase(
    private val appSettingsRepo: AppSettingsRepository,
    private val appInstaller: AppInstallerRepository,
    private val moduleInstaller: ModuleInstallerRepository,
    private val capabilityProvider: DeviceCapabilityProvider,
    private val installedPackageSignatureProvider: InstalledPackageSignatureProvider,
    private val recordOperationHistory: RecordOperationHistoryUseCase
) {
    companion object {
        private const val MODULE_INSTALL_BANNER = """
              ___           _        _ _         __  __ 
             |_ _|_ __  ___| |_ __ _| | | ___ _ _\ \/ / 
              | || '_ \/ __| __/ _` | | |/ _ \ '__\  /  
              | || | | \__ \ || (_| | | |  __/ |  /  \  
             |___|_| |_|___/\__\__,_|_|_|\___|_| /_/\_\ 

              ____            _               _ 
             |  _ \ _____   _(_)_   _____  __| | 
             | |_) / _ \ \ / / \ \ / / _ \/ _` | 
             |  _ <  __/\ V /| |\ V /  __/ (_| | 
             |_| \_\___| \_/ |_| \_/ \___|\__,_| 
            """
    }

    /**
     * Executes the installation process and emits progress entities.
     */
    operator fun invoke(
        config: ConfigModel,
        analysisResults: List<PackageAnalysisResult>,
        metadata: InstallMetadata = InstallMetadata.Empty,
        current: Int = 1,
        total: Int = 1
    ): Flow<ProgressEntity> = flow {
        val selected = analysisResults.flatMap { it.appEntities }.filter { it.selected }
        if (selected.isEmpty()) {
            Timber.w("ProcessInstallationUseCase: No entities selected for installation.")
            throw IllegalStateException("No items selected")
        }

        val firstApp = selected.first().app

        if (firstApp is AppEntity.ModuleEntity) {
            installModule(config, firstApp).collect { emit(it) }
        } else {
            // 1. Get the label first
            val appLabel = selected.firstOrNull()?.app?.let {
                (it as? AppEntity.BaseEntity)?.label ?: it.packageName
            }

            // 2. Check profile policy before proceeding
            checkBlockedByProfile(config, analysisResults)

            // 3. Emit the 'Installing' state BEFORE blocking the thread
            Timber.d("installApp: Starting. AppLabel=$appLabel ($current/$total)")
            emit(
                ProgressEntity.Installing(
                    current = current,
                    total = total,
                    appLabel = appLabel
                )
            )

            // 4. Now perform the heavy, blocking installation work
            installApp(config, analysisResults, selected, metadata)

            // 5. Emit success if it is a single task or the last task in a batch
            if (total <= 1) {
                emit(ProgressEntity.InstallSuccess)
            }
        }
    }

    private fun installModule(
        config: ConfigModel,
        module: AppEntity.ModuleEntity
    ): Flow<ProgressEntity> = flow {
        Timber.d("installModule: Starting module installation for ${module.name}")
        val output = mutableListOf<String>()
        val showArt = appSettingsRepo.getBoolean(BooleanSetting.LabModuleFlashShowArt, true).first()

        if (showArt) {
            output.addAll(MODULE_INSTALL_BANNER.trimIndent().lines())
            output.add("")
        }

        output.add("Starting installation...")
        // Emit initial state
        emit(ProgressEntity.InstallingModule(output.toList()))

        val rootImpl = RootMode.fromString(
            appSettingsRepo.getString(StringSetting.LabRootImplementation).first()
        )
        val systemUseRoot = capabilityProvider.isSystemApp &&
                appSettingsRepo.getBoolean(BooleanSetting.AlwaysUseRootInSystem, false).first()

        moduleInstaller.doInstallWork(
            config = config,
            module = module,
            useRoot = systemUseRoot,
            rootMode = rootImpl
        ).collect { line ->
            output.add(line)
            // Continually emit updated logs
            emit(ProgressEntity.InstallingModule(output.toList()))
        }

        Timber.d("installModule: Succeeded.")
        emit(ProgressEntity.InstallSuccess)
    }

    /**
     * Checks the profile's policy toggles against the analysis results.
     * Throws [InstallException] if a restriction is violated and not bypassed.
     */
    private suspend fun checkBlockedByProfile(config: ConfigModel, results: List<PackageAnalysisResult>) {
        if (config.bypassProfileRestriction) return
        if (!appSettingsRepo.getBoolean(BooleanSetting.CheckAppSignature, true).first()) return

        val selectedResults = results.filter { result -> result.appEntities.any { it.selected } }

        for (result in selectedResults) {
            if (!shouldApplySignaturePolicy(result)) continue

            val signatureAnalysis = result.appEntities.analyzePackageSignatureSelection(
                result.installedAppInfo
            )
            val signatureMatchStatus = result.appEntities.analyzePackageSignatureMatch(
                installedInfo = result.installedAppInfo,
                hasSigningCertificate = installedPackageSignatureProvider::hasSigningCertificate
            )

            if (!config.allowSigMismatch &&
                (signatureMatchStatus == SignatureMatchStatus.MISMATCH ||
                        signatureAnalysis.hasSignatureMismatchPolicyViolation())
            ) {
                throw InstallException(
                    InstallErrorType.BLOCKED_BY_PROFILE_SIGNATURE_MISMATCH,
                    "Installing apps with a different signature is blocked by this profile"
                )
            }

            if (!config.allowSigUnknown &&
                (signatureMatchStatus == SignatureMatchStatus.UNKNOWN_ERROR ||
                        signatureMatchStatus == SignatureMatchStatus.CANDIDATE_ROTATION_UNCONFIRMED ||
                        signatureAnalysis.hasSignatureUnknownPolicyViolation())
            ) {
                throw InstallException(
                    InstallErrorType.BLOCKED_BY_PROFILE_SIGNATURE_UNKNOWN,
                    "Installing apps with an unverifiable signature is blocked by this profile"
                )
            }
        }
    }

    private fun shouldApplySignaturePolicy(result: PackageAnalysisResult): Boolean {
        val selectedApps = result.appEntities.filter { it.selected }.map { it.app }
        val selectedApks = selectedApps.filter { it is AppEntity.BaseEntity || it is AppEntity.SplitEntity }
        val containerType = selectedApks.firstOrNull()?.sourceType ?: return false
        val hasInstalledApp = result.installedAppInfo != null
        val hasSelectedBase = selectedApks.any { it is AppEntity.BaseEntity }
        val hasSelectedSplit = selectedApks.any { it is AppEntity.SplitEntity }
        val isSplitUpdateMode = hasInstalledApp && hasSelectedSplit && !hasSelectedBase

        return !isSplitUpdateMode && containerType.supportsApkSignaturePolicy()
    }

    private fun DataType.supportsApkSignaturePolicy() = when (this) {
        DataType.APK,
        DataType.APKS,
        DataType.APKM,
        DataType.XAPK,
        DataType.MULTI_APK,
        DataType.MULTI_APK_ZIP -> true

        else -> false
    }

    private fun PackageSignatureAnalysis.hasSignatureMismatchPolicyViolation(): Boolean {
        return splitSignatureMismatchFiles.isNotEmpty()
    }

    private fun PackageSignatureAnalysis.hasSignatureUnknownPolicyViolation(): Boolean {
        return verificationFailedFiles.isNotEmpty() ||
                duplicateSplitNames.isNotEmpty()
    }

    private suspend fun installApp(
        config: ConfigModel,
        analysisResults: List<PackageAnalysisResult>,
        selectedEntities: List<SelectInstallEntity>,
        metadata: InstallMetadata
    ) {
        val blacklist = appSettingsRepo.getNamedPackageList(NamedPackageListSetting.ManagedBlacklistPackages)
            .first().map { it.packageName }

        val sharedUidBlacklist = appSettingsRepo.getSharedUidList(SharedUidListSetting.ManagedSharedUserIdBlacklist)
            .first().map { it.uidName }

        val sharedUidWhitelist = appSettingsRepo.getNamedPackageList(NamedPackageListSetting.ManagedSharedUserIdExemptedPackages)
            .first().map { it.packageName }

        val installEntities = selectedEntities.map {
            InstallEntity(
                name = it.app.name,
                packageName = it.app.packageName,
                sharedUserId = (it.app as? AppEntity.BaseEntity)?.sharedUserId,
                arch = it.app.arch,
                data = it.app.data,
                sourceType = it.app.sourceType!!,
                installLocation = (it.app as? AppEntity.BaseEntity)?.installLocation
            )
        }

        var historyConfig = config
        val result = runCatching {
            historyConfig = installWithResolvedAuthorizer(
                config = config,
                installEntities = installEntities,
                metadata = metadata,
                blacklist = blacklist,
                sharedUidBlacklist = sharedUidBlacklist,
                sharedUidWhitelist = sharedUidWhitelist
            )
        }

        recordInstallHistory(historyConfig, analysisResults, selectedEntities, result)
        result.onFailure { throw it }
    }

    private suspend fun installWithResolvedAuthorizer(
        config: ConfigModel,
        installEntities: List<InstallEntity>,
        metadata: InstallMetadata,
        blacklist: List<String>,
        sharedUidBlacklist: List<String>,
        sharedUidWhitelist: List<String>
    ): ConfigModel {
        val tryMultipleAuthorizers = appSettingsRepo
            .getBoolean(BooleanSetting.TryMultipleAuthorizersOnInstall, false)
            .first()

        if (!tryMultipleAuthorizers) {
            submitInstall(config, installEntities, metadata, blacklist, sharedUidBlacklist, sharedUidWhitelist)
            return config
        }

        val candidates = buildAuthorizerCandidates(config)
        if (candidates.isEmpty()) {
            submitInstall(config, installEntities, metadata, blacklist, sharedUidBlacklist, sharedUidWhitelist)
            return config
        }

        var lastAuthorizerFailure: PrivilegedException? = null
        for (authorizer in candidates) {
            val attemptConfig = config.copy(authorizer = authorizer).withAuthorizerAdjustedInstallFlags()
            Timber.d("Trying install with authorizer: ${attemptConfig.authorizer}")

            try {
                submitInstall(attemptConfig, installEntities, metadata, blacklist, sharedUidBlacklist, sharedUidWhitelist)
                return attemptConfig
            } catch (e: PrivilegedException) {
                lastAuthorizerFailure = e
                Timber.w(e, "Authorizer ${attemptConfig.authorizer} unavailable, trying next candidate.")
            }
        }

        throw InstallException(
            InstallErrorType.ALL_AUTHORIZERS_FAILED,
            "All authorizers failed: ${lastAuthorizerFailure?.message.orEmpty()}"
        )
    }

    private suspend fun submitInstall(
        config: ConfigModel,
        installEntities: List<InstallEntity>,
        metadata: InstallMetadata,
        blacklist: List<String>,
        sharedUidBlacklist: List<String>,
        sharedUidWhitelist: List<String>
    ) {
        appInstaller.doInstallWork(
            config = config,
            entities = installEntities,
            metadata = metadata,
            blacklist = blacklist,
            sharedUserIdBlacklist = sharedUidBlacklist,
            sharedUserIdExemption = sharedUidWhitelist
        )
    }

    private suspend fun buildAuthorizerCandidates(config: ConfigModel): List<Authorizer> {
        val fallbackAuthorizers = SmartAuthorizerPreferences.decode(
            value = appSettingsRepo
                .getString(StringSetting.SmartAuthorizerCandidates)
                .first(),
            isSystemApp = capabilityProvider.isSystemApp
        )
            .filter { it.enabled }
            .map { it.authorizer }

        return buildList {
            if (config.authorizer != Authorizer.Global) add(config.authorizer)
            addAll(fallbackAuthorizers)
        }.filter { authorizer ->
            authorizer != Authorizer.Global &&
                    (authorizer != Authorizer.Customize || config.customizeAuthorizer.isNotBlank())
        }.distinct()
    }

    private fun ConfigModel.withAuthorizerAdjustedInstallFlags(): ConfigModel {
        if (authorizer != Authorizer.Dhizuku) return this

        val flags = installFlags
            .removeFlag(InstallOption.AllUsers.value)
            .removeFlag(InstallOption.GrantAllRequestedPermissions.value)

        return copy(
            installFlags = flags,
            forAllUser = false,
            allowAllRequestedPermissions = false
        )
    }

    private suspend fun recordInstallHistory(
        config: ConfigModel,
        analysisResults: List<PackageAnalysisResult>,
        selectedEntities: List<SelectInstallEntity>,
        result: Result<Unit>
    ) {
        val installerPackageName = runCatching {
            appInstaller.resolveInstallerPackageName(config)
        }.getOrNull()

        selectedEntities
            .groupBy { it.app.packageName }
            .forEach { (packageName, selectedForPackage) ->
                val analysis = analysisResults.find { it.packageName == packageName }
                val base = selectedForPackage.map { it.app }
                    .filterIsInstance<AppEntity.BaseEntity>()
                    .firstOrNull()
                    ?: analysis?.appEntities
                        ?.map { it.app }
                        ?.filterIsInstance<AppEntity.BaseEntity>()
                        ?.firstOrNull()
                val installed = analysis?.installedAppInfo?.takeUnless { it.isUninstalled }
                val oldVersionCode = installed?.versionCode
                val newVersionCode = base?.versionCode
                val sourcePaths = selectedForPackage
                    .mapNotNull { it.app.data.sourcePath() }
                    .distinct()

                runCatching {
                    recordOperationHistory(
                    OperationHistoryModel(
                        operationType = OperationType.INSTALL,
                        status = if (result.isSuccess) OperationStatus.SUCCESS else OperationStatus.FAILED,
                        packageName = packageName,
                        appLabel = base?.label ?: installed?.label,
                        isFreshInstall = oldVersionCode == null,
                        versionChange = VersionChangeResolver.resolve(oldVersionCode, newVersionCode),
                        oldVersionName = installed?.versionName,
                        oldVersionCode = oldVersionCode,
                        newVersionName = base?.versionName,
                        newVersionCode = newVersionCode,
                        sourcePaths = sourcePaths,
                        initiatorPackageName = config.initiatorPackageName,
                        installerPackageName = installerPackageName,
                        installMethod = InstallMethod.PACKAGE_MANAGER,
                        authorizer = config.authorizer,
                        installMode = config.installMode,
                        errorSummary = result.exceptionOrNull()?.historyErrorSummary(),
                        errorType = result.exceptionOrNull()?.historyErrorType()
                    )
                    )
                }.onFailure { e ->
                    Timber.e(e, "Failed to record install history for $packageName")
                }
            }
    }
}
