package com.rosan.installer.data.app.model.impl

import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.model.entity.DataType
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

        val analysers = mapOf(
            DataType.APK to ApkAnalyserRepoImpl,
            DataType.APKS to ApksAnalyserRepoImpl,
            DataType.APKM to ApkMAnalyserRepoImpl,
            DataType.XAPK to XApkAnalyserRepoImpl,
            DataType.MULTI_APK_ZIP to MultiApkZipAnalyserRepoImpl
        )
        // 尝试为每个文件确定类型，无法识别的暂时记为 null
        val typedTasks = data.map { dataEntity ->
            val type = runCatching { getDataType(config, dataEntity) }.getOrNull()
            type to dataEntity // 创建一个 (DataType?, DataEntity) 的配对
        }
        // 筛选出所有可以被分析的文件
        val validTasks = typedTasks.map { (type, dataEntity) ->
            if (type != null) type to dataEntity else DataType.APK to dataEntity
        }
        // 如果没有任何一个文件是有效的，则抛出异常
        if (validTasks.isEmpty() && data.isNotEmpty()) {
            Timber.e("All ${data.size} files were unrecognized. Assume as APK")

            /*throw AnalyseFailedAllFilesUnsupportedException(
                "All ${data.size} file(s) were unrecognized. Please check the file formats."
            )*/
        }
        // --- NEW LOGIC: Determine if this is a Multi-APK installation session ---
        // A "Multi-APK Session" is defined as the app received more than one file,
        // and all of those files being identified as standard APKs.
        val isMultiApkSession = data.size > 1 && typedTasks.all { it.first == DataType.APK }
        val sessionType = if (isMultiApkSession) {
            Timber.d("This is a Multi-APK session. All resulting entities will be marked as such.")
            DataType.MULTI_APK
        } else {
            null // Not a special session, use the type of each file.
        }
        // 按类型分组有效的文件
        val tasksByType = validTasks.groupBy(
            keySelector = { it.first },
            valueTransform = { it.second }
        )
        // 遍历每种类型及其对应的文件列表
        for ((type, dataList) in tasksByType) {
            val analyser = analysers[type]
                ?: throw Exception("can't found analyser for this data type: '$type'")
            val containerTypeForAnalyser = sessionType ?: type
            // 调用子分析器时，通过 extra.copy() 将确定的类型传递下去
            val analysedEntities = analyser.doWork(
                config,
                dataList,
                extra.copy(dataType = containerTypeForAnalyser) // 将类型信息下发
            )
            rawApps.addAll(analysedEntities)
        }

        // 按packageName分组
        val groupedByPackage = rawApps.groupBy { it.packageName }
        val finalApps = mutableListOf<AppEntity>()

        // 遍历每个包，进行信息修正
        groupedByPackage.forEach { (_, entitiesInPackage) ->
            // 找到这个包里的BaseEntity，它是信息的来源
            val baseEntity =
                entitiesInPackage.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()

            if (baseEntity != null) {
                // 如果找到了BaseEntity，用它的targetSdk去修正所有其他实体
                val authoritativeTargetSdk = baseEntity.targetSdk
                entitiesInPackage.forEach { entity ->
                    val correctedEntity = when (entity) {
                        is AppEntity.SplitEntity -> entity.copy(targetSdk = authoritativeTargetSdk) // 修正SplitEntity
                        is AppEntity.DexMetadataEntity -> entity.copy(targetSdk = authoritativeTargetSdk) // 修正DexMetadataEntity
                        else -> entity // BaseEntity自身或其他类型保持不变
                    }
                    finalApps.add(correctedEntity)
                }
            } else {
                // 如果在这个分组里没有找到BaseEntity（例如只安装一个split.apk），则直接添加，不做修正
                finalApps.addAll(entitiesInPackage)
            }
        }
        return finalApps
        // TODO after supporting abi analyse, deduplicate the result
        // .deduplicate()
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

                Timber.d("Could not determine file type, returning null.")
                return@use null
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
                return@use null
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