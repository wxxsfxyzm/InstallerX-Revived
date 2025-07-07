package com.rosan.installer.data.app.model.impl

import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.model.impl.analyser.ApkAnalyserRepoImpl
import com.rosan.installer.data.app.model.impl.analyser.ApkMAnalyserRepoImpl
import com.rosan.installer.data.app.model.impl.analyser.ApksAnalyserRepoImpl
import com.rosan.installer.data.app.model.impl.analyser.MultiApkZipAnalyserRepoImpl
import com.rosan.installer.data.app.model.impl.analyser.XApkAnalyserRepoImpl
import com.rosan.installer.data.app.repo.AnalyserRepo
import com.rosan.installer.data.app.util.DataType
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
        // 1. 先收集所有原始分析结果
        val rawApps = mutableListOf<AppEntity>()

        val analysers = mapOf(
            DataType.APK to ApkAnalyserRepoImpl,
            DataType.APKS to ApksAnalyserRepoImpl,
            DataType.APKM to ApkMAnalyserRepoImpl,
            DataType.XAPK to XApkAnalyserRepoImpl,
            DataType.MULTI_APK_ZIP to MultiApkZipAnalyserRepoImpl
        )
        val tasks = mutableMapOf<AnalyserRepo, MutableList<DataEntity>>()
        data.forEach {
            val type = kotlin.runCatching { getDataType(config, it) ?: DataType.APK }
                .getOrDefault(DataType.APK)
            Timber.tag("AnalyserRepoImpl").d(
                "getDataType: $type"
            )
            val analyser =
                analysers[type] ?: throw Exception("can't found analyser for this data: '$data'")
            val value = tasks[analyser] ?: mutableListOf()
            value.add(it)
            tasks[analyser] = value
        }
        for ((key, value) in tasks) {
            rawApps.addAll(key.doWork(config, value, extra))
        }

        // 2. 按packageName分组
        val groupedByPackage = rawApps.groupBy { it.packageName }
        val finalApps = mutableListOf<AppEntity>()

        // 3. 遍历每个包，进行信息修正
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
    }

    private fun getDataType(config: ConfigEntity, data: DataEntity): DataType? =
        when (data) {
            is DataEntity.FileEntity -> ZipFile(data.path).use { zipFile ->
                // 优先判断标准的清单文件
                when {
                    zipFile.getEntry("AndroidManifest.xml") != null -> return@use DataType.APK
                    zipFile.getEntry("info.json") != null -> {
                        val isApkm = isGenuineApkmInfo(zipFile, zipFile.getEntry("info.json")!!)
                        return@use if (isApkm) DataType.APKM else DataType.APKS
                    }

                    zipFile.getEntry("manifest.json") != null -> return@use DataType.XAPK
                }

                // 如果没有清单文件，则进行更严格的APKS格式检查
                var hasBaseApk = false
                var hasSplitApk = false
                val entries = zipFile.entries().toList()
                for (entry in entries) {
                    if (entry.isDirectory) continue
                    val entryName = File(entry.name).name
                    if (entryName == "base.apk") {
                        hasBaseApk = true
                    } else if (entryName.startsWith("split_") && entryName.endsWith(".apk")) {
                        hasSplitApk = true
                    }
                    // 优化：如果两个条件都满足，可以提前退出循环
                    if (hasBaseApk && hasSplitApk) break
                }

                if (hasBaseApk && hasSplitApk) {
                    return@use DataType.APKS
                }

                // 如果以上都不是，最后检查是否为包含任意APK的通用ZIP包
                if (entries.any {
                        !it.isDirectory && it.name.endsWith(
                            ".apk",
                            ignoreCase = true
                        )
                    }) {
                    return@use DataType.MULTI_APK_ZIP
                }

                return@use null
            }

            else -> ZipInputStream(data.getInputStream()).use { zip ->
                Timber.tag("AnalyserRepoImpl").d("data is ZipInputStream")
                var type: DataType? = null
                var containsApk = false
                while (true) {
                    val entry = zip.nextEntry ?: break
                    type = when (entry.name) {
                        "AndroidManifest.xml" -> DataType.APK
                        "info.json" -> {
                            val content = zip.bufferedReader().use(BufferedReader::readText)
                            val jsonElement = Json.parseToJsonElement(content)
                            if (jsonElement is JsonObject && jsonElement.containsKey("apkm_version"))
                                DataType.APKM
                            else
                                DataType.APKS

                        }

                        "manifest.json" -> DataType.XAPK
                        else -> null
                    }
                    if (File(entry.name).extension.toLowerCase(Locale.current) == "apk") containsApk =
                        true
                    zip.closeEntry()
                    if (type != null) break
                }
                if (type == null && containsApk) type = DataType.MULTI_APK_ZIP
                return@use type
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