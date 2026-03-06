// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.session.processor

import android.system.Os
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.engine.model.AppEntity
import com.rosan.installer.domain.engine.model.InstallEntity
import com.rosan.installer.domain.engine.model.InstallExtraInfoEntity
import com.rosan.installer.domain.engine.model.PackageAnalysisResult
import com.rosan.installer.domain.engine.repository.InstallerRepository
import com.rosan.installer.domain.engine.repository.ModuleInstallerRepository
import com.rosan.installer.domain.session.model.ProgressEntity
import com.rosan.installer.domain.session.model.SelectInstallEntity
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.domain.settings.model.ConfigModel
import com.rosan.installer.domain.settings.model.RootImplementation
import com.rosan.installer.domain.settings.repository.AppSettingsRepo
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.domain.settings.repository.NamedPackageListSetting
import com.rosan.installer.domain.settings.repository.SharedUidListSetting
import com.rosan.installer.domain.settings.repository.StringSetting
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class InstallationProcessor(
    private val repo: InstallerSessionRepository,
    private val progressFlow: MutableSharedFlow<ProgressEntity>
) : KoinComponent {
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

    private val appSettingsRepo by inject<AppSettingsRepo>()
    private val capabilityProvider by inject<DeviceCapabilityProvider>()

    private val installerRepository by inject<InstallerRepository>()
    private val moduleInstallerRepository by inject<ModuleInstallerRepository>()

    /**
     * Performs the installation.
     * Updated to accept progress indicators for batch installations.
     *
     * @param current The current index (1-based) in the batch queue. Default is 1.
     * @param total The total number of items in the batch queue. Default is 1.
     */
    suspend fun install(
        config: ConfigModel,
        analysisResults: List<PackageAnalysisResult>,
        cacheDirectory: String,
        current: Int = 1,
        total: Int = 1
    ) {
        val selected = analysisResults.flatMap { it.appEntities }.filter { it.selected }
        if (selected.isEmpty()) {
            Timber.w("install: No entities selected for installation.")
            throw IllegalStateException("No items selected")
        }

        val firstApp = selected.first().app

        // For standard apps, we need to pass the progress info down.
        // For modules, the progress is tracked via log output lines.
        if (firstApp is AppEntity.ModuleEntity) {
            installModule(config, firstApp)
        } else {
            installApp(config, selected, cacheDirectory, current, total)
        }
    }

    private suspend fun installModule(config: ConfigModel, module: AppEntity.ModuleEntity) {
        Timber.d("installModule: Starting module installation for ${module.name}")
        // Initialize output list
        val output = mutableListOf<String>()
        // Check if user enabled the ASCII art display in settings
        val showArt = appSettingsRepo.getBoolean(BooleanSetting.LabModuleFlashShowArt, true).first()

        if (showArt) {
            val banner = MODULE_INSTALL_BANNER.trimIndent()

            output.addAll(banner.lines())
            // Add an empty line for better separation
            output.add("")
        }

        output.add("Starting installation...")
        // Sync to Repo immediately so it persists
        repo.moduleLog = output.toList()
        // Emit the initial state with the current log.
        progressFlow.emit(ProgressEntity.InstallingModule(output.toList()))

        val rootImpl = RootImplementation.fromString(appSettingsRepo.getString(StringSetting.LabRootImplementation).first())
        val systemUseRoot = capabilityProvider.isSystemApp && appSettingsRepo.getBoolean(BooleanSetting.LabModuleAlwaysRoot, false).first()

        // Collect logs from the underlying implementation and emit full updates.
        moduleInstallerRepository.doInstallWork(
            config = config,
            module = module,
            useRoot = systemUseRoot,
            rootImplementation = rootImpl
        ).collect { line ->
            output.add(line)
            // Sync to Repo: This ensures logs are saved even if UI is destroyed
            repo.moduleLog = output.toList()
            // Always emit the full list so UI gets the complete history upon reconnection.
            progressFlow.emit(ProgressEntity.InstallingModule(output.toList()))
        }

        Timber.d("installModule: Succeeded. Emitting ProgressEntity.InstallSuccess.")
        progressFlow.emit(ProgressEntity.InstallSuccess)
    }

    private suspend fun installApp(
        config: ConfigModel,
        selectedEntities: List<SelectInstallEntity>,
        cacheDirectory: String,
        current: Int,
        total: Int
    ) {
        val appLabel = selectedEntities.firstOrNull()?.app?.let {
            (it as? AppEntity.BaseEntity)?.label ?: it.packageName
        }

        Timber.d("install: Starting. Emitting ProgressEntity.Installing ($current/$total).")

        // Use the data class format with progress info.
        // This ensures the UI displays "Installing 1/5" instead of a generic message.
        progressFlow.emit(
            ProgressEntity.Installing(
                current = current,
                total = total,
                appLabel = appLabel
            )
        )

        Timber.d("install: Loading package name blacklist from AppSettingsRepo.")
        val blacklist = appSettingsRepo.getNamedPackageList(NamedPackageListSetting.ManagedBlacklistPackages)
            .first().map { it.packageName }

        Timber.d("install: Loading SharedUID blacklist from AppSettingsRepo.")
        val sharedUidBlacklist = appSettingsRepo.getSharedUidList(SharedUidListSetting.ManagedSharedUserIdBlacklist)
            .first().map { it.uidName }

        Timber.d("install: Loading SharedUID whitelist from AppSettingsRepo.")
        val sharedUidWhitelist = appSettingsRepo.getNamedPackageList(NamedPackageListSetting.ManagedSharedUserIdExemptedPackages)
            .first().map { it.packageName }

        val installEntities = selectedEntities.map {
            InstallEntity(
                name = it.app.name,
                packageName = it.app.packageName,
                sharedUserId = (it.app as? AppEntity.BaseEntity)?.sharedUserId,
                arch = it.app.arch,
                data = it.app.data,
                sourceType = it.app.sourceType!!
            )
        }

        val targetUserId = if (config.enableCustomizeUser) {
            Timber.d("Custom user is enabled. Installing for user: ${config.targetUserId}")
            config.targetUserId
        } else {
            val uid = Os.getuid() / 100000
            Timber.d("Custom user is disabled. Installing for current user: $uid")
            uid
        }

        // Perform the blocking install work.
        installerRepository.doInstallWork(
            config,
            installEntities,
            InstallExtraInfoEntity(targetUserId, cacheDirectory),
            blacklist,
            sharedUidBlacklist,
            sharedUidWhitelist
        )

        Timber.d("install: Single unit of work succeeded.")

        // Only emit InstallSuccess if this is a single install task.
        // In batch mode (total > 1), we must NOT emit Success here, as it would
        // prematurely signal completion to the UI while the ActionHandler loop is still running.
        if (total <= 1) {
            Timber.d("install: Total is $total, emitting ProgressEntity.InstallSuccess.")
            progressFlow.emit(ProgressEntity.InstallSuccess)
        } else {
            Timber.d("install: Total is $total (Batch Mode), skipping InstallSuccess emission.")
        }
    }
}