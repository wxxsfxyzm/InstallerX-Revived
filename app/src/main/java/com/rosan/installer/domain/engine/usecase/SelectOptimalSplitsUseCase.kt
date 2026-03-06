// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.usecase

import com.rosan.installer.core.env.DeviceConfig
import com.rosan.installer.data.engine.parser.FilterType
import com.rosan.installer.domain.engine.model.AppEntity
import com.rosan.installer.domain.engine.model.DataType
import com.rosan.installer.domain.session.model.SelectInstallEntity
import com.rosan.installer.util.convertLegacyLanguageCode
import timber.log.Timber

/**
 * A Domain UseCase responsible for determining the optimal set of APK splits/bases to install
 * based on user configuration and device hardware/language preferences.
 */
class SelectOptimalSplitsUseCase {

    operator fun invoke(
        splitChooseAll: Boolean,
        apkChooseAll: Boolean,
        entities: List<AppEntity>,
        sessionType: DataType
    ): List<SelectInstallEntity> {
        Timber.d("SelectionStrategy: Starting selection for ${entities.size} entities. ContainerType: $sessionType")

        val isBatchMode = sessionType == DataType.MULTI_APK ||
                sessionType == DataType.MULTI_APK_ZIP ||
                sessionType == DataType.MIXED_MODULE_APK ||
                sessionType == DataType.MIXED_MODULE_ZIP

        // 1. Mixed Modules
        if (sessionType == DataType.MIXED_MODULE_APK || sessionType == DataType.MIXED_MODULE_ZIP) {
            return entities.map {
                val isSelected = if (it is AppEntity.BaseEntity) apkChooseAll else false
                SelectInstallEntity(it, selected = isSelected)
            }
        }

        val bases = entities.filterIsInstance<AppEntity.BaseEntity>()

        // 2. Multi-App Mode
        val isMultiAppMode = (sessionType == DataType.MULTI_APK || sessionType == DataType.MULTI_APK_ZIP)
        if (isMultiAppMode && bases.size > 1) {
            val bestBase = findBestBase(bases)
            return entities.map { entity ->
                val isSelected = if (entity is AppEntity.BaseEntity) {
                    if (apkChooseAll) true else (entity == bestBase)
                } else false
                SelectInstallEntity(entity, selected = isSelected)
            }
        }

        // 3. Single Split Check
        if (entities.size == 1 && entities.first() is AppEntity.SplitEntity) {
            return listOf(SelectInstallEntity(entities.first(), selected = true))
        }

        // 4. Optimal Splits Calculation
        val allSplits = entities.filterIsInstance<AppEntity.SplitEntity>()
        val selectedSplits = if (splitChooseAll) {
            allSplits.toSet()
        } else {
            selectOptimalSplits(allSplits)
        }

        return entities.map { entity ->
            val isSelected = when (entity) {
                is AppEntity.BaseEntity -> if (isBatchMode) apkChooseAll else true
                is AppEntity.DexMetadataEntity -> true
                is AppEntity.SplitEntity -> entity in selectedSplits
                is AppEntity.ModuleEntity -> true
            }
            SelectInstallEntity(entity, selected = isSelected)
        }
    }

    private fun selectOptimalSplits(splits: List<AppEntity.SplitEntity>): Set<AppEntity.SplitEntity> {
        if (splits.isEmpty()) return emptySet()

        val availableAbis = splits.filter { it.filterType == FilterType.ABI }.mapNotNull { it.configValue }.toSet()
        val deviceAbis = DeviceConfig.supportedArchitectures.map { it.arch }
        val targetAbi = findBestDeviceMatch(availableAbis, deviceAbis)

        val availableDensities = splits.filter { it.filterType == FilterType.DENSITY }.mapNotNull { it.configValue }.toSet()
        val deviceDensities = DeviceConfig.supportedDensities.map { it.key }
        val targetDensity = findBestDeviceMatch(availableDensities, deviceDensities)

        val availableLangs = splits.filter { it.filterType == FilterType.LANGUAGE }.mapNotNull { it.configValue }.toSet()
        val targetLangs = findBestLanguageMatches(availableLangs)

        return splits.filter { split ->
            when (split.filterType) {
                FilterType.NONE -> true
                FilterType.ABI -> split.configValue == targetAbi
                FilterType.DENSITY -> split.configValue == targetDensity
                FilterType.LANGUAGE -> split.configValue in targetLangs
            }
        }.toSet()
    }

    private fun findBestDeviceMatch(candidates: Set<String>, devicePreferences: List<String>): String? {
        return devicePreferences.firstOrNull { it in candidates }
    }

    private fun findBestLanguageMatches(candidates: Set<String>): Set<String> {
        if (candidates.isEmpty()) return emptySet()
        val deviceLangs = DeviceConfig.supportedLocales.map { it.convertLegacyLanguageCode() }
        val selected = mutableSetOf<String>()

        val exactMatches = candidates.filter { lang -> deviceLangs.contains(lang) }
        selected.addAll(exactMatches)

        if (selected.isEmpty()) {
            val primaryBase = deviceLangs.firstOrNull()?.substringBefore('-')
            if (primaryBase != null) {
                candidates.firstOrNull { it.startsWith(primaryBase) }?.let { selected.add(it) }
            }
        }
        return selected
    }

    private fun findBestBase(bases: List<AppEntity.BaseEntity>): AppEntity.BaseEntity? {
        if (bases.isEmpty()) return null
        if (bases.size == 1) return bases.first()

        val deviceAbis = DeviceConfig.supportedArchitectures.map { it.arch }
        val sorted = bases.sortedWith(
            compareBy<AppEntity.BaseEntity> { base ->
                val abiIndex = base.arch?.let { deviceAbis.indexOf(it.arch) } ?: -1
                if (abiIndex == -1) Int.MAX_VALUE else abiIndex
            }
                .thenByDescending { it.versionCode }
                .thenByDescending { it.versionName }
        )
        return sorted.firstOrNull()
    }
}
