// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.usecase

import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.engine.model.AppEntity
import com.rosan.installer.domain.engine.model.InstallEntity
import com.rosan.installer.domain.engine.model.PackageAnalysisResult
import com.rosan.installer.domain.engine.repository.AppInstallerRepository
import com.rosan.installer.domain.engine.repository.ModuleInstallerRepository
import com.rosan.installer.domain.session.model.ProgressEntity
import com.rosan.installer.domain.session.model.SelectInstallEntity
import com.rosan.installer.domain.settings.model.ConfigModel
import com.rosan.installer.domain.settings.model.RootImplementation
import com.rosan.installer.domain.settings.repository.AppSettingsRepo
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
    private val appSettingsRepo: AppSettingsRepo,
    private val appInstaller: AppInstallerRepository,
    private val moduleInstaller: ModuleInstallerRepository,
    private val capabilityProvider: DeviceCapabilityProvider
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

            // 2. Emit the 'Installing' state BEFORE blocking the thread
            Timber.d("installApp: Starting. AppLabel=$appLabel ($current/$total)")
            emit(
                ProgressEntity.Installing(
                    current = current,
                    total = total,
                    appLabel = appLabel
                )
            )

            // 3. Now perform the heavy, blocking installation work
            installApp(config, selected)

            // 4. Emit success if it is a single task or the last task in a batch
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

        val rootImpl = RootImplementation.fromString(
            appSettingsRepo.getString(StringSetting.LabRootImplementation).first()
        )
        val systemUseRoot = capabilityProvider.isSystemApp &&
                appSettingsRepo.getBoolean(BooleanSetting.LabModuleAlwaysRoot, false).first()

        moduleInstaller.doInstallWork(
            config = config,
            module = module,
            useRoot = systemUseRoot,
            rootImplementation = rootImpl
        ).collect { line ->
            output.add(line)
            // Continually emit updated logs
            emit(ProgressEntity.InstallingModule(output.toList()))
        }

        Timber.d("installModule: Succeeded.")
        emit(ProgressEntity.InstallSuccess)
    }

    private suspend fun installApp(
        config: ConfigModel,
        selectedEntities: List<SelectInstallEntity>
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
                sourceType = it.app.sourceType!!
            )
        }

        // Perform the actual installation via repository (This blocks until finished)
        appInstaller.doInstallWork(
            config = config,
            entities = installEntities,
            blacklist = blacklist,
            sharedUserIdBlacklist = sharedUidBlacklist,
            sharedUserIdExemption = sharedUidWhitelist
        )
    }
}
