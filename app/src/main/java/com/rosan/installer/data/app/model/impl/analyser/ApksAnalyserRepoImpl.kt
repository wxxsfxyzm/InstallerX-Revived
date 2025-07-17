package com.rosan.installer.data.app.model.impl.analyser

import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
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
import java.util.zip.ZipInputStream

object ApksAnalyserRepoImpl : AnalyserRepo {
    override suspend fun doWork(
        config: ConfigEntity, data: List<DataEntity>, extra: AnalyseExtraEntity
    ): List<AppEntity> {
        val apps = mutableListOf<AppEntity>()
        data.forEach { apps.addAll(doWork(config, it, extra)) }
        return apps
    }

    private suspend fun doWork(
        config: ConfigEntity, data: DataEntity, extra: AnalyseExtraEntity
    ): List<AppEntity> {
        // 主要优化 doFileWork，因为其他 doWork 最终会依赖 InputStream，难以在不完全读取的情况下定位 base.apk
        return when (data) {
            is DataEntity.FileEntity -> doFileWorkOptimized(config, data, extra)
            is DataEntity.FileDescriptorEntity -> doFileDescriptorWork(config, data, extra)
            is DataEntity.ZipFileEntity -> doZipFileWork(config, data, extra)
            is DataEntity.ZipInputStreamEntity -> doZipInputStreamWork(config, data, extra)
        }
    }

    /**

    优化后的核心逻辑：只分析 base.apk，轻量化处理 split.apk
     */
    private suspend fun doFileWorkOptimized(
        config: ConfigEntity, data: DataEntity.FileEntity, extra: AnalyseExtraEntity
    ): List<AppEntity> {
        val apps = mutableListOf<AppEntity>()
        ZipFile(data.path).use { zipFile ->
            // 1. 找到并优先处理 base.apk
            val baseEntry = zipFile.getEntry("base.apk")
                ?: throw IllegalStateException("APKS file does not contain a base.apk")

            // 2. 仅提取和分析 base.apk
            var baseEntity = zipFile.getInputStream(baseEntry).use { inputStream ->
                // 使用现有逻辑提取并分析单个APK
                doApkInInputStreamWork(
                    config,
                    DataEntity.ZipFileEntity(baseEntry.name, data),
                    inputStream,
                    extra
                )
            }.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
                ?: throw IllegalStateException("Failed to parse base.apk")

            baseEntity = baseEntity.copy(containerType = extra.dataType)
            apps.add(baseEntity)

            // 3. 遍历所有条目，为 split.apk 创建轻量级实体
            val entries = zipFile.entries().toList()
            for (entry in entries) {
                val entryName = entry.name
                if (entryName == "base.apk" || File(entryName).extension.toLowerCase(Locale.current) != "apk") {
                    continue // 跳过 base.apk 和非 apk 文件
                }

                // 4. 创建轻量级 SplitEntity，不进行文件提取和解析
                val splitEntity = AppEntity.SplitEntity(
                    packageName = baseEntity.packageName, // 复用 base 的信息
                    data = DataEntity.ZipFileEntity(entryName, data), // 指向原始zip中的条目
                    splitName = getSplitNameFromEntry(entry), // 从文件名解析 split 名称
                    targetSdk = baseEntity.targetSdk, // 复用 base 的信息
                    minSdk = baseEntity.minSdk, // 复用 base 的信息
                    arch = null,
                    containerType = extra.dataType // 直接从 extra 中获取并赋值
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

    private suspend fun doFileDescriptorWork(
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
    }
}