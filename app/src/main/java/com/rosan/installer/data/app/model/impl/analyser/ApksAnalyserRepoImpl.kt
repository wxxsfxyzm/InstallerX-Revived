package com.rosan.installer.data.app.model.impl.analyser

import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.repo.FileAnalyserRepo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import timber.log.Timber
import java.io.File
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

object ApksAnalyserRepoImpl : FileAnalyserRepo {
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
            // Single iteration and categorization using partition.
            val (baseEntries, splitEntries) = zipFile.entries().asSequence()
                .filterNot { it.isDirectory } // Filter out directories
                .filter { it.name.endsWith(".apk", ignoreCase = true) }
                .filterNot { File(it.name).name.matches(Regex("base-master_\\d+\\.apk")) }
                .partition { entry ->
                    // The predicate to identify base APKs.
                    val entryName = File(entry.name).name
                    entryName == "base.apk" || entryName == "base-master.apk"
                }

            // Determine the primary base APK.
            val baseEntry = baseEntries.firstOrNull()
                ?: throw IllegalStateException("APKS file does not contain a recognizable base APK.")

            // Parse the primary base APK to get its info.
            val baseEntity = zipFile.getInputStream(baseEntry).use { inputStream ->
                analyseSingleApkStream(
                    config,
                    DataEntity.ZipFileEntity(baseEntry.name, data),
                    inputStream,
                    extra
                )
            }.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
                ?: throw IllegalStateException("Failed to parse base APK '${baseEntry.name}' from APKS.")

            val finalBaseEntity = baseEntity.copy(containerType = extra.dataType)

            // Create all SplitEntity objects in a single pass over the consolidated list.
            val finalSplitEntities = splitEntries
                .filterNot { File(it.name).name.matches(Regex("base-.*_2\\.apk")) }
                .map { entry ->
                    AppEntity.SplitEntity(
                        packageName = finalBaseEntity.packageName,
                        data = DataEntity.ZipFileEntity(entry.name, data),
                        splitName = getSplitNameFromEntry(entry),
                        targetSdk = finalBaseEntity.targetSdk,
                        minSdk = finalBaseEntity.minSdk,
                        arch = null,
                        containerType = extra.dataType
                    )
                }

            // Return the final combined list.
            return listOf(finalBaseEntity) + finalSplitEntities
        }
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