package com.rosan.installer.data.app.model.impl

import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.model.impl.analyser.ApkAnalyserRepoImpl
import com.rosan.installer.data.app.model.impl.analyser.ApkMAnalyserRepoImpl
import com.rosan.installer.data.app.model.impl.analyser.ApksAnalyserRepoImpl
import com.rosan.installer.data.app.model.impl.analyser.XApkAnalyserRepoImpl
import com.rosan.installer.data.app.repo.AnalyserRepo
import com.rosan.installer.data.app.util.DataType
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
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
        val apps = mutableListOf<AppEntity>()

        val analysers = mapOf(
            DataType.APK to ApkAnalyserRepoImpl,
            DataType.APKS to ApksAnalyserRepoImpl,
            DataType.APKM to ApkMAnalyserRepoImpl,
            DataType.XAPK to XApkAnalyserRepoImpl
        )
        val tasks = mutableMapOf<AnalyserRepo, MutableList<DataEntity>>()
        data.forEach {
            val type = kotlin.runCatching { getDataType(config, it) ?: DataType.APK }
                .getOrDefault(DataType.APK)
            val analyser =
                analysers[type] ?: throw Exception("can't found analyser for this data: '$data'")
            val value = tasks[analyser] ?: mutableListOf()
            value.add(it)
            tasks[analyser] = value
        }
        for ((key, value) in tasks) {
            apps.addAll(key.doWork(config, value, extra))
        }
        return apps
    }

    private fun getDataType(config: ConfigEntity, data: DataEntity): DataType? =
        when (data) {
            is DataEntity.FileEntity -> ZipFile(data.path).use { zipFile ->
                when {
                    zipFile.getEntry("AndroidManifest.xml") != null -> DataType.APK
                    zipFile.getEntry("info.json") != null -> if (isGenuineApkmInfo(
                            zipFile,
                            zipFile.getEntry("info.json")
                        )
                    )
                        DataType.APKM
                    else
                        DataType.APKS

                    zipFile.getEntry("manifest.json") != null -> DataType.XAPK
                    else -> {
                        val entries = zipFile.entries().toList()
                        var containsApk = false
                        for (entry in entries) {
                            if (File(entry.name).extension.toLowerCase(Locale.current) != "apk") continue
                            containsApk = true
                            break
                        }
                        if (containsApk) DataType.APKS else null
                    }
                }
            }

            else -> ZipInputStream(data.getInputStream()).use { zip ->
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
                if (type == null && containsApk) type = DataType.APKS
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