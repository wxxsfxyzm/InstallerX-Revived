package com.rosan.installer.data.app.model.impl

import com.rosan.installer.build.Architecture
import com.rosan.installer.build.Density
import com.rosan.installer.build.RsConfig
import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.model.entity.DataType
import com.rosan.installer.data.app.model.entity.InstalledAppInfo
import com.rosan.installer.data.app.model.entity.PackageAnalysisResult
import com.rosan.installer.data.app.model.entity.SignatureMatchStatus
import com.rosan.installer.data.app.model.impl.analyser.ApkAnalyserRepoImpl
import com.rosan.installer.data.app.model.impl.analyser.ApkMAnalyserRepoImpl
import com.rosan.installer.data.app.model.impl.analyser.ApksAnalyserRepoImpl
import com.rosan.installer.data.app.model.impl.analyser.MixedModuleApkAnalyserRepoImpl
import com.rosan.installer.data.app.model.impl.analyser.ModuleZipAnalyserRepoImpl
import com.rosan.installer.data.app.model.impl.analyser.MultiApkZipAnalyserRepoImpl
import com.rosan.installer.data.app.model.impl.analyser.XApkAnalyserRepoImpl
import com.rosan.installer.data.app.repo.AnalyserRepo
import com.rosan.installer.data.app.repo.FileAnalyserRepo
import com.rosan.installer.data.app.util.calculateSHA256
import com.rosan.installer.data.installer.model.entity.SelectInstallEntity
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.util.convertLegacyLanguageCode
import com.rosan.installer.util.isLanguageCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile

