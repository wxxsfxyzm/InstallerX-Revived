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
            apps.addAll(doFileWorkOptimized(config, it as DataEntity.FileEntity, extra))
        }
        return apps
    }

    /*private suspend fun doWork(
        config: ConfigEntity, data: DataEntity, extra: AnalyseExtraEntity
    ): List<AppEntity> {
        // 主要优化 doFileWork，因为其他 doWork 最终会依赖 InputStream，难以在不完全读取的情况下定位 base.apk
        return when (data) {
            is DataEntity.FileEntity -> doFileWorkOptimized(config, data, extra)
            is DataEntity.FileDescriptorEntity -> doFileDescriptorWork(config, data, extra)
            is DataEntity.ZipFileEntity -> doZipFileWork(config, data, extra)
            is DataEntity.ZipInputStreamEntity -> doZipInputStreamWork(config, data, extra)
        }
    }*/

    /**
     * Optimized core logic: only fully analyzes base.apk and creates lightweight entities for splits.
     * This is now the only entry point for this analyser.
     */
    private suspend fun doFileWorkOptimized(
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

                // 4. Create a lightweight SplitEntity without extracting or parsing the file.
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

    从 ZipEntry 的名称中解析出 split name。

    例如：从 "split_config.arm64_v8a.apk" 解析出 "config.arm64_v8a"
     */
    private fun getSplitNameFromEntry(entry: ZipEntry): String {
        return File(entry.name).nameWithoutExtension.removePrefix("split_")
    }

    /*    private suspend fun doFileWork(
            config: ConfigEntity, data: DataEntity.FileEntity, extra: AnalyseExtraEntity
        ): List<AppEntity> {
            val apps = mutableListOf<AppEntity>()
            ZipFile(data.path).use {
                val entries = it.entries().toList()
                for (entry in entries) {
                    if (File(entry.name).extension.toLowerCase(Locale.current) != "apk") continue
                    it.getInputStream(entry).use {
                        apps.addAll(
                            doApkInInputStreamWork(
                                config,
                                DataEntity.ZipFileEntity(entry.name, data),
                                it,
                                extra
                            )
                        )
                    }
                }
            }
            return apps
        }*/

    /*    private suspend fun doFileDescriptorWork(
            config: ConfigEntity, data: DataEntity.FileDescriptorEntity, extra: AnalyseExtraEntity
        ): List<AppEntity> =
            doZipInputStreamWork(config, data, ZipInputStream(data.getInputStream()), extra)

        private suspend fun doZipFileWork(
            config: ConfigEntity, data: DataEntity.ZipFileEntity, extra: AnalyseExtraEntity
        ): List<AppEntity> =
            doZipInputStreamWork(config, data, ZipInputStream(data.getInputStream()), extra)

        private suspend fun doZipInputStreamWork(
            config: ConfigEntity, data: DataEntity.ZipInputStreamEntity, extra: AnalyseExtraEntity
        ): List<AppEntity> =
            doZipInputStreamWork(config, data, ZipInputStream(data.getInputStream()), extra)

        private suspend fun doZipInputStreamWork(
            config: ConfigEntity, data: DataEntity, zip: ZipInputStream, extra: AnalyseExtraEntity
        ): List<AppEntity> {
            val apps = mutableListOf<AppEntity>()
            zip.use {
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (File(entry.name).extension.toLowerCase(Locale.current) != "apk") continue
                    apps.addAll(
                        doApkInInputStreamWork(
                            config,
                            DataEntity.ZipInputStreamEntity(entry.name, data),
                            it,
                            extra
                        )
                    )
                }
            }
            return apps
        }

        private suspend fun doApkInInputStreamWork(
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

                // 调用下游分析器
                val originalEntities = ApkAnalyserRepoImpl.doWork(
                    config,
                    listOf(tempData),
                    extra
                )

                // 对返回结果进行增强，附加 containerType
                originalEntities.map { entity ->
                    when (entity) {
                        // 确保对所有可能的 AppEntity 子类都进行增强
                        is AppEntity.BaseEntity -> entity.copy(containerType = extra.dataType)
                        is AppEntity.SplitEntity -> entity.copy(containerType = extra.dataType)
                        // 如果未来有更多子类，也需要在这里添加
                        else -> entity
                    }
                }
            }.getOrElse { error ->
                // 记录错误日志
                Timber.tag("ApksAnalyser").e(error, "Failed to analyse APK from stream")

                // 清理失败时产生的临时文件
                // 使用 delete() 即可，因为 tempFile 是一个文件
                tempFile.delete()

                // 返回一个空列表，表示分析失败
                emptyList()
            }
        }*/
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