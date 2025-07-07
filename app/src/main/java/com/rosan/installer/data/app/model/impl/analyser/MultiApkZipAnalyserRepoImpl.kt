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
     * 它将流内容写入临时文件，然后使用 ApkAnalyserRepoImpl 进行分析。
     */
    private suspend fun analyseApkStream(
        config: ConfigEntity,
        data: DataEntity,
        inputStream: InputStream,
        extra: AnalyseExtraEntity
    ): List<AppEntity> {
        // 在缓存目录中创建一个唯一的临时文件
        val tempFile =
            File.createTempFile(UUID.randomUUID().toString(), ".apk", File(extra.cacheDirectory))
        var analysisResult: List<AppEntity> = emptyList()
        try {
            // 将APK流写入临时文件
            tempFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            // 为临时文件创建一个新的数据实体
            val tempData = DataEntity.FileEntity(tempFile.absolutePath).apply {
                source = data
            }
            // 使用标准的APK分析器来解析这个临时文件
            val originalEntities = ApkAnalyserRepoImpl.doWork(config, listOf(tempData), extra)

            // --- 关键修改 ---
            // 遍历分析结果，并为每个实体创建一个唯一的包名
            analysisResult = originalEntities.mapNotNull { entity ->
                when (entity) {
                    is AppEntity.BaseEntity -> {
                        // 通过将原始包名和文件名结合，来创建一个唯一的包名
                        val uniquePackageName = "${entity.packageName}:${File(tempFile.name).name}"
                        // 返回一个带有唯一包名的新实体副本
                        entity.copy(packageName = uniquePackageName)
                    }
                    // 如果有其他类型的实体（如SplitEntity），我们在此场景下将其忽略，
                    // 因为我们只关心独立的APK。
                    else -> null
                }
            }
        } finally {
            // 确保临时文件在使用后被删除
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
        return analysisResult
    }
}
