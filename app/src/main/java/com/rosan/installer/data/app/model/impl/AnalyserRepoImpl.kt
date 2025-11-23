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
import com.rosan.installer.data.app.model.impl.analyser.MixedModuleZipAnalyserRepoImpl
import com.rosan.installer.data.app.model.impl.analyser.ModuleZipAnalyserRepoImpl
import com.rosan.installer.data.app.model.impl.analyser.MultiApkZipAnalyserRepoImpl
import com.rosan.installer.data.app.model.impl.analyser.XApkAnalyserRepoImpl
import com.rosan.installer.data.app.repo.AnalyserRepo
import com.rosan.installer.data.app.repo.FileAnalyserRepo
import com.rosan.installer.data.app.util.calculateSHA256
import com.rosan.installer.data.app.util.sourcePath
import com.rosan.installer.data.installer.model.entity.SelectInstallEntity
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.util.convertLegacyLanguageCode
import com.rosan.installer.util.isLanguageCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile

object AnalyserRepoImpl : AnalyserRepo {
    override suspend fun doWork(
        config: ConfigEntity,
        data: List<DataEntity>,
        extra: AnalyseExtraEntity
    ): List<PackageAnalysisResult> = coroutineScope {
        // 1. ANALYZE: Parse files in parallel
        val analysers: Map<DataType, FileAnalyserRepo> = mapOf(
            DataType.APK to ApkAnalyserRepoImpl,
            DataType.APKS to ApksAnalyserRepoImpl,
            DataType.APKM to ApkMAnalyserRepoImpl,
            DataType.XAPK to XApkAnalyserRepoImpl,
            DataType.MULTI_APK_ZIP to MultiApkZipAnalyserRepoImpl,
            DataType.MODULE_ZIP to ModuleZipAnalyserRepoImpl,
            DataType.MIXED_MODULE_APK to MixedModuleApkAnalyserRepoImpl,
            DataType.MIXED_MODULE_ZIP to MixedModuleZipAnalyserRepoImpl
        )

        val rawEntities = data.map { dataEntity ->
            async(Dispatchers.IO) {
                val fileType = getDataType(config, dataEntity, extra)
                if (fileType == DataType.NONE) {
                    // 优化点 1：日志中使用 sourcePath()，更直观
                    Timber.w("Skipping unsupported file: ${dataEntity.sourcePath()}")
                    return@async emptyList<AppEntity>()
                }

                val analyser = analysers[fileType]
                    ?: throw Exception("No analyser found for data type: '$fileType'")

                try {
                    analyser.doWork(config, listOf(dataEntity), extra.copy(dataType = fileType))
                } catch (e: Exception) {
                    // 优化点 1：日志中使用 sourcePath()
                    Timber.e(e, "Failed to analyze file: ${dataEntity.sourcePath()}")
                    emptyList()
                }
            }
        }.awaitAll().flatten()

        if (rawEntities.isEmpty()) return@coroutineScope emptyList()

        // PRE-PROCESS: Group, Deduplicate, and Fetch System Info in PARALLEL
        val packageGroups = rawEntities.groupBy { it.packageName }

        data class ProcessedGroup(
            val packageName: String,
            val entities: List<AppEntity>,
            val installedInfo: InstalledAppInfo?
        )

        val processedGroups = packageGroups.map { (packageName, entities) ->
            async(Dispatchers.IO) {
                // Deduplication (Lazy Hashing)
                val baseEntities = entities.filterIsInstance<AppEntity.BaseEntity>()
                val uniqueEntities = if (baseEntities.size > 1) {
                    Timber.d("Duplicate base entities found for $packageName. Calculating hashes to deduplicate.")

                    val hashedBases = baseEntities.map { entity ->
                        async {
                            val path = entity.data.sourcePath()
                            val hash = path?.let { File(it).calculateSHA256() }
                            entity.copy(fileHash = hash)
                        }
                    }.awaitAll()

                    val distinctBases = hashedBases.distinctBy { it.fileHash }
                    distinctBases + entities.filter { it !is AppEntity.BaseEntity }
                } else {
                    entities
                }

                // B. Fetch Installed App Info
                val installedInfo = InstalledAppInfo.buildByPackageName(packageName)

                ProcessedGroup(packageName, uniqueEntities, installedInfo)
            }
        }.awaitAll()

        // 3. DECIDE: Determine Global Session Type
        val allEntities = processedGroups.flatMap { it.entities }

        val isMultiPackage = processedGroups.size > 1
        val hasMultipleBasesInSinglePackage = !isMultiPackage &&
                allEntities.count { it is AppEntity.BaseEntity } > 1

        // 优化点 3：使用 sourcePath() 来判断所有 Entity 是否来自同一个源文件
        // mapNotNull 过滤掉了无法获取路径的内存对象（如果有的话），distinct 比较路径字符串比比较 DataEntity 对象更准确
        val sourcePaths = allEntities.mapNotNull { it.data.sourcePath() }.distinct()
        val isFromSingleFile = sourcePaths.size == 1

        val firstContainerType = allEntities.firstOrNull()?.containerType

        val isMultiAppSession = if (isFromSingleFile &&
            (firstContainerType == DataType.MIXED_MODULE_APK || firstContainerType == DataType.MIXED_MODULE_ZIP)
        ) {
            false
        } else {
            isMultiPackage || hasMultipleBasesInSinglePackage
        }

        val sessionContainerType = if (isMultiAppSession) {
            if (firstContainerType == DataType.MULTI_APK_ZIP) DataType.MULTI_APK_ZIP else DataType.MULTI_APK
        } else {
            firstContainerType ?: DataType.APK
        }

        Timber.d("Analysis Decision: MultiApp=$isMultiAppSession, Type=$sessionContainerType, Source=${if (isFromSingleFile) "SingleFile" else "MultiFile"}")

        // BUILD: Construct Final Results
        val finalResults = processedGroups.map { group ->
            val packageName = group.packageName
            val entities = group.entities
            val installedAppInfo = group.installedInfo

            val baseEntity = entities.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
            val newApkSignatureHash = baseEntity?.signatureHash

            val signatureMatchStatus = when {
                installedAppInfo == null -> SignatureMatchStatus.NOT_INSTALLED
                newApkSignatureHash.isNullOrBlank() || installedAppInfo.signatureHash.isNullOrBlank() -> SignatureMatchStatus.UNKNOWN_ERROR
                newApkSignatureHash == installedAppInfo.signatureHash -> SignatureMatchStatus.MATCH
                else -> SignatureMatchStatus.MISMATCH
            }

            val authoritativeTargetSdk = baseEntity?.targetSdk
            val correctedEntities = entities.map { entity ->
                var corrected = when (entity) {
                    is AppEntity.BaseEntity -> entity.copy(containerType = sessionContainerType)
                    is AppEntity.SplitEntity -> entity.copy(containerType = sessionContainerType)
                    is AppEntity.DexMetadataEntity -> entity.copy(containerType = sessionContainerType)
                    is AppEntity.CollectionEntity -> entity.copy(containerType = sessionContainerType)
                    is AppEntity.ModuleEntity -> entity.copy(containerType = sessionContainerType)
                }

                if (authoritativeTargetSdk != null) {
                    corrected = when (corrected) {
                        is AppEntity.SplitEntity -> corrected.copy(targetSdk = authoritativeTargetSdk)
                        is AppEntity.DexMetadataEntity -> corrected.copy(targetSdk = authoritativeTargetSdk)
                        else -> corrected
                    }
                }
                corrected
            }

            val selectableEntities = determineDefaultSelections(correctedEntities, sessionContainerType)

            PackageAnalysisResult(
                packageName = packageName,
                appEntities = selectableEntities,
                installedAppInfo = installedAppInfo,
                signatureMatchStatus = signatureMatchStatus
            )
        }

        return@coroutineScope finalResults
    }

