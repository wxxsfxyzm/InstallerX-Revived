package com.rosan.installer.data.app.model.impl.processor

import com.rosan.installer.build.RsConfig
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataType
import com.rosan.installer.data.app.util.FilterType
import com.rosan.installer.data.installer.model.entity.SelectInstallEntity
import com.rosan.installer.util.convertLegacyLanguageCode

object SelectionStrategy {

    fun select(
        entities: List<AppEntity>,
        containerType: DataType
    ): List<SelectInstallEntity> {
        // 1. Mixed Modules: Default unselected
        if (containerType == DataType.MIXED_MODULE_APK || containerType == DataType.MIXED_MODULE_ZIP) {
            return entities.map { SelectInstallEntity(it, selected = false) }
        }

        // 2. Multi-App Mode: Best Base only
        val isMultiAppMode = containerType == DataType.MULTI_APK || containerType == DataType.MULTI_APK_ZIP
        if (isMultiAppMode) {
            val bestBase = findBestBase(entities.filterIsInstance<AppEntity.BaseEntity>())
            return entities.map { entity ->
                SelectInstallEntity(entity, selected = (entity == bestBase))
            }
        }

        // 3. Single App Mode: Base + Calculated Splits
        // Extract all splits to analyze them globally
        val allSplits = entities.filterIsInstance<AppEntity.SplitEntity>()
        val selectedSplits = selectOptimalSplits(allSplits)

        return entities.map { entity ->
            val isSelected = when (entity) {
                is AppEntity.BaseEntity -> true
                is AppEntity.DexMetadataEntity -> true
                is AppEntity.SplitEntity -> entity in selectedSplits
                is AppEntity.CollectionEntity -> false
                is AppEntity.ModuleEntity -> true
            }
            SelectInstallEntity(entity, selected = isSelected)
        }
    }

    /**
     * Unified selection logic.
     * Calculates the target configuration for the device, then filters ALL splits
     * (both Standalone Configs and Feature Configs) against these targets.
     */
    private fun selectOptimalSplits(splits: List<AppEntity.SplitEntity>): Set<AppEntity.SplitEntity> {
        if (splits.isEmpty()) return emptySet()

        // Step A: Determine the "Target" configurations for this specific app's split set.
        // We look at what matches the device supports, and pick the best one available in the splits.

        // A1. Target ABI
        val availableAbis = splits
            .filter { it.filterType == FilterType.ABI }
            .mapNotNull { it.configValue } // configValue is strict (e.g. "x86_64" from Enum)
            .toSet()

        // Use raw arch string from Enum (e.g. "x86_64") to match SplitMetadata
        val deviceAbis = RsConfig.supportedArchitectures.map { it.arch }
        val targetAbi = findBestDeviceMatch(availableAbis, deviceAbis)

        // A2. Target Density
        val availableDensities = splits
            .filter { it.filterType == FilterType.DENSITY }
            .mapNotNull { it.configValue }
            .toSet()

        // Fix: Use .map { it.key } as defined in Density.kt ("xhdpi", etc.)
        val deviceDensities = RsConfig.supportedDensities.map { it.key }
        val targetDensity = findBestDeviceMatch(availableDensities, deviceDensities)

        // A3. Target Languages (Can be multiple)
        val availableLangs = splits
            .filter { it.filterType == FilterType.LANGUAGE }
            .mapNotNull { it.configValue }
            .toSet()
        val targetLangs = findBestLanguageMatches(availableLangs)

        // Step B: Filter every split based on its individual restriction.
        return splits.filter { split ->
            when (split.filterType) {
                // No restriction (Generic Feature) -> Always keep
                FilterType.NONE -> true

                // ABI restriction -> Must match the calculated Target ABI
                FilterType.ABI -> split.configValue == targetAbi

                // Density restriction -> Must match the calculated Target Density
                FilterType.DENSITY -> split.configValue == targetDensity

                // Language restriction -> Must be in the calculated Target Languages list
                FilterType.LANGUAGE -> split.configValue in targetLangs
            }
        }.toSet()
    }

    // --- Helper Algorithms ---

    private fun findBestDeviceMatch(candidates: Set<String>, devicePreferences: List<String>): String? {
        if (candidates.isEmpty()) return null
        // Return the first device preference that is actually available in the APKs
        return devicePreferences.firstOrNull { it in candidates }
    }

    private fun findBestLanguageMatches(candidates: Set<String>): Set<String> {
        if (candidates.isEmpty()) return emptySet()
        val deviceLangs = RsConfig.supportedLocales.map { it.convertLegacyLanguageCode() }
        val selected = mutableSetOf<String>()

        // 1. Exact Matches
        val exactMatches = candidates.filter { lang -> deviceLangs.contains(lang) }
        selected.addAll(exactMatches)

        // 2. Fuzzy Matches
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

        val deviceAbis = RsConfig.supportedArchitectures.map { it.arch }
        return bases.sortedWith(
            compareBy<AppEntity.BaseEntity> { base ->
                val abiIndex = base.arch?.let { deviceAbis.indexOf(it.arch) } ?: -1
                if (abiIndex == -1) Int.MAX_VALUE else abiIndex
            }
                .thenByDescending { it.versionCode }
                .thenByDescending { it.versionName }
        ).firstOrNull()
    }
}