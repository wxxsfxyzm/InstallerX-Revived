package com.rosan.installer.data.app.model.impl.analyser

import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.model.enums.DataType
import timber.log.Timber
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile

object FileTypeDetector {

    fun detect(data: DataEntity, extra: AnalyseExtraEntity): DataType {
        val fileEntity = data as? DataEntity.FileEntity ?: return DataType.NONE

        return try {
            ZipFile(fileEntity.path).use { zip ->
                // Convert entries to List because we might iterate multiple times
                val entries = zip.entries().asSequence().toList()

                // Priority 1: Module Types (only if enabled)
                if (extra.isModuleFlashEnabled) {
                    detectModuleType(zip, entries)?.let { return it }
                }

                // Priority 2: Standard App Types
                detectStandardType(zip, entries)
            }
        } catch (e: Exception) {
            // Fallback: assume APK if path ends with .apk and zip open failed
            if (fileEntity.path.endsWith(".apk", true)) {
                DataType.APK
            } else {
                Timber.Forest.e(e, "Failed to detect file type for path: ${fileEntity.path}")
                if (e is ZipException) throw e
                else DataType.NONE
            }
        }
    }

    private fun detectModuleType(zipFile: ZipFile, entries: List<ZipEntry>): DataType? {
        // Core feature: presence of module.prop
        val hasModuleProp = entries.any {
            it.name == "module.prop" || it.name == "common/module.prop"
        }

        if (!hasModuleProp) return null

        val hasAndroidManifest = zipFile.getEntry("AndroidManifest.xml") != null
        val hasApksInside = entries.any {
            !it.isDirectory && it.name.endsWith(".apk", ignoreCase = true)
        }

        return when {
            // Module and APK mixed
            hasAndroidManifest -> {
                Timber.Forest.d("Detected MIXED_MODULE_APK")
                DataType.MIXED_MODULE_APK
            }
            // Module containing APKs
            hasApksInside -> {
                Timber.Forest.d("Detected MIXED_MODULE_ZIP")
                DataType.MIXED_MODULE_ZIP
            }
            // Pure module
            else -> {
                Timber.Forest.d("Detected MODULE_ZIP")
                DataType.MODULE_ZIP
            }
        }
    }

    private fun detectStandardType(zipFile: ZipFile, entries: List<ZipEntry>): DataType {
        // 1. XAPK (manifest.json)
        if (zipFile.getEntry("manifest.json") != null) return DataType.XAPK

        // 2. APKM (info.json) - Validate content to distinguish from generic info.json
        val infoEntry = zipFile.getEntry("info.json")
        if (infoEntry != null) {
            try {
                // Read content to verify mandatory APKM fields exist
                // This prevents misidentifying other formats (like some APKS/ZIPs) that happen to have an info.json
                val isValidApkm = zipFile.getInputStream(infoEntry).use { input ->
                    val content = input.reader().readText()
                    // "pname" and "versioncode" are strictly required by ApkmStrategy
                    content.contains("\"pname\"") && content.contains("\"versioncode\"")
                }

                if (isValidApkm) {
                    return DataType.APKM
                } else {
                    Timber.d("Found info.json but missing APKM keys (pname/versioncode). Skipping APKM detection.")
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to read info.json content. Skipping APKM detection.")
            }
        }

        // 3. Standard APK (AndroidManifest.xml)
        if (zipFile.getEntry("AndroidManifest.xml") != null) return DataType.APK

        // 4. APKS (Split APKs)
        // Feature: contains base.apk or base-master.apk
        val hasBaseApk = entries.any {
            val name = File(it.name).name
            name.equals("base.apk", true) || name.startsWith("base-master")
        }
        if (hasBaseApk) return DataType.APKS

        // 5. Multi-APK Zip (Zip containing arbitrary APKs)
        val hasApkFiles = entries.any {
            !it.isDirectory && it.name.endsWith(".apk", ignoreCase = true)
        }
        if (hasApkFiles) return DataType.MULTI_APK_ZIP

        return DataType.NONE
    }
}