    /**
     * Determines the data type of a given file.
     */
    private fun getDataType(config: ConfigEntity, data: DataEntity, extra: AnalyseExtraEntity): DataType {
        val fileEntity = data as? DataEntity.FileEntity
            ?: throw IllegalArgumentException("AnalyserRepoImpl expected a FileEntity, but got ${data::class.simpleName}")

        return try {
            ZipFile(fileEntity.path).use { zipFile ->

                // OPTIMIZATION: Retrieve entries once as a list to avoid traversing the zip central directory multiple times.
                // This is crucial for performance on large zip files (like XAPKs or huge flashable zips).
                val entries = zipFile.entries().asSequence().toList()

                // --- Check for module-related types ONLY if the feature is enabled.
                if (extra.isModuleFlashEnabled) {
                    // Check logic using the pre-fetched entries list
                    val hasModuleProp = entries.any {
                        it.name == "module.prop" || it.name == "common/module.prop"
                    }

                    if (hasModuleProp) {
                        val hasAndroidManifest = zipFile.getEntry("AndroidManifest.xml") != null
                        val hasApksInside = entries.any { !it.isDirectory && it.name.endsWith(".apk", ignoreCase = true) }

                        return@use when {
                            hasAndroidManifest -> {
                                Timber.d("Found module.prop and AndroidManifest.xml, it's a MIXED_MODULE_APK.")
                                DataType.MIXED_MODULE_APK
                            }

                            hasApksInside -> {
                                Timber.d("Found module.prop and internal APK(s), it's a MIXED_MODULE_ZIP.")
                                DataType.MIXED_MODULE_ZIP
                            }

                            else -> {
                                Timber.d("Found module.prop, it's a MODULE_ZIP.")
                                DataType.MODULE_ZIP
                            }
                        }
                    }
                } else {
                    Timber.d("Module flashing is disabled. Skipping module-specific checks.")
                }

                val hasAndroidManifest = zipFile.getEntry("AndroidManifest.xml") != null
                if (hasAndroidManifest) {
                    Timber.d("Found AndroidManifest.xml at root, it's an APK.")
                    return@use DataType.APK
                }

                if (zipFile.getEntry("info.json") != null) {
                    val isApkm = isGenuineApkmInfo(zipFile, zipFile.getEntry("info.json")!!)
                    Timber.d("Found info.json at root, it's ${if (isApkm) "APKM" else "APKS"}.")
                    return@use if (isApkm) DataType.APKM else DataType.APKS
                }

                if (zipFile.getEntry("manifest.json") != null) {
                    Timber.d("Found manifest.json at root, it's an XAPK.")
                    return@use DataType.XAPK
                }

                // OPTIMIZATION: Reuse the 'entries' list we fetched earlier.
                var hasBaseApk = false
                var hasSplitApk = false

                for (entry in entries) {
                    if (entry.isDirectory || !entry.name.endsWith(".apk", ignoreCase = true)) continue
                    val entryName = File(entry.name).name
                    if (entryName == "base.apk" || entryName.startsWith("base-master"))
                        hasBaseApk = true
                    else
                        hasSplitApk = true
                    // Break early if both are found
                    if (hasBaseApk && hasSplitApk) break
                }

                if (hasBaseApk && hasSplitApk) {
                    Timber.d("Detected APKS structure without a manifest.")
                    return@use DataType.APKS
                }

                if (entries.any { !it.isDirectory && it.name.endsWith(".apk", ignoreCase = true) }) {
                    Timber.d("Detected as a generic ZIP with APKs (MULTI_APK_ZIP).")
                    return@use DataType.MULTI_APK_ZIP
                }

                Timber.d("Could not determine file type for ${fileEntity.path}, returning NONE.")
                return@use DataType.NONE
            }
        } catch (e: ZipException) {
            Timber.w(e, "File is a malformed zip, but assuming it is APK for compatibility: ${fileEntity.path}")
            DataType.APK
        } catch (e: IOException) {
            Timber.e(e, "Unexpected IO error while reading zip file, marking as unsupported: ${fileEntity.path}")
            DataType.NONE
        }
    }

