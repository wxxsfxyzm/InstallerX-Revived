package com.rosan.installer.data.app.model.impl.analyser

import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.repo.AnalyserRepo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import timber.log.Timber
import java.io.File
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

object ApksAnalyserRepoImpl : AnalyserRepo {
    override suspend fun doWork(
        config: ConfigEntity, data: List<DataEntity>, extra: AnalyseExtraEntity
    ): List<AppEntity> {
        val apps = mutableListOf<AppEntity>()
        data.forEach {
            // Due to the cache-first strategy, we can assume 'it' is always a FileEntity.
            apps.addAll(doFileWork(config, it as DataEntity.FileEntity, extra))
        }
        return apps
    }

    /**
     * Optimized core logic: only fully analyzes base.apk and creates lightweight entities for splits.
     * This is now the only entry point for this analyser.
     */
    private suspend fun doFileWork(
        config: ConfigEntity, data: DataEntity.FileEntity, extra: AnalyseExtraEntity
    ): List<AppEntity> {
        val apps = mutableListOf<AppEntity>()
        ZipFile(data.path).use { zipFile ->
            // Find and prioritize base.apk.
            val baseEntry = zipFile.getEntry("base.apk")
                ?: throw IllegalStateException("APKS file does not contain a base.apk")

            // Extract and analyze ONLY base.apk.
            var baseEntity = zipFile.getInputStream(baseEntry).use { inputStream ->
                // This helper still extracts the single base.apk to a temp file for ApkAnalyserRepoImpl.
                analyseSingleApkStream(
                    config,
                    DataEntity.ZipFileEntity(baseEntry.name, data),
                    inputStream,
                    extra
                )
            }.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
                ?: throw IllegalStateException("Failed to parse base.apk from APKS.")

            baseEntity = baseEntity.copy(containerType = extra.dataType)
            apps.add(baseEntity)

            // Iterate through all other entries and create lightweight entities for splits.
            val entries = zipFile.entries().toList()
            for (entry in entries) {
                val entryName = entry.name
                if (entryName == "base.apk" || !entryName.endsWith(".apk", ignoreCase = true)) {
                    continue // Skip base.apk and non-apk files.
                }

                // Create a lightweight SplitEntity without extracting or parsing the file.
                val splitEntity = AppEntity.SplitEntity(
                    packageName = baseEntity.packageName, // Reuse info from base
                    data = DataEntity.ZipFileEntity(entryName, data), // Point to the entry within the original zip
                    splitName = getSplitNameFromEntry(entry), // Parse split name from filename
                    targetSdk = baseEntity.targetSdk, // Reuse info from base
                    minSdk = baseEntity.minSdk, // Reuse info from base
                    arch = null,
                    containerType = extra.dataType
                )
                apps.add(splitEntity)
            }
        }
        return apps
    }

    /**
     * Gets the split name from the ZipEntry by stripping "split_" prefix and ".apk" suffix.
     * For example: from "split_config.arm64_v8a.apk" extracts "config.arm64_v8a"
     *
     * @param entry The ZipEntry representing the split APK file.
     * @return The extracted split name.
     */
    private fun getSplitNameFromEntry(entry: ZipEntry): String {
        return File(entry.name).nameWithoutExtension.removePrefix("split_")
    }

    /**
     * Helper to analyze a single APK stream by saving it to a temp file.
     * This is necessary for ApkAnalyserRepoImpl to work on a file path.
     * In the context of APKS, this is now ONLY used for base.apk.
     */
    private suspend fun analyseSingleApkStream(
        config: ConfigEntity, data: DataEntity, inputStream: InputStream, extra: AnalyseExtraEntity
    ): List<AppEntity> {
        val tempFile =
            File.createTempFile(UUID.randomUUID().toString(), ".apk", File(extra.cacheDirectory))

        return runCatching {
            tempFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }

            val tempData = DataEntity.FileEntity(tempFile.absolutePath).apply {
                source = data
            }

            // Call the downstream analyser.
            val originalEntities = ApkAnalyserRepoImpl.doWork(
                config,
                listOf(tempData),
                extra
            )

            // Enhance the result with the containerType.
            originalEntities.map { entity ->
                when (entity) {
                    is AppEntity.BaseEntity -> entity.copy(containerType = extra.dataType)
                    is AppEntity.SplitEntity -> entity.copy(containerType = extra.dataType)
                    else -> entity
                }
            }
        }.getOrElse { error ->
            Timber.tag("ApksAnalyser").e(error, "Failed to analyse single APK from stream: $data")
            tempFile.delete() // Clean up the failed temp file.
            emptyList()
        }
    }
}