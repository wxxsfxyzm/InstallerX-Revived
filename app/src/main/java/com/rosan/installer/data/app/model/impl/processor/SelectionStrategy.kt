package com.rosan.installer.data.app.model.impl.processor

import com.rosan.installer.build.RsConfig
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.enums.DataType
import com.rosan.installer.data.app.util.FilterType
import com.rosan.installer.data.installer.model.entity.SelectInstallEntity
import com.rosan.installer.util.convertLegacyLanguageCode
import timber.log.Timber

object SelectionStrategy {

    fun select(
        splitChooseAll: Boolean,
        apkChooseAll: Boolean,
        entities: List<AppEntity>,
        sessionType: DataType
    ): List<SelectInstallEntity> {
        Timber.d("SelectionStrategy: Starting selection for ${entities.size} entities. ContainerType: $sessionType")

        // 1. Mixed Modules: Default unselected
        if (sessionType == DataType.MIXED_MODULE_APK || sessionType == DataType.MIXED_MODULE_ZIP) {
            Timber.d("Mixed Module detected. All entities deselected by default.")
            return entities.map { SelectInstallEntity(it, selected = false) }
        }

        // Identify Base entities count
        val bases = entities.filterIsInstance<AppEntity.BaseEntity>()

        // 2. Multi-App Mode: Best Base only
        // Only trigger "Best Base Selection" if there is actually more than one base to choose from.
        // If it's MULTI_APK but only has 1 base (e.g. batch installing App Bundles), allow it to fall through to split selection.
        val isMultiAppMode = (sessionType == DataType.MULTI_APK || sessionType == DataType.MULTI_APK_ZIP)
        // Only apply strict filtering if we have conflict (multiple bases for same package)
        if (isMultiAppMode && bases.size > 1) {
            Timber.d("Multi-App Mode detected. Selecting best base entity.")
            val bestBase = findBestBase(bases)

            if (bestBase != null) {
                Timber.d("Best Base selected: ${bestBase.packageName} (VC: ${bestBase.versionCode})")
            } else {
                Timber.w("No BaseEntity found in Multi-App Mode!")
            }

            return entities.map { entity ->
                SelectInstallEntity(entity, selected = (entity == bestBase))
            }
        }

        // Special handling for single Split input
        if (entities.size == 1 && entities.first() is AppEntity.SplitEntity) {
            Timber.d("Single Split detected. User explicitly provided this file. Force selecting it.")
            return listOf(SelectInstallEntity(entities.first(), selected = true))
        }

        // 3. Single App Mode: Base + Calculated Splits
        Timber.d("Single App Mode detected. Calculating optimal splits.")

        // Extract all splits to analyze them globally
        val allSplits = entities.filterIsInstance<AppEntity.SplitEntity>()
        val selectedSplits = if (splitChooseAll) {
            Timber.d("Split Selection: Config 'splitChooseAll' is TRUE. Selecting all ${allSplits.size} splits.")
            allSplits.toSet()
        } else {
            Timber.d("Split Selection: Config 'splitChooseAll' is FALSE. Calculating optimal splits.")
            selectOptimalSplits(allSplits)
        }

        Timber.d("Selected ${selectedSplits.size} splits out of ${allSplits.size} available.")

        return entities.map { entity ->
            val isSelected = when (entity) {
                is AppEntity.BaseEntity -> apkChooseAll // Select Base APK based on apkChooseAll setting
                is AppEntity.DexMetadataEntity -> true // Metadata is always selected
                is AppEntity.SplitEntity -> entity in selectedSplits // Only optimal splits are selected
                is AppEntity.ModuleEntity -> true // Modules are usually selected
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
        if (splits.isEmpty()) {
            Timber.d("No splits to select from.")
            return emptySet()
        }

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

        Timber.d("Split Selection [ABI]: Available=$availableAbis, Device=$deviceAbis, Target=$targetAbi")

        // A2. Target Density
        val availableDensities = splits
            .filter { it.filterType == FilterType.DENSITY }
            .mapNotNull { it.configValue }
            .toSet()

        // Fix: Use .map { it.key } as defined in Density.kt ("xhdpi", etc.)
        val deviceDensities = RsConfig.supportedDensities.map { it.key }
        val targetDensity = findBestDeviceMatch(availableDensities, deviceDensities)

        Timber.d("Split Selection [Density]: Available=$availableDensities, Device=$deviceDensities, Target=$targetDensity")

        // A3. Target Languages (Can be multiple)
        val availableLangs = splits
            .filter { it.filterType == FilterType.LANGUAGE }
            .mapNotNull { it.configValue }
            .toSet()
        val targetLangs = findBestLanguageMatches(availableLangs)

        Timber.d("Split Selection [Language]: Available=$availableLangs, Selected=$targetLangs")

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

    private fun findBestDeviceMatch(candidates: Set<String>, devicePreferences: List<String>): String? {
        if (candidates.isEmpty()) return null
        // Return the first device preference that is actually available in the APKs
        return devicePreferences.firstOrNull { it in candidates }
    }

    private fun findBestLanguageMatches(candidates: Set<String>): Set<String> {
        if (candidates.isEmpty()) return emptySet()
        val deviceLangs = RsConfig.supportedLocales.map { it.convertLegacyLanguageCode() }
        val selected = mutableSetOf<String>()

        // 1. Exact Matches (e.g. "zh-cn" == "zh-cn")
        val exactMatches = candidates.filter { lang -> deviceLangs.contains(lang) }
        selected.addAll(exactMatches)

        // 2. Fuzzy Matches (e.g. Device "zh-cn" matches APK "zh")
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
        Timber.d("Selecting best base from ${bases.size} candidates. Device ABIs: $deviceAbis")

        // Sort bases by Architecture Preference -> Version Code -> Version Name
        val sorted = bases.sortedWith(
            compareBy<AppEntity.BaseEntity> { base ->
                val abiIndex = base.arch?.let { deviceAbis.indexOf(it.arch) } ?: -1
                // -1 implies incompatible or unknown, put at the end
                if (abiIndex == -1) Int.MAX_VALUE else abiIndex
            }
                .thenByDescending { it.versionCode }
                .thenByDescending { it.versionName }
        )

        // Log top candidate for debugging
        sorted.firstOrNull()?.let {
            Timber.d("Best base candidate found: ${it.packageName} (Arch=${it.arch}, Ver=${it.versionCode})")
        }

        return sorted.firstOrNull()
    }
}