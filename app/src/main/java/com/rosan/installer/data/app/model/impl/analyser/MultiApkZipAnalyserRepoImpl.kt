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
        // 使用 flatMap 可以优雅地将多个ZIP包的分析结果合并到一个列表中
        return data.flatMap { analyseSingleZip(config, it, extra) }
    }

    /**
     * 分析单个 ZIP 文件实体。
     */
    private suspend fun analyseSingleZip(
        config: ConfigEntity,
        zipDataEntity: DataEntity,
        extra: AnalyseExtraEntity
    ): List<AppEntity> {
        val results = mutableListOf<AppEntity>()
        val inputStream = zipDataEntity.getInputStreamWhileNotEmpty() ?: return emptyList()

        // 使用 ZipInputStream 流式读取，避免将整个ZIP解压到内存
        ZipInputStream(inputStream).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) {
                    zip.closeEntry()
                    continue
                }

                // 核心判断：只处理以 .apk 结尾的文件（不区分大小写）
                if (entry.name.endsWith(".apk", ignoreCase = true)) {
                    Timber.tag("MultiApkZipAnalyser")
                        .d("Found APK entry: ${entry.name} in ${zipDataEntity}")

                    // 为ZIP内的这个APK条目创建一个临时的DataEntity
                    val entryDataEntity = DataEntity.ZipInputStreamEntity(entry.name, zipDataEntity)

                    // 对这个APK流进行分析
                    val analysedApks = analyseApkStream(config, entryDataEntity, zip, extra)
                    results.addAll(analysedApks)
                }
                // 非APK文件会被自动忽略

                zip.closeEntry()
            }
        }
        return results
    }

    /**
     * 将单个APK流提取到临时文件并调用 ApkAnalyserRepoImpl 进行分析。
     */
    private suspend fun analyseApkStream(
        config: ConfigEntity,
        data: DataEntity.ZipInputStreamEntity, // 明确传入源，用于追溯和命名
        inputStream: InputStream, // 这是未关闭的 ZipInputStream
        extra: AnalyseExtraEntity
    ): List<AppEntity> {
        // 1. 创建一个临时文件来存放提取出来的APK
        val tempFile =
            File.createTempFile(UUID.randomUUID().toString(), ".apk", File(extra.cacheDirectory))

        // 2. 将APK内容从ZIP流复制到临时文件
        // runCatching 保证即使某个APK分析失败，也不会中断整个流程
        val result = runCatching {
            tempFile.outputStream().use { output ->
                // 注意：这里不能关闭 inputStream (zip)，因为它由外层循环控制
                inputStream.copyTo(output)
            }

            // 3. 创建指向这个临时文件的 DataEntity
            val tempData = DataEntity.FileEntity(tempFile.absolutePath).apply {
                source = data // 保持数据来源的链条
            }

            // 4. 【核心复用】调用现有的 `ApkAnalyserRepoImpl` 来分析这个独立的APK文件
            val originalEntities = ApkAnalyserRepoImpl.doWork(config, listOf(tempData), extra)

            // 5. 【数据增强】
            //    - 附加 containerType
            //    - 使用ZIP内的文件名作为更友好的显示标签(label)
            val displayNameFromZip = File(data.name).nameWithoutExtension

            originalEntities.map { entity ->
                if (entity is AppEntity.BaseEntity) {
                    entity.copy(
                        label = displayNameFromZip,         // 使用ZIP包内的文件名作为应用名
                        containerType = extra.dataType    // 附加容器类型 (MULTI_APK_ZIP)
                    )
                } else {
                    entity // 对于非BaseEntity，暂时不处理 (如果有SplitEntity等，也需要copy)
                }
            }
        }

        // 6. 处理结果
        result.onSuccess { entities ->
            Timber.tag("MultiApkZipAnalyser").d("Successfully analysed ${data.name}")
            // 【重要】！！！
            // 分析成功后，**不要删除** tempFile。
            // 因为返回的 AppEntity.BaseEntity.data 指向了这个临时文件，
            // 后续的安装过程需要它。临时文件应该在整个安装会话结束后统一清理。
            return entities
        }

        result.onFailure { error ->
            Timber.tag("MultiApkZipAnalyser").e(error, "Failed to analyse ${data.name}")
            tempFile.delete() // 如果分析过程中出错，则删除这个无效的临时文件
        }

        return emptyList()
    }
}