    private fun isGenuineApkmInfo(zipFile: ZipFile, entry: ZipEntry): Boolean =
        try {
            zipFile.getInputStream(entry).use { inputStream ->
                val content = inputStream.bufferedReader().use(BufferedReader::readText)
                val jsonElement = Json.parseToJsonElement(content)
                jsonElement is JsonObject && jsonElement.containsKey("apkm_version")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse info.json as genuine APKM info.")
            false
        }

    private fun determineDefaultSelections(
        entities: List<AppEntity>,
        containerType: DataType?
    ): List<SelectInstallEntity> {
        val finalSelectEntities = mutableListOf<SelectInstallEntity>()
        val isMultiAppMode = containerType == DataType.MULTI_APK || containerType == DataType.MULTI_APK_ZIP

        if (containerType == DataType.MIXED_MODULE_APK || containerType == DataType.MIXED_MODULE_ZIP) {
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
            // Only calculate optimal splits if there are splits to process
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
        val value: String,
        val entity: AppEntity.SplitEntity
    )

    private fun AppEntity.SplitEntity.getCleanConfigValue(): String {
        val nameWithoutApk = this.splitName.removeSuffix(".apk")

        val strippedPrefix = nameWithoutApk
            .removePrefix("split_")
            .removePrefix("base-")

        val configValue = if (strippedPrefix.contains(".config.")) {
            strippedPrefix.substringAfterLast(".config.")
        } else if (strippedPrefix.startsWith("config.")) {
            strippedPrefix.removePrefix("config.")
        } else {
            strippedPrefix
        }

        // Timber.v("-> Parsing split '${this.splitName}': raw='${configValue}'") // Reduce log noise
        return configValue
    }

    private fun findOptimalSplits(splits: List<AppEntity.SplitEntity>): List<AppEntity.SplitEntity> {
        if (splits.isEmpty()) return emptyList()

        Timber.d("--- Begin findOptimalSplits for ${splits.size} splits ---")

        // Parse and categorize all splits
        // OPTIMIZATION: Perform mapping once and group immediately.
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

            val normalizedValue = rawConfigValue.replace('_', '-')
            ParsedSplit(category, normalizedValue, split)
        }

        val categorized = parsedSplits.groupBy { it.category }
        val optimalSplits = mutableListOf<AppEntity.SplitEntity>()

        // Select BEST ABI
        categorized[SplitCategory.ABI]?.let { abiSplits ->
            val abiSplitsGroups = abiSplits.groupBy { it.value }
            val deviceAbis = RsConfig.supportedArchitectures.map { it.arch }
            val normalizedDeviceAbis = deviceAbis.map { it.replace('_', '-') }

            // Find the single best ABI that is available
            val bestAbi = normalizedDeviceAbis.firstOrNull { abi -> abiSplitsGroups.containsKey(abi) }

            if (bestAbi != null) {
                Timber.d("   -> Best ABI match is '$bestAbi'.")
                abiSplitsGroups[bestAbi]?.forEach { optimalSplits.add(it.entity) }
            }
        }

        // Select BEST Density
        categorized[SplitCategory.DENSITY]?.let { densitySplits ->
            val densitySplitsGroups = densitySplits.groupBy { it.value }
            val deviceDensities = RsConfig.supportedDensities.map { it.key }

            val bestDensity = deviceDensities.firstOrNull { density -> densitySplitsGroups.containsKey(density) }

            if (bestDensity != null) {
                Timber.d("   -> Best Density match is '$bestDensity'.")
                densitySplitsGroups[bestDensity]?.forEach { optimalSplits.add(it.entity) }
            }
        }

        // Select BEST Language
        categorized[SplitCategory.LANGUAGE]?.let { langSplits ->
            val langSplitsGroups = langSplits.groupBy { it.value.convertLegacyLanguageCode() }
            val deviceLanguages = RsConfig.supportedLocales

            // OPTIMIZATION: Simplified logic to find best language match (Exact > Base)
            var bestLangMatch: String? = null

            // 1. Try Exact Match
            bestLangMatch = deviceLanguages.map { it.convertLegacyLanguageCode() }
                .firstOrNull { langSplitsGroups.containsKey(it) }

            // 2. If no exact match, Try Base Match (e.g., 'zh' matches 'zh-cn')
            if (bestLangMatch == null) {
                bestLangMatch = deviceLanguages.map { it.convertLegacyLanguageCode().substringBefore('-') }
                    .firstOrNull { baseLang -> langSplitsGroups.keys.any { it.startsWith(baseLang) } }
                // Note: The original logic selected the split key itself.
                // If we match base, we need to know WHICH split key matched.
                // Re-adopting original robust logic but cleaner:
                if (bestLangMatch != null) {
                    // Find the actual key in the map that corresponds to this base language
                    bestLangMatch = langSplitsGroups.keys.firstOrNull { it.startsWith(bestLangMatch!!) }
                }
            }

            if (bestLangMatch != null) {
                Timber.d("   -> Best Language match is '$bestLangMatch'.")
                langSplitsGroups[bestLangMatch]?.forEach { optimalSplits.add(it.entity) }
            }
        }

        // ALWAYS include all feature splits
        categorized[SplitCategory.FEATURE]?.forEach {
            // Timber.d("   -> Including feature split: '${it.value}'")
            optimalSplits.add(it.entity)
        }

        Timber.d("--- Finished findOptimalSplits. Total selected: ${optimalSplits.distinct().size} ---")
        return optimalSplits.distinct()
    }
}