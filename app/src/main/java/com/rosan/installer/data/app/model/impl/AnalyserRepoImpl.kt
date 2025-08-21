package com.rosan.installer.data.app.model.impl

import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.model.entity.DataType
import com.rosan.installer.data.app.model.exception.AnalyseFailedAllFilesUnsupportedException
import com.rosan.installer.data.app.model.impl.analyser.ApkAnalyserRepoImpl
import com.rosan.installer.data.app.model.impl.analyser.ApkMAnalyserRepoImpl
import com.rosan.installer.data.app.model.impl.analyser.ApksAnalyserRepoImpl
import com.rosan.installer.data.app.model.impl.analyser.MultiApkZipAnalyserRepoImpl
import com.rosan.installer.data.app.model.impl.analyser.XApkAnalyserRepoImpl
import com.rosan.installer.data.app.repo.AnalyserRepo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile

object AnalyserRepoImpl : AnalyserRepo {
    override suspend fun doWork(
        config: ConfigEntity,
        data: List<DataEntity>,
        extra: AnalyseExtraEntity
    ): List<AppEntity> {
        // --- Logic to analyse data and return a list of AppEntity ---
        // A list to hold all raw analysis results before final processing.
        val rawApps = mutableListOf<AppEntity>()
        // A map of all available analysers for different file types.
        val analysers = mapOf(
            DataType.APK to ApkAnalyserRepoImpl,
            DataType.APKS to ApksAnalyserRepoImpl,
            DataType.APKM to ApkMAnalyserRepoImpl,
            DataType.XAPK to XApkAnalyserRepoImpl,
            DataType.MULTI_APK_ZIP to MultiApkZipAnalyserRepoImpl
        )
        // This loop processes every file/data source provided by the user.
        for (dataEntity in data) {
            // Determine the type of the current file (APK, APKS, etc.)
            val fileType = getDataType(config, dataEntity)
                ?: throw AnalyseFailedAllFilesUnsupportedException("Unrecognized file type for: ${dataEntity.getSourceTop()}")

            if (fileType == DataType.NONE) {
                throw AnalyseFailedAllFilesUnsupportedException("Not a valid installer file: ${dataEntity.getSourceTop()}")
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
            rawApps.addAll(analysedEntities)
        }

        // If after analyzing all files, no app entities were produced, return empty.
        if (rawApps.isEmpty()) {
            return emptyList()
        }

        // --- Group the analysis results by package name ---
        // This is the key step to differentiate single-app vs multi-app installs.
        val groupedByPackage = rawApps.groupBy { it.packageName }

        // --- Determine the final installation type and correct entity properties ---
        val finalApps = mutableListOf<AppEntity>()

        // Check if all analyzed files belong to one single package name.
        val isSinglePackageInstall = groupedByPackage.size == 1

        val sessionContainerType = if (isSinglePackageInstall) {
            // All files are for the same app. This is a single-app install (with splits).
            // The container type should be what the first entity was analyzed as (e.g., APK, APKS).
            // This ensures downstream logic (like findOptimalSplits) is triggered correctly.
            Timber.d("Determined install type: Single-App (all files for '${groupedByPackage.keys.first()}').")
            rawApps.first().containerType ?: DataType.APK
        } else {
            // Files belong to multiple different apps. This is a multi-app install.
            Timber.d("Determined install type: Multi-App (found ${groupedByPackage.size} unique packages).")
            DataType.MULTI_APK
        }

        // --- Post-process all entities to apply corrections ---
        groupedByPackage.forEach { (_, entitiesInPackage) ->
            // Find the BaseEntity in the group to use its info as the source of truth.
            val baseEntity = entitiesInPackage.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
            val authoritativeTargetSdk = baseEntity?.targetSdk

            // Correct each entity within the package group.
            entitiesInPackage.forEach { entity ->
                var correctedEntity = entity

                // Correct the containerType for all entities based on our final decision.
                correctedEntity = when (correctedEntity) {
                    is AppEntity.BaseEntity -> correctedEntity.copy(containerType = sessionContainerType)
                    is AppEntity.SplitEntity -> correctedEntity.copy(containerType = sessionContainerType)
                    is AppEntity.DexMetadataEntity -> correctedEntity.copy(containerType = sessionContainerType)
                    is AppEntity.CollectionEntity -> correctedEntity.copy(containerType = sessionContainerType)
                }

                // Correct the targetSdk for splits and metadata if a base entity exists.
                if (authoritativeTargetSdk != null) {
                    correctedEntity = when (correctedEntity) {
                        is AppEntity.SplitEntity -> correctedEntity.copy(targetSdk = authoritativeTargetSdk)
                        is AppEntity.DexMetadataEntity -> correctedEntity.copy(targetSdk = authoritativeTargetSdk)
                        else -> correctedEntity // BaseEntity and others are already correct.
                    }
                }
                finalApps.add(correctedEntity)
            }
        }

        return finalApps
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
                // First, check for standard manifest files at the root.
                when {
                    zipFile.getEntry("AndroidManifest.xml") != null -> {
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
                    if (entry.isDirectory) continue
                    // Check only the filename, ignoring the path.
                    val entryName = File(entry.name).name
                    if (entryName == "base.apk") {
                        hasBaseApk = true
                    } else if ((entryName.startsWith("split_") ||
                                entryName.startsWith("config")) &&
                        entryName.endsWith(".apk")
                    ) {
                        hasSplitApk = true
                    }
                    // Optimization: exit early if both conditions are met.
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
            // If the file is not a valid ZIP archive (e.g., PDF, XLS), catch the exception.
            Timber.w("File is not a valid zip archive: ${fileEntity.path}. It's an unsupported file type.")
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
}