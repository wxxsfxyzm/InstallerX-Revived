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
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

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

    private fun getDataType(config: ConfigEntity, data: DataEntity): DataType? =
        when (data) {
            is DataEntity.FileEntity -> ZipFile(data.path).use { zipFile ->
                Timber.d("data is zipFile")
                // 优先判断标准的清单文件
                when {
                    // *** 关键修正：检查根目录下的文件 ***
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

                // 如果没有清单文件，则进行更严格的APKS格式检查
                var hasBaseApk = false
                var hasSplitApk = false
                val entries = zipFile.entries().toList()
                for (entry in entries) {
                    if (entry.isDirectory) continue
                    // *** 关键修正：只检查文件名，不关心路径 ***
                    val entryName = File(entry.name).name
                    if (entryName == "base.apk") {
                        hasBaseApk = true
                    } else if ((entryName.startsWith("split_") ||
                                entryName.startsWith("config")) &&
                        entryName.endsWith(".apk")
                    ) {
                        hasSplitApk = true
                    }
                    // 优化：如果两个条件都满足，可以提前退出循环
                    if (hasBaseApk && hasSplitApk) break
                }

                if (hasBaseApk && hasSplitApk) {
                    Timber.d("Detected APKS structure.")
                    return@use DataType.APKS
                }

                // 如果以上都不是，最后检查是否为包含任意APK的通用ZIP包
                // *** 关键修正：检查任意路径下的apk文件 ***
                if (entries.any { !it.isDirectory && it.name.endsWith(".apk", ignoreCase = true) }) {
                    Timber.d("Detected as a generic ZIP with APKs.")
                    return@use DataType.MULTI_APK_ZIP
                }

                Timber.d("Could not determine file type, returning NONE.")
                return@use DataType.NONE
            }

            else -> ZipInputStream(data.getInputStream()).use { zip ->
                Timber.d("data is ZipInputStream")

                // --- 重构后的逻辑 ---
                var hasBaseApk = false
                var hasSplitApk = false
                var containsAnyApk = false
                var manifestType: DataType? = null

                while (true) {
                    val entry = zip.nextEntry ?: break
                    Timber.d("Processing zip entry: ${entry.name}")
                    if (entry.isDirectory) {
                        zip.closeEntry()
                        continue
                    }

                    // *** 关键修正 1: 严格检查根目录的清单文件 ***
                    // entry.name 是包含完整路径的
                    when (entry.name) {
                        "AndroidManifest.xml" -> {
                            Timber.d("Found AndroidManifest.xml at root, it's an APK.")
                            return@use DataType.APK // 最高优先级，直接返回
                        }

                        "info.json" -> {
                            // 发现 info.json，需要读取内容来判断是APKM还是APKS
                            val content = zip.bufferedReader().use(BufferedReader::readText)
                            val jsonElement = Json.parseToJsonElement(content)
                            manifestType = if (jsonElement is JsonObject && jsonElement.containsKey("apkm_version")) {
                                DataType.APKM
                            } else {
                                DataType.APKS
                            }
                            Timber
                                .d("Found info.json at root, determined type: $manifestType. Exiting early.")
                            return@use manifestType // 第二优先级，直接返回
                        }

                        "manifest.json" -> {
                            // 发现 manifest.json，暂定为XAPK，但优先级低于info.json
                            if (manifestType == null) {
                                manifestType = DataType.XAPK
                            }
                        }
                    }

                    // *** 关键修正 2: 仅在需要时收集APK文件信息，用于回退判断 ***
                    if (entry.name.endsWith(".apk", ignoreCase = true)) {
                        containsAnyApk = true
                        val entryName = File(entry.name).name
                        if (entryName == "base.apk") {
                            hasBaseApk = true
                        } else if (entryName.startsWith("split_") ||
                            entryName.startsWith("config")
                        ) {
                            hasSplitApk = true
                        }
                    }

                    zip.closeEntry()

                    // 如果已经通过低优先级的 manifest.json 确定了类型，并且继续扫描也没有发现更高优先级的，
                    // 那么在循环结束后来判断。这里不提前break，以防后面有更高优先级的清单文件。
                }

                // --- 循环结束后，根据收集到的信息进行最终判断 ---
                // 如果在循环中已经通过 manifest.json 推断出类型
                if (manifestType != null) {
                    Timber.d("Finished scan. Returning type from manifest: $manifestType")
                    return@use manifestType
                }

                // 如果没有清单文件，则根据文件结构判断
                if (hasBaseApk && hasSplitApk) {
                    Timber.d("Finished scan. Detected APKS structure.")
                    return@use DataType.APKS
                }

                // 最后，如果只包含普通的apk文件
                if (containsAnyApk) {
                    Timber.d("Finished scan. Detected as a generic ZIP with APKs.")
                    return@use DataType.MULTI_APK_ZIP
                }

                Timber.d("Finished scan. Could not determine file type.")
                return@use DataType.NONE
            }
        }

    /**
     * 辅助函数：使用 kotlinx.serialization 检查 zip 条目是否为真正的 APKM 的 info.json。
     *
     * @author wxxsfxyzm
     * @param zipFile ZipFile 对象
     * @param entry 要检查的 ZipEntry (info.json)
     * @return 如果是真正的 APKM info.json，返回 true
     */
    private fun isGenuineApkmInfo(zipFile: ZipFile, entry: ZipEntry): Boolean {
        return try {
            zipFile.getInputStream(entry).use { inputStream ->
                val content = inputStream.bufferedReader().use(BufferedReader::readText)

                // 1. 使用 kotlinx.serialization 将字符串解析为通用的 JsonElement
                val jsonElement = Json.parseToJsonElement(content)

                // 2. 检查它是否为一个 JsonObject，并且是否包含 "apkm_version" 键
                jsonElement is JsonObject && jsonElement.containsKey("apkm_version")
            }
        } catch (e: Exception) {
            // 捕获所有可能的异常，包括 IO 异常和序列化异常 (SerializationException)
            // e.printStackTrace() // 在调试时可以打开此行
            false
        }
    }
}