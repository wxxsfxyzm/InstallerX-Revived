// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.usecase

import com.rosan.installer.domain.engine.model.AnalyseExtraEntity
import com.rosan.installer.domain.engine.model.AppEntity
import com.rosan.installer.domain.engine.model.DataEntity
import com.rosan.installer.domain.engine.model.PackageAnalysisResult
import com.rosan.installer.domain.engine.repository.AnalyserRepository
import com.rosan.installer.domain.engine.repository.AppIconRepository
import com.rosan.installer.domain.settings.model.ConfigModel
import com.rosan.installer.domain.settings.repository.AppSettingsRepo
import com.rosan.installer.domain.settings.repository.BooleanSetting
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive

/**
 * UseCase for analyzing installation sources (APKs, ZIPs, Modules).
 * It coordinates file parsing and optional dynamic color extraction.
 */
class AnalyzePackageUseCase(
    private val analyserRepository: AnalyserRepository,
    private val appIconRepository: AppIconRepository,
    private val appSettingsRepo: AppSettingsRepo
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

        // 2. Fetch user preferences regarding dynamic colors
        val useDynamicColor = appSettingsRepo.getBoolean(BooleanSetting.UiDynColorFollowPkgIcon, false).first()
        val useDynamicColorForLiveActivity = appSettingsRepo.getBoolean(BooleanSetting.LiveActivityDynColorFollowPkgIcon, false).first()
        val preferSystemIcon = appSettingsRepo.getBoolean(BooleanSetting.PreferSystemIconForInstall, false).first()

        // 3. If dynamic color is enabled, concurrently extract seed colors from package icons
        return@coroutineScope if (useDynamicColor || useDynamicColorForLiveActivity) {
            results.map { res ->
                async {
                    if (!isActive) throw CancellationException()

                    // Extract the base APK entity to find the icon
                    val base = res.appEntities
                        .map { it.app }
                        .filterIsInstance<AppEntity.BaseEntity>()
                        .firstOrNull()

                    // Extract color using the repository (abstracted from IconColorExtractor)
                    val color = appIconRepository.extractColorFromApp(
                        sessionId = sessionId,
                        packageName = res.packageName,
                        entityToInstall = base,
                        preferSystemIcon = preferSystemIcon
                    )
                    res.copy(seedColor = color)
                }
            }.awaitAll()
        } else {
            results
        }
    }
}