object AnalyserRepoImpl : AnalyserRepo {
    override suspend fun doWork(
        config: ConfigEntity,
        data: List<DataEntity>,
        extra: AnalyseExtraEntity
    ): List<PackageAnalysisResult> {
        // --- Logic to analyse data and return a list of AppEntity ---
        // A list to hold all raw analysis results before final processing.
        val rawAppsWithoutHash = mutableListOf<AppEntity>()
        // A map of all available analysers for different file types.
        val analysers: Map<DataType, FileAnalyserRepo> = mapOf(
            DataType.APK to ApkAnalyserRepoImpl,
            DataType.APKS to ApksAnalyserRepoImpl,
            DataType.APKM to ApkMAnalyserRepoImpl,
            DataType.XAPK to XApkAnalyserRepoImpl,
            DataType.MULTI_APK_ZIP to MultiApkZipAnalyserRepoImpl,
            DataType.MODULE_ZIP to ModuleZipAnalyserRepoImpl,
            DataType.MIXED_MODULE_APK to MixedModuleApkAnalyserRepoImpl
        )
        // This loop processes every file/data source provided by the user.
        for (dataEntity in data) {
            // Determine the type of the current file (APK, APKS, etc.)
            val fileType = getDataType(config, dataEntity)
                ?: continue // If type is null for any reason, just skip to the next file.

            // --- FIX: Instead of throwing an exception, just skip the invalid file. ---
            if (fileType == DataType.NONE) {
                Timber.w("Skipping unsupported file: ${dataEntity.getSourceTop()}")
                continue // Continue to the next file in the loop.
            }

            // Get the appropriate analyser for this file type.
            val analyser = analysers[fileType]
                ?: throw Exception("No analyser found for data type: '$fileType'")

            // Run the specific analyser for this file.
            // We pass the file's own type for now. The final containerType will be decided later.
            val analysedEntities = analyser.doWork(
                config,
                listOf(dataEntity), // Pass only the current entity to the sub-analyser
                extra.copy(dataType = fileType)
            )
            rawAppsWithoutHash.addAll(analysedEntities)
        }

        // If after analyzing all files, no app entities were produced, return empty.
        if (rawAppsWithoutHash.isEmpty()) {
            return emptyList()
        }

        val needsDeDuplication = rawAppsWithoutHash
            .filterIsInstance<AppEntity.BaseEntity>()
            .groupBy { it.packageName }
            .any { it.value.size > 1 }

        // Calculate hash values for all entities if needed.
        val rawApps = if (needsDeDuplication) {
            Timber.d("Multiple base entities for the same package detected. Calculating file hashes for de-duplication.")
            rawAppsWithoutHash.map { entity ->
                if (entity is AppEntity.BaseEntity) {
                    val path = (entity.data as? DataEntity.FileEntity)?.path
                    val hash = path?.let { File(it).calculateSHA256() }
                    entity.copy(fileHash = hash)
                } else {
                    entity
                }
            }
        } else {
            Timber.d("Single install entity detected. Skipping hash calculation.")
            rawAppsWithoutHash
        }

        // --- Group the analysis results by package name ---
        // This is the key step to differentiate single-app vs multi-app installs.
        val groupedByPackage = rawApps.groupBy { it.packageName }

        // --- Determine the final installation type and correct entity properties ---
        val finalResults = mutableListOf<PackageAnalysisResult>()

        // A multi-app session is defined by having multiple packages, OR having multiple
        // base APKs within a single package (e.g., different versions of the same app).
        // However, a single MIXED_MODULE_APK can also produce multiple packages, but should be treated as a single session.
        val isMultiPackage = groupedByPackage.size > 1
        val hasMultipleBasesInSinglePackage = !isMultiPackage && rawApps.count { it is AppEntity.BaseEntity } > 1
        val isFromSingleFile = rawApps.map { entity ->
            when (entity) {
                is AppEntity.BaseEntity -> entity.data.getSourceTop()
                is AppEntity.SplitEntity -> entity.data.getSourceTop()
                is AppEntity.DexMetadataEntity -> entity.data.getSourceTop()
                is AppEntity.CollectionEntity -> entity.data.getSourceTop()
                is AppEntity.ModuleEntity -> entity.data.getSourceTop()
            }
        }.distinct().size == 1
        val originalContainerType = rawApps.firstOrNull()?.containerType

        val isMultiAppSession = if (isFromSingleFile && originalContainerType == DataType.MIXED_MODULE_APK) {
            false // SPECIAL CASE: A single mixed module/apk file is NOT a multi-app session.
        } else {
            isMultiPackage || hasMultipleBasesInSinglePackage // Use original logic for all other cases
        }

        val sessionContainerType = if (isMultiAppSession) {
            // This is a multi-app install scenario.
            if (isMultiPackage) {
                Timber.d("Determined install type: Multi-App (found ${groupedByPackage.size} unique packages).")
            } else {
                Timber.d("Determined install type: Multi-App (multiple base APKs found for a single package).")
            }
            // If the original source was a ZIP file containing multiple APKs, preserve its type.
            // Otherwise, it's a generic multi-APK session from multiple file shares.
            val originalContainerType = rawApps.firstOrNull()?.containerType
            if (originalContainerType == DataType.MULTI_APK_ZIP) {
                DataType.MULTI_APK_ZIP
            } else {
                DataType.MULTI_APK
            }
        } else {
            // This is a single-app install (e.g., one APK, or one base + splits).
            Timber.d("Determined install type: Single-App (all files for '${groupedByPackage.keys.first()}').")
            rawApps.first().containerType ?: DataType.APK
        }

        // Iterate over each package group to build the final result
        groupedByPackage.forEach { (packageName, entitiesInPackage) ->
            // --- CORE REFACTORING LOGIC ---
            // Fetch information about the currently installed package.
            // This logic is now part of the backend analysis, not the ViewModel.
            val installedAppInfo = InstalledAppInfo.buildByPackageName(packageName)

            // Find the base entity to get the new signature
            val baseEntity = entitiesInPackage.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
            val newApkSignatureHash = baseEntity?.signatureHash
            Timber.d("Signature hash for $packageName: $newApkSignatureHash")
            // --- SIGNATURE COMPARISON LOGIC ---
            val signatureMatchStatus = when {
                installedAppInfo == null -> {
                    // The app is not installed yet.
                    SignatureMatchStatus.NOT_INSTALLED
                }

                newApkSignatureHash.isNullOrBlank() || installedAppInfo.signatureHash.isNullOrBlank() -> {
                    // We failed to get a signature from either the APK or the installed app.
                    Timber.w("Could not compare signatures. New: '$newApkSignatureHash', Installed: '${installedAppInfo.signatureHash}'")
                    SignatureMatchStatus.UNKNOWN_ERROR
                }

                newApkSignatureHash == installedAppInfo.signatureHash -> {
                    // Signatures match, this is a safe update.
                    Timber.d("Signatures match for $packageName.")
                    SignatureMatchStatus.MATCH
                }

                else -> {
                    // Signatures DO NOT match. This is a potential issue.
                    Timber.w("SIGNATURE MISMATCH for $packageName. New: '$newApkSignatureHash', Installed: '${installedAppInfo.signatureHash}'")
                    SignatureMatchStatus.MISMATCH
                }
            }
            // --- END OF SIGNATURE LOGIC ---

            // --- Post-process all entities to apply corrections ---
            val correctedEntities = mutableListOf<AppEntity>()
            // moved upwards
            // val baseEntity = entitiesInPackage.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
            val authoritativeTargetSdk = baseEntity?.targetSdk

            entitiesInPackage.forEach { entity ->
                var correctedEntity = entity

                // Correct the containerType for all entities based on our final decision.
                correctedEntity = when (correctedEntity) {
                    is AppEntity.BaseEntity -> correctedEntity.copy(containerType = sessionContainerType)
                    is AppEntity.SplitEntity -> correctedEntity.copy(containerType = sessionContainerType)
                    is AppEntity.DexMetadataEntity -> correctedEntity.copy(containerType = sessionContainerType)
                    is AppEntity.CollectionEntity -> correctedEntity.copy(containerType = sessionContainerType)
                    is AppEntity.ModuleEntity -> correctedEntity.copy(containerType = sessionContainerType)
                }

                // Correct the targetSdk for splits and metadata if a base entity exists.
                if (authoritativeTargetSdk != null) {
                    correctedEntity = when (correctedEntity) {
                        is AppEntity.SplitEntity -> correctedEntity.copy(targetSdk = authoritativeTargetSdk)
                        is AppEntity.DexMetadataEntity -> correctedEntity.copy(targetSdk = authoritativeTargetSdk)
                        else -> correctedEntity // BaseEntity and others are already correct.
                    }
                }
                correctedEntities.add(correctedEntity)
            }
            // --- End of correction logic ---
            val selectableEntities = determineDefaultSelections(correctedEntities, sessionContainerType)
            // Create the final result object for this package
            finalResults.add(
                PackageAnalysisResult(
                    packageName = packageName,
                    appEntities = selectableEntities,
                    installedAppInfo = installedAppInfo,
                    signatureMatchStatus = signatureMatchStatus
                )
            )
        }

        return finalResults
    }

