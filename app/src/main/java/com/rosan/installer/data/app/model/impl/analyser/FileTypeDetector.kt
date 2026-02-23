package com.rosan.installer.data.app.model.impl.analyser

import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.model.enums.DataType
import com.rosan.installer.data.app.model.exception.AnalyseFailedCorruptedArchiveException
import com.rosan.installer.util.ArchiveUtils
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile

object FileTypeDetector : KoinComponent {

    private val json: Json by inject()

    fun detect(data: DataEntity, extra: AnalyseExtraEntity): DataType {
        val fileEntity = data as? DataEntity.FileEntity ?: return DataType.NONE
        val isZip = ArchiveUtils.isZipArchive(File(fileEntity.path))

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
            if (e is ZipException) {
                if (isZip) {
                    // The file contains the ZIP magic number but failed to open.
                    // This typically means the file is truncated or corrupted.
                    Timber.e(e, "Archive is corrupted or truncated: ${fileEntity.path}")
                    throw AnalyseFailedCorruptedArchiveException("Archive is corrupted or truncated", e)
                } else {
                    // The file does not have the ZIP magic number, so it is not a ZIP file at all.
                    handleNonZipFallback(fileEntity, e)
                }
            } else {
                Timber.e(e, "Failed to detect file type for path: ${fileEntity.path}")
                DataType.NONE
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
                Timber.d("Detected MIXED_MODULE_APK")
                DataType.MIXED_MODULE_APK
            }
            // Module containing APKs
            hasApksInside -> {
                Timber.d("Detected MIXED_MODULE_ZIP")
                DataType.MIXED_MODULE_ZIP
            }
            // Pure module
            else -> {
                Timber.d("Detected MODULE_ZIP")
                DataType.MODULE_ZIP
            }
        }
    }

    private fun detectStandardType(zipFile: ZipFile, entries: List<ZipEntry>): DataType {
        // 1. XAPK (manifest.json)
        val manifestEntry = zipFile.getEntry("manifest.json")
        if (manifestEntry != null) {
            try {
                val content = zipFile.getInputStream(manifestEntry).use { input ->
                    input.reader().readText()
                }

                // Parse as a dynamic JsonElement tree instead of strict deserialization
                val jsonObject = json.parseToJsonElement(content).jsonObject

                // A valid XAPK must have basic package identification
                val hasBaseInfo = jsonObject.containsKey("package_name") && jsonObject.containsKey("version_code")

                // A valid XAPK must define its installation payload.
                // It either contains 'split_apks' (modern) or 'expansions' (OBB format).
                val hasSplitApks = jsonObject.containsKey("split_apks")
                val hasExpansions = jsonObject.containsKey("expansions")

                if (hasBaseInfo && (hasSplitApks || hasExpansions)) {
                    return DataType.XAPK
                } else {
                    Timber.d("manifest.json found, but missing payload definitions (split_apks or expansions). Skipping XAPK detection.")
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to parse manifest.json via kotlinx.serialization. Skipping XAPK detection.")
            }
        }

        // 2. APKM (info.json)
        val infoEntry = zipFile.getEntry("info.json")
        if (infoEntry != null) {
            try {
                val content = zipFile.getInputStream(infoEntry).use { input ->
                    input.reader().readText()
                }

                val jsonObject = json.parseToJsonElement(content).jsonObject

                // APKM requires these specific keys
                val isValidApkm = jsonObject.containsKey("pname") && jsonObject.containsKey("versioncode")

                if (isValidApkm) {
                    return DataType.APKM
                } else {
                    Timber.d("Found info.json but missing APKM keys (pname/versioncode). Skipping APKM detection.")
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to parse info.json via kotlinx.serialization. Skipping APKM detection.")
            }
        }

        // 3. Standard APK (AndroidManifest.xml)
        if (zipFile.getEntry("AndroidManifest.xml") != null) return DataType.APK

        // 4. APKS (Split APKs)
        val hasTocPb = zipFile.getEntry("toc.pb") != null
        if (hasTocPb) return DataType.APKS

        val hasBaseApk = entries.any {
            val name = File(it.name).name
            name.equals("base.apk", true) || name.startsWith("base-master")
        }
        if (hasBaseApk) return DataType.APKS

        // 5. Multi-APK Zip
        val hasApkFiles = entries.any {
            !it.isDirectory && it.name.endsWith(".apk", ignoreCase = true)
        }
        if (hasApkFiles) return DataType.MULTI_APK_ZIP

        return DataType.NONE
    }

    private fun handleNonZipFallback(fileEntity: DataEntity.FileEntity, e: Exception): DataType =
        if (fileEntity.path.endsWith(".apk", ignoreCase = true)) {
            // Fallback: assume APK if path ends with .apk and zip open failed
            DataType.APK
        } else {
            Timber.e(e, "File is not a valid ZIP archive: ${fileEntity.path}")
            DataType.NONE
        }
}
