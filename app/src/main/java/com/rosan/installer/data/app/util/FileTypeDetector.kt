package com.rosan.installer.data.app.util

import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.model.enums.DataType
import timber.log.Timber
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

object FileTypeDetector {

    fun detect(data: DataEntity, extra: AnalyseExtraEntity): DataType {
        val fileEntity = data as? DataEntity.FileEntity ?: return DataType.NONE

        return try {
            ZipFile(fileEntity.path).use { zip ->
                // 将 entries 转为 List，因为后续可能需要多次遍历（虽然性能有轻微损耗，但逻辑更清晰且安全）
                val entries = zip.entries().asSequence().toList()

                // 优先级 1: 模块类型 (Module Types) - 仅当开关开启时检测
                if (extra.isModuleFlashEnabled) {
                    detectModuleType(zip, entries)?.let { return it }
                }

                // 优先级 2: 标准应用格式 (Standard App Types)
                detectStandardType(zip, entries)
            }
        } catch (e: Exception) {
            // 如果无法作为 Zip 打开，且路径以 .apk 结尾，则兜底认为是 APK
            if (fileEntity.path.endsWith(".apk", true)) {
                DataType.APK
            } else {
                DataType.NONE
            }
        }
    }

    private fun detectModuleType(zipFile: ZipFile, entries: List<ZipEntry>): DataType? {
        // 核心特征：存在 module.prop
        val hasModuleProp = entries.any {
            it.name == "module.prop" || it.name == "common/module.prop"
        }

        if (!hasModuleProp) return null

        // 进一步细分模块类型
        val hasAndroidManifest = zipFile.getEntry("AndroidManifest.xml") != null
        val hasApksInside = entries.any {
            !it.isDirectory && it.name.endsWith(".apk", ignoreCase = true)
        }

        return when {
            // 既是模块又是 APK
            hasAndroidManifest -> {
                Timber.d("Detected MIXED_MODULE_APK")
                DataType.MIXED_MODULE_APK
            }
            // 包含 APK 的模块包
            hasApksInside -> {
                Timber.d("Detected MIXED_MODULE_ZIP")
                DataType.MIXED_MODULE_ZIP
            }
            // 纯模块
            else -> {
                Timber.d("Detected MODULE_ZIP")
                DataType.MODULE_ZIP
            }
        }
    }

    private fun detectStandardType(zipFile: ZipFile, entries: List<ZipEntry>): DataType {
        // 1. XAPK (manifest.json)
        if (zipFile.getEntry("manifest.json") != null) return DataType.XAPK

        // 2. APKM (info.json) - 可增加对 json 内容的简单校验
        if (zipFile.getEntry("info.json") != null) return DataType.APKM

        // 3. 标准 APK (AndroidManifest.xml)
        if (zipFile.getEntry("AndroidManifest.xml") != null) return DataType.APK

        // 4. APKS (Split APKs)
        // 特征：包含 base.apk 或 base-master.apk
        val hasBaseApk = entries.any {
            val name = File(it.name).name
            name.equals("base.apk", true) || name.startsWith("base-master")
        }
        if (hasBaseApk) return DataType.APKS

        // 5. Multi-APK Zip (包含任意 APK 的压缩包)
        val hasApkFiles = entries.any {
            !it.isDirectory && it.name.endsWith(".apk", ignoreCase = true)
        }
        if (hasApkFiles) return DataType.MULTI_APK_ZIP

        return DataType.NONE
    }
}