    /**
     * Determines the data type of a given file.
     * This method is now significantly simplified as it only needs to handle FileEntity.
     */
    private fun getDataType(config: ConfigEntity, data: DataEntity): DataType? {
        val fileEntity = data as? DataEntity.FileEntity
            ?: // This should not happen if the cache-first strategy is implemented correctly.
            throw IllegalArgumentException("AnalyserRepoImpl expected a FileEntity, but got ${data::class.simpleName}")

        return try {
            ZipFile(fileEntity.path).use { zipFile ->
                val hasModuleProp = zipFile.getEntry("module.prop") != null || zipFile.getEntry("common/module.prop") != null
                val hasAndroidManifest = zipFile.getEntry("AndroidManifest.xml") != null
                // Give module detection the highest priority.
                if (hasModuleProp) {
                    return@use if (hasAndroidManifest) {
                        Timber.d("Found module.prop and AndroidManifest.xml, it's a MIXED_MODULE_APK.")
                        DataType.MIXED_MODULE_APK
                    } else {
                        Timber.d("Found module.prop, it's a MODULE_ZIP.")
                        DataType.MODULE_ZIP
                    }
                }
                when {
                    hasAndroidManifest -> {
                        Timber.d("Found AndroidManifest.xml at root, it's an APK.")
                        return@use DataType.APK
                    }

                    zipFile.getEntry("info.json") != null -> {
                        val isApkm = isGenuineApkmInfo(zipFile, zipFile.getEntry("info.json")!!)
                        Timber.d("Found info.json at root, it's ${if (isApkm) "APKM" else "APKS"}.")
                        return@use if (isApkm) DataType.APKM else DataType.APKS
                    }

                    zipFile.getEntry("manifest.json") != null -> {
                        Timber.d("Found manifest.json at root, it's an XAPK.")
                        return@use DataType.XAPK
                    }
                }

                // If no manifest file is found, check for APKS file structure.
                var hasBaseApk = false
                var hasSplitApk = false
                val entries = zipFile.entries().toList()
                for (entry in entries) {
                    if (entry.isDirectory || !entry.name.endsWith(".apk", ignoreCase = true)) continue
                    // Check only the filename, ignoring the path.
                    val entryName = File(entry.name).name
                    // The base APK could be base.apk, base-master.apk, etc.
                    if (entryName == "base.apk" || entryName.startsWith("base-master"))
                        hasBaseApk = true
                    else
                    // Any other APK file found alongside a base APK implies it's a split.
                        hasSplitApk = true

                    // Exit early if both conditions are met.
                    if (hasBaseApk && hasSplitApk) break
                }

                if (hasBaseApk && hasSplitApk) {
                    Timber.d("Detected APKS structure without a manifest.")
                    return@use DataType.APKS
                }

                // Finally, check if it's a generic ZIP containing any APK files.
                if (entries.any { !it.isDirectory && it.name.endsWith(".apk", ignoreCase = true) }) {
                    Timber.d("Detected as a generic ZIP with APKs (MULTI_APK_ZIP).")
                    return@use DataType.MULTI_APK_ZIP
                }

                Timber.d("Could not determine file type for ${fileEntity.path}, returning NONE.")
                return@use DataType.NONE
            }
        } catch (e: ZipException) {
            // CATCH: This is a malformed ZIP file.
            // Assume it's a problematic APK and try to process it as such.
            Timber.w(e, "File is a malformed zip, but assuming it is APK for compatibility: ${fileEntity.path}")
            DataType.APK
        } catch (e: IOException) {
            // CATCH for other IO errors: Though ActionHandler should prevent EACCES from reaching here,
            // this serves as a final safety net for other potential IO issues.
            Timber.e(e, "Unexpected IO error while reading zip file, marking as unsupported: ${fileEntity.path}")
            DataType.NONE // Mark it as an unsupported type.
        }
    }

