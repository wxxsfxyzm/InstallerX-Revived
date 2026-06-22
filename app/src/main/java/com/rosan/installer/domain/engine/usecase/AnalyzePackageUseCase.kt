// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.usecase

import com.rosan.installer.domain.engine.model.AnalyseExtraEntity
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.engine.model.packageinfo.PackageAnalysisResult
import com.rosan.installer.domain.engine.provider.InstalledModuleInfoProvider
import com.rosan.installer.domain.engine.repository.AnalyserRepository
import com.rosan.installer.domain.engine.repository.AppIconRepository
import com.rosan.installer.domain.settings.model.config.ConfigModel
import com.rosan.installer.domain.settings.model.preferences.RootMode
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.settings.repository.BooleanSetting
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive

private const val DISPLAY_ICON_SIZE_PX = 512

/**
 * UseCase for analyzing installation sources (APKs, ZIPs, Modules).
 * It coordinates file parsing, display icon preparation, and optional dynamic color extraction.
 */
class AnalyzePackageUseCase(
    private val analyserRepository: AnalyserRepository,
    private val appIconRepository: AppIconRepository,
    private val installedModuleInfoProvider: InstalledModuleInfoProvider,
    private val appSettingsRepo: AppSettingsRepository
) {
    /**
     * Executes the analysis flow.
     * * @param sessionId Current installation session ID for caching.
     * @param config The current configuration model.
     * @param data List of data entities to analyze.
     * @param extra Extra parameters for the analysis engine.
     * @return A list of [PackageAnalysisResult] enriched with metadata.
     */
    suspend operator fun invoke(
        sessionId: String,
        config: ConfigModel,
        data: List<DataEntity>,
        extra: AnalyseExtraEntity
    ): List<PackageAnalysisResult> = coroutineScope {
        if (data.isEmpty()) return@coroutineScope emptyList()

        // 1. Perform the core file analysis using the repository implementation
        val results = analyserRepository.doWork(config, data, extra)

        if (results.isEmpty()) return@coroutineScope emptyList()

        val rootMode = appSettingsRepo.preferencesFlow.first().labRootMode
        val enrichedResults = enrichInstalledModuleInfo(config, results, rootMode)

        // 2. Fetch user preferences regarding dynamic colors
        val useDynamicColor = appSettingsRepo.getBoolean(BooleanSetting.UiDynColorFollowPkgIcon, false).first()
        val useDynamicColorForLiveActivity = appSettingsRepo.getBoolean(BooleanSetting.LiveActivityDynColorFollowPkgIcon, false).first()
        val preferSystemIcon = appSettingsRepo.getBoolean(BooleanSetting.PreferSystemIconForInstall, false).first()

        // 3. Prepare display icons during analysis so UI stages can consume stable visual data.
        enrichedResults.map { res ->
            async {
                if (!isActive) throw CancellationException()

                val entityForIcon = res.appEntities
                    .map { it.app }
                    .let { entities ->
                        entities.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
                            ?: entities.filterIsInstance<AppEntity.ModuleEntity>().firstOrNull()
                            ?: entities.firstOrNull()
                    }

                val displayIcon = appIconRepository.getIcon(
                    sessionId = sessionId,
                    packageName = res.packageName,
                    entityToInstall = entityForIcon,
                    userId = 0,
                    iconSizePx = DISPLAY_ICON_SIZE_PX,
                    preferSystemIcon = preferSystemIcon
                )

                val color = if (useDynamicColor || useDynamicColorForLiveActivity) {
                    appIconRepository.extractColorFromBitmap(displayIcon)
                } else {
                    null
                }

                res.copy(displayIcon = displayIcon, seedColor = color)
            }
        }.awaitAll()
    }

    private suspend fun enrichInstalledModuleInfo(
        config: ConfigModel,
        results: List<PackageAnalysisResult>,
        rootMode: RootMode
    ): List<PackageAnalysisResult> {
        val hasModule = results.any { result ->
            result.appEntities.any { it.app is AppEntity.ModuleEntity }
        }
        if (!hasModule) return results

        val installedModules = runCatching {
            installedModuleInfoProvider.list(config, rootMode)
        }.getOrDefault(emptyList())
        if (installedModules.isEmpty()) return results

        val modulesById = installedModules.associateBy { it.id }
        return results.map { result ->
            val module = result.appEntities
                .map { it.app }
                .filterIsInstance<AppEntity.ModuleEntity>()
                .firstOrNull()
            val installedModule = modulesById[result.packageName]
                ?: module?.let { modulesById[it.id] }

            if (installedModule == null) {
                result
            } else {
                result.copy(installedModuleInfo = installedModule)
            }
        }
    }
}
