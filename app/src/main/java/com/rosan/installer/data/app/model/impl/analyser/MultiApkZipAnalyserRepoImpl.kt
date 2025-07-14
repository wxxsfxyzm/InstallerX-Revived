package com.rosan.installer.data.app.model.impl.analyser

import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.repo.AnalyserRepo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import java.io.File
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

/**
 * 分析包含多个独立APK的压缩包 (.zip)。
 * 它会遍历压缩包，对内部的每个 .apk 文件进行独立的分析以获取版本号等信息。
 */
object MultiApkZipAnalyserRepoImpl : AnalyserRepo {
    override suspend fun doWork(
        config: ConfigEntity,
        data: List<DataEntity>,
        extra: AnalyseExtraEntity
    ): List<AppEntity> {
        val apps = mutableListOf<AppEntity>()
        data.forEach { dataEntity ->
            apps.addAll(analyseSingleZip(config, dataEntity, extra))
        }
        return apps
    }

    private suspend fun analyseSingleZip(
        config: ConfigEntity,
        dataEntity: DataEntity,
        extra: AnalyseExtraEntity
    ): List<AppEntity> {
        return when (dataEntity) {
            is DataEntity.FileEntity -> analyseFile(config, dataEntity, extra)
            else -> analyseStream(config, dataEntity, extra)
        }
    }

    private suspend fun analyseFile(
        config: ConfigEntity,
        data: DataEntity.FileEntity,
        extra: AnalyseExtraEntity
    ): List<AppEntity> {
        val apps = mutableListOf<AppEntity>()
        ZipFile(data.path).use { zipFile ->
            val entries = zipFile.entries().toList()
            for (entry in entries) {
                if (entry.isDirectory || !entry.name.endsWith(".apk", ignoreCase = true)) continue
                zipFile.getInputStream(entry).use { inputStream ->
                    apps.addAll(
                        analyseApkStream(
                            config,
                            DataEntity.ZipFileEntity(entry.name, data),
                            inputStream,
                            extra
                        )
                    )
                }
            }
        }
        return apps
    }

    private suspend fun analyseStream(
        config: ConfigEntity,
        data: DataEntity,
        extra: AnalyseExtraEntity
    ): List<AppEntity> {
        val apps = mutableListOf<AppEntity>()
        ZipInputStream(data.getInputStream()).use { zipInputStream ->
            while (true) {
                val entry = zipInputStream.nextEntry ?: break
                if (entry.isDirectory || !entry.name.endsWith(".apk", ignoreCase = true)) continue
                apps.addAll(
                    analyseApkStream(
                        config,
                        DataEntity.ZipInputStreamEntity(entry.name, data),
                        zipInputStream,
                        extra
                    )
                )
                zipInputStream.closeEntry()
            }
        }
        return apps
    }

    /**
     * 从输入流中分析单个APK文件。
     * 根据最终分析器 ApkAnalyserRepoImpl 的要求，此函数必须执行以下操作：
     * 1. 将 InputStream 写入一个临时的、可访问的文件中。
     * 2. 调用下游分析器处理该文件。
     * 3. 使用健壮的错误处理机制，并在失败时清理临时文件。
     */
    private suspend fun analyseApkStream(
        config: ConfigEntity,
        data: DataEntity,
        inputStream: InputStream,
        extra: AnalyseExtraEntity
    ): List<AppEntity> {
        val tempFile =
            File.createTempFile(UUID.randomUUID().toString(), ".apk", File(extra.cacheDirectory))

        val result = runCatching {
            // 核心逻辑: 将流写入文件，然后进行分析
            tempFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }

            val tempData = DataEntity.FileEntity(tempFile.absolutePath).apply {
                source = data
            }

            // 调用必须使用文件路径的下游分析器
            val originalEntities = ApkAnalyserRepoImpl.doWork(config, listOf(tempData), extra)

            // 根据你的建议，优化UI显示名称
            val displayNameFromZip = when (data) {
                is DataEntity.ZipFileEntity -> File(data.name).nameWithoutExtension
                is DataEntity.ZipInputStreamEntity -> File(data.name).nameWithoutExtension
                else -> tempFile.nameWithoutExtension
            }

            originalEntities.map { entity ->
                when (entity) {
                    is AppEntity.BaseEntity -> entity.copy(label = displayNameFromZip)
                    else -> entity
                }
            }
        }

        // 根据分析结果进行资源管理
        result.onSuccess { entities ->
            // 成功：返回结果，保留临时文件用于安装
            return entities
        }

        result.onFailure { error ->
            // 失败：删除临时文件，防止垃圾残留。可以添加日志记录。
            tempFile.delete()
            // Log.e("MultiApkZipAnalyser", "Failed to analyse APK stream from ${data.name}", error)
        }

        // 默认返回空列表
        return emptyList()
    }
}