    /**
     * Uses the ZipFile API to check if the info.json in the APKM is genuine.
     *
     * @author wxxsfxyzm
     * @param zipFile ZipFile Object
     * @param entry ZipEntry (info.json) to check
     * @return true if the info.json is a genuine APKM info file.
     */
    private fun isGenuineApkmInfo(zipFile: ZipFile, entry: ZipEntry): Boolean {
        return try {
            zipFile.getInputStream(entry).use { inputStream ->
                val content = inputStream.bufferedReader().use(BufferedReader::readText)

                // Use kotlinx.serialization to parse the JSON content.
                val jsonElement = Json.parseToJsonElement(content)

                // Check if the JSON element is an object and contains the "apkm_version" key.
                jsonElement is JsonObject && jsonElement.containsKey("apkm_version")
            }
        } catch (e: Exception) {
            // Catch all exceptions to ensure we handle any issues gracefully.
            // This includes IO exceptions, parsing errors, and serialization exceptions.
            Timber.e(e, "Failed to parse info.json as genuine APKM info.")
            false
        }
    }

    /**
     * Determines the default selection state for a list of entities.
     * This method contains the business logic moved from ActionHandler.
     */
    private fun determineDefaultSelections(
        entities: List<AppEntity>,
        containerType: DataType?
    ): List<SelectInstallEntity> {
        val finalSelectEntities = mutableListOf<SelectInstallEntity>()
        val isMultiAppMode = containerType == DataType.MULTI_APK || containerType == DataType.MULTI_APK_ZIP
        if (containerType == DataType.MIXED_MODULE_APK) {
            Timber.d("Mixed Module/APK detected. Setting all entities to be de-selected by default.")
            return entities.map { SelectInstallEntity(it, selected = false) }
        }
        if (isMultiAppMode) {
            // --- Multi App Mode Logic ---
            val allBaseEntities = entities.filterIsInstance<AppEntity.BaseEntity>()
            val uniqueBaseEntities = allBaseEntities.distinctBy { it.fileHash }

            if (uniqueBaseEntities.isEmpty()) {
                finalSelectEntities.addAll(entities.map { SelectInstallEntity(it, selected = true) })
            } else {
                val deviceAbis = RsConfig.supportedArchitectures.map { it.arch }
                val sortedBases = uniqueBaseEntities.sortedWith(
                    compareBy<AppEntity.BaseEntity> {
                        val abiIndex = it.arch?.let { arch -> deviceAbis.indexOf(arch.arch) } ?: -1
                        if (abiIndex == -1) Int.MAX_VALUE else abiIndex
                    }
                        .thenByDescending { it.versionCode }
                        .thenByDescending { it.versionName }
                )
                val bestInGroup = sortedBases.firstOrNull()
                finalSelectEntities.addAll(uniqueBaseEntities.map {
                    SelectInstallEntity(it, selected = (it == bestInGroup))
                })
            }
        } else {
            // --- Single App Mode Logic ---
            val splits = entities.filterIsInstance<AppEntity.SplitEntity>()
            val optimalSplits = if (splits.isNotEmpty()) findOptimalSplits(splits).toSet() else emptySet()

            val processedEntities = entities.map { entity ->
                val isSelected = when (entity) {
                    is AppEntity.BaseEntity,
                    is AppEntity.DexMetadataEntity -> true

                    is AppEntity.SplitEntity -> entity in optimalSplits
                    is AppEntity.CollectionEntity -> false
                    is AppEntity.ModuleEntity -> true
                }
                SelectInstallEntity(app = entity, selected = isSelected)
            }
            finalSelectEntities.addAll(processedEntities)
        }
        return finalSelectEntities
    }

    private enum class SplitCategory { ABI, DENSITY, LANGUAGE, FEATURE }

    private data class ParsedSplit(
        val category: SplitCategory,
        val value: String, // The extracted value, e.g., "arm64-v8a", "xhdpi", "en"
        val entity: AppEntity.SplitEntity
    )

    /**
     * Parses a raw split name into a clean configuration value.
     * This logic is now robust against filenames containing dots.
     *
     * Example transformations:
     * - "split_config.arm64_v8a.apk" -> "arm64_v8a"
     * - "split_phonesky_data_loader.config.arm64_v8a.apk" -> "arm64_v8a"
     * - "split_config_zh.apk" -> "zh"
     * - "split_phonesky_data_loader.apk" -> "phonesky_data_loader"
     */
    private fun AppEntity.SplitEntity.getCleanConfigValue(): String {
        // Safely remove only the .apk suffix
        val nameWithoutApk = this.splitName.removeSuffix(".apk")

        // Remove the standard split_ prefix
        val strippedPrefix = nameWithoutApk
            .removePrefix("split_")
            .removePrefix("base-")

        // Extract the actual config value. This is the crucial part.
        // If ".config." exists, take the part after the last occurrence.
        // Otherwise, if it starts with "config.", remove that prefix.
        // If neither, use the whole string (for feature splits like "phonesky_data_loader").
        val configValue = if (strippedPrefix.contains(".config.")) {
            strippedPrefix.substringAfterLast(".config.")
        } else if (strippedPrefix.startsWith("config.")) {
            strippedPrefix.removePrefix("config.")
        } else {
            strippedPrefix
        }

        Timber.d("-> Parsing split '${this.splitName}': raw='${configValue}'")
        return configValue
    }

    /**
     * Finds the most suitable split APKs from a list based on the current device's configuration.
     * This version implements the final, correct selection logic for each category.
     *
     * @param splits The list of all available SplitEntity objects.
     * @return A list containing only the best-matching splits for each category.
     */
    private fun findOptimalSplits(splits: List<AppEntity.SplitEntity>): List<AppEntity.SplitEntity> {
        if (splits.isEmpty()) return emptyList()

        Timber.d("--- Begin findOptimalSplits for ${splits.size} splits ---")

        val isoLanguages = Locale.getISOLanguages().toSet()

        // Parse and categorize all splits
        val parsedSplits = splits.mapNotNull { split ->
            val rawConfigValue = split.getCleanConfigValue()
            if (rawConfigValue.isEmpty()) {
                Timber.w("Parsed empty config value from split: '${split.splitName}', skipping.")
                return@mapNotNull null
            }

            val category = when {
                Architecture.fromArchString(rawConfigValue) != Architecture.UNKNOWN -> SplitCategory.ABI
                Density.fromDensityString(rawConfigValue) != Density.UNKNOWN -> SplitCategory.DENSITY
                isLanguageCode(
                    rawConfigValue.convertLegacyLanguageCode().substringBefore('-')
                ) -> SplitCategory.LANGUAGE

                else -> SplitCategory.FEATURE
            }
            Timber.d("   > Categorized '$rawConfigValue' (from ${split.splitName}) as $category")

            val normalizedValue = rawConfigValue.replace('_', '-')
            ParsedSplit(category, normalizedValue, split)
        }

        val categorized = parsedSplits.groupBy { it.category }
        val optimalSplits = mutableListOf<AppEntity.SplitEntity>()

        // Select BEST ABI
        val abiSplitsGroups = categorized[SplitCategory.ABI]?.groupBy { it.value } ?: emptyMap()
        if (abiSplitsGroups.isNotEmpty()) {
            val deviceAbis = RsConfig.supportedArchitectures.map { it.arch }
            Timber.d("Device ABIs (priority order): $deviceAbis")
            Timber.d("Available ABI splits: ${abiSplitsGroups.keys}")

            val normalizedDeviceAbis = deviceAbis.map { it.replace('_', '-') }
            // Find the single best ABI that is available
            val bestAbi = normalizedDeviceAbis.firstOrNull { abi -> abiSplitsGroups.containsKey(abi) }

            if (bestAbi != null) {
                Timber.d("   -> Best ABI match is '$bestAbi'. Selecting all splits for this ABI.")
                // Add all splits for that best ABI ONLY
                abiSplitsGroups.getValue(bestAbi).forEach { optimalSplits.add(it.entity) }
            } else {
                Timber.w("No matching ABI splits found for this device's architectures.")
            }
        }

        // Select BEST Density
        val densitySplitsGroups = categorized[SplitCategory.DENSITY]?.groupBy { it.value } ?: emptyMap()
        if (densitySplitsGroups.isNotEmpty()) {
            val deviceDensities = RsConfig.supportedDensities.map { it.key }
            Timber.d("Device Densities (priority order): $deviceDensities")
            Timber.d("Available Density splits: ${densitySplitsGroups.keys}")

            val bestDensity = deviceDensities.firstOrNull { density -> densitySplitsGroups.containsKey(density) }

            if (bestDensity != null) {
                Timber.d("   -> Best Density match is '$bestDensity'. Selecting.")
                densitySplitsGroups.getValue(bestDensity).forEach { optimalSplits.add(it.entity) }
            }
            // Include 'nodpi' if it exists, as it's universal
            // TODO is there a nodpi split ?
            /*densitySplitsGroups["nodpi"]?.let {
                Timber.d("   -> Found 'nodpi' split(s). Selecting.")
                it.forEach { p -> optimalSplits.add(p.entity) }
            }*/
        }

        // Select BEST Language
        val langSplitsGroups =
            categorized[SplitCategory.LANGUAGE]?.groupBy { it.value.convertLegacyLanguageCode() } ?: emptyMap()
        if (langSplitsGroups.isNotEmpty()) {
            val applicationLanguages = langSplitsGroups.keys
            Timber.d("Available Language splits: $applicationLanguages")
            val deviceLanguages = RsConfig.supportedLocales
            Timber.d("Device Languages (priority order): $deviceLanguages")
            var langFound = false
            // First, try for an exact match (e.g., 'zh-cn')
            for (lang in deviceLanguages) {
                val modernLang = lang.convertLegacyLanguageCode()
                if (langSplitsGroups.containsKey(modernLang)) {
                    Timber.d("   -> Found best Language match (exact): '$modernLang'. Selecting.")
                    langSplitsGroups.getValue(modernLang).forEach { optimalSplits.add(it.entity) }
                    langFound = true
                    break
                }
            }
            // If no exact match, try for a base language match (e.g., 'zh')
            if (!langFound) {
                for (lang in deviceLanguages) {
                    val baseLang = lang.convertLegacyLanguageCode().substringBefore('-')
                    if (langSplitsGroups.containsKey(baseLang)) {
                        Timber.d("   -> Found best Language match (base): '$baseLang'. Selecting.")
                        langSplitsGroups.getValue(baseLang).forEach { optimalSplits.add(it.entity) }
                        langFound = true
                        break
                    }
                }
            }
        }

        // ALWAYS include all feature splits
        categorized[SplitCategory.FEATURE]?.forEach {
            Timber.d("   -> Including feature split: '${it.value}'")
            optimalSplits.add(it.entity)
        }

        Timber.d("--- Finished findOptimalSplits. Total selected: ${optimalSplits.distinct().size} ---")
        val result = optimalSplits.distinct()
        result.forEach { Timber.d("   >> Final Selected: ${it.splitName}") }
        return result
    }
}