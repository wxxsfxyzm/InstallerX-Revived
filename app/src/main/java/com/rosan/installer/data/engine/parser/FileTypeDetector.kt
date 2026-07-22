// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.parser

import android.os.Build
import com.rosan.installer.core.env.AppConfig
import com.rosan.installer.domain.engine.exception.AnalyseException
import com.rosan.installer.domain.engine.model.AnalyseExtraEntity
import com.rosan.installer.domain.engine.model.error.AnalyseErrorType
import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.engine.model.source.DataType
import com.rosan.installer.util.isZipMagicNumber
import dalvik.system.ZipPathValidator
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import timber.log.Timber
import java.io.File
import java.io.IOException

class FileTypeDetector(
    private val json: Json,
    private val unifiedZipFileProvider: UnifiedZipFileProvider
) {

    private companion object {
        const val ENTRY_LOG_LIMIT = 20
        const val ENTRY_NAME_LOG_LIMIT = 160
        const val FILE_HEADER_LOG_BYTES = 8
    }

    init {
        disableZipPathValidation()
    }

    private fun disableZipPathValidation() {
        // ZipPathValidator was introduced in Android 14 (API 34)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                ZipPathValidator.clearCallback()
                Timber.d("ZipPathValidator callback cleared for safe in-memory analysis.")
            } catch (e: Exception) {
                Timber.w(e, "Failed to clear ZipPathValidator callback.")
            }
        }
    }

    fun detect(data: DataEntity, extra: AnalyseExtraEntity): DataType {
        val fileEntity = data as? DataEntity.FileEntity
        if (fileEntity == null) {
            Timber.d(
                "File type detection skipped: entity=${data::class.java.simpleName}, " +
                        "source=${data.source}"
            )
            return DataType.NONE
        }

        val file = File(fileEntity.path)
        val descriptorBacked = fileEntity is DataEntity.FileDescriptorEntity
        val sourcePath = (fileEntity.getSourceTop() as? DataEntity.FileEntity)?.path
        val sourceExtension = sourcePath?.let { File(it).extension }.orEmpty()
        val isZip = fileEntity.hasZipMagic()

        Timber.d(
            "File type detection started: workingPath=${fileEntity.path}, sourcePath=$sourcePath, " +
                    "workingExtension=${file.extension.ifBlank { "<none>" }}, " +
                    "sourceExtension=${sourceExtension.ifBlank { "<none>" }}, " +
                    "descriptorBacked=$descriptorBacked, " +
                    "exists=${descriptorBacked || file.exists()}, isFile=${descriptorBacked || file.isFile}, " +
                    "canRead=${descriptorBacked || file.canRead()}, size=${fileEntity.getSize()}, " +
                    "header=${fileEntity.readHeaderForLog()}, zipMagic=$isZip, " +
                    "moduleDetection=${extra.isModuleFlashEnabled}"
        )

        return try {
            val detectedType = detectArchiveType(fileEntity, sourceExtension, extra)

            Timber.d(
                "File type detection finished: workingPath=${fileEntity.path}, " +
                        "sourcePath=$sourcePath, result=$detectedType"
            )
            if (sourceExtension.equals("apks", ignoreCase = true) && detectedType != DataType.APKS) {
                Timber.w(
                    "File type/extension mismatch: source extension is .apks but content detector " +
                            "selected $detectedType. See archive diagnostics and selected-rule logs above."
                )
            }
            detectedType
        } catch (e: Exception) {
            if (e is AnalyseException) throw e

            when {
                e is IOException && isZip -> {
                    // The file has ZIP magic, but neither the central nor local-header view was usable.
                    Timber.e(e, "Archive is corrupted or truncated: ${fileEntity.path}")
                    throw AnalyseException(
                        errorType = AnalyseErrorType.CORRUPTED_ARCHIVE,
                        message = "Archive is corrupted or truncated",
                        cause = e
                    )
                }

                e is IOException -> {
                    // The file does not have the ZIP magic number, so it is not a ZIP file at all.
                    handleNonZipFallback(fileEntity, e)
                }

                else -> {
                    Timber.e(e, "Failed to detect file type for path: ${fileEntity.path}")
                    DataType.NONE
                }
            }
        }
    }

    private fun detectArchiveType(
        file: DataEntity.FileEntity,
        sourceExtension: String,
        extra: AnalyseExtraEntity
    ): DataType {
        if (extra.isModuleFlashEnabled) {
            try {
                unifiedZipFileProvider.open(file, allowLocalHeaderFallback = false).use { zipFile ->
                    detectModuleType(zipFile)?.let { result ->
                        if (shouldLogArchiveDiagnostics(sourceExtension, result, zipFile.entries)) {
                            logArchiveDiagnostics(file.path, zipFile)
                        }
                        return result
                    }
                }
            } catch (e: AnalyseException) {
                throw e
            } catch (e: IOException) {
                Timber.d(
                    e,
                    "Module central-directory precheck unavailable; trying Android package fallback: ${file.path}"
                )
            }
        }

        return unifiedZipFileProvider.open(file, allowLocalHeaderFallback = true).use { zipFile ->
            val result = detectStandardType(zipFile)
            if (shouldLogArchiveDiagnostics(sourceExtension, result, zipFile.entries)) {
                logArchiveDiagnostics(file.path, zipFile)
            }
            result
        }
    }

    private fun detectModuleType(zipFile: UnifiedZipFile): DataType? {
        val entries = zipFile.entries
        // Core feature: presence of module.prop
        val hasModuleProp = entries.any {
            it.name == "module.prop" || it.name == "common/module.prop"
        }

        if (!hasModuleProp) return null

        val hasAndroidManifest = zipFile.getEntry("AndroidManifest.xml") != null
        val hasApksInside = entries.any { !it.isDirectory && it.name.endsWith(".apk", ignoreCase = true) }

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

    private fun detectStandardType(zipFile: UnifiedZipFile): DataType {
        val entries = zipFile.entries
        // 1. XAPK (manifest.json)
        val manifestEntry = zipFile.getEntry("manifest.json")
        if (manifestEntry != null) {
            try {
                val content = zipFile.openEntry(manifestEntry).use { input ->
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
            } catch (e: AnalyseException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Failed to parse manifest.json via kotlinx.serialization. Skipping XAPK detection.")
            }
        }

        // 2. APKM (info.json)
        val infoEntry = zipFile.getEntry("info.json")
        if (infoEntry != null) {
            try {
                val content = zipFile.openEntry(infoEntry).use { input ->
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
            } catch (e: AnalyseException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Failed to parse info.json via kotlinx.serialization. Skipping APKM detection.")
            }
        }

        // 3. Standard APK (AndroidManifest.xml)
        val androidManifestEntry = zipFile.getEntry("AndroidManifest.xml")
        if (androidManifestEntry != null) {
            val tocEntry = zipFile.getEntry("toc.pb")
            val baseApkEntries = entries.filter { it.isBaseApkMarker() }
            val apkEntries = entries.filter { it.isApkFile() }
            Timber.d(
                "File type selected: APK (rule=root AndroidManifest.xml; " +
                        "lower-priority APKS markers: toc.pb=${tocEntry != null}, " +
                        "baseApk=${baseApkEntries.summarizeNamesForLog()}, " +
                        "nestedApkCount=${apkEntries.size})."
            )
            return DataType.APK
        }

        // 4. APKS (Split APKs)
        val hasTocPb = zipFile.getEntry("toc.pb") != null
        if (hasTocPb) return DataType.APKS

        val hasBaseApk = entries.any { it.isBaseApkMarker() }
        if (hasBaseApk) return DataType.APKS

        // 5. Multi-APK Zip
        val hasApkFiles = entries.any { it.isApkFile() }
        if (hasApkFiles) return DataType.MULTI_APK_ZIP

        return DataType.NONE
    }

    private fun handleNonZipFallback(fileEntity: DataEntity.FileEntity, e: Exception): DataType =
        if (fileEntity.path.endsWith(".apk", ignoreCase = true)) {
            // Fallback: assume APK if path ends with .apk and zip open failed
            Timber.w(e, "File type selected: APK (rule=non-ZIP .apk path fallback): ${fileEntity.path}")
            DataType.APK
        } else {
            Timber.e(e, "File is not a valid ZIP archive: ${fileEntity.path}")
            DataType.NONE
        }

    private fun shouldLogArchiveDiagnostics(
        sourceExtension: String,
        detectedType: DataType,
        entries: List<UnifiedZipEntry>
    ): Boolean = if (AppConfig.isDebug) true else sourceExtension.equals("apks", ignoreCase = true) ||
            detectedType == DataType.APK && entries.any {
        it.isApkFile() || it.isBaseApkCandidate() || File(it.name).name.equals("toc.pb", ignoreCase = true)
    }

    private fun logArchiveDiagnostics(
        path: String,
        zipFile: UnifiedZipFile
    ) {
        val entries = zipFile.entries
        val files = entries.filterNot { it.isDirectory }
        val androidManifestCandidates = entries.filter {
            File(it.name).name.equals("AndroidManifest.xml", ignoreCase = true)
        }
        val tocCandidates = entries.filter {
            File(it.name).name.equals("toc.pb", ignoreCase = true)
        }
        val baseApkCandidates = entries.filter { it.isBaseApkCandidate() }
        val apkEntries = entries.filter { it.isApkFile() }

        Timber.d(
            "Archive diagnostics: backend=${zipFile.backend}, path=$path, " +
                    "entries=${entries.size}, files=${files.size}, " +
                    "directories=${entries.size - files.size}, nestedApkCount=${apkEntries.size}"
        )
        Timber.d(
            "Archive diagnostics markers: rootAndroidManifest=${zipFile.getEntry("AndroidManifest.xml") != null}, " +
                    "rootTocPb=${zipFile.getEntry("toc.pb") != null}, " +
                    "AndroidManifest.xml(any depth/case)=${androidManifestCandidates.summarizeNamesForLog()}, " +
                    "toc.pb(any depth/case)=${tocCandidates.summarizeNamesForLog()}, " +
                    "baseApk(any depth/case)=${baseApkCandidates.summarizeNamesForLog()}"
        )
        Timber.d("Archive diagnostics nested APKs: ${apkEntries.summarizeNamesForLog()}")
        Timber.d("Archive diagnostics entry sample: ${entries.summarizeNamesForLog()}")
    }

    private fun UnifiedZipEntry.isApkFile(): Boolean =
        !isDirectory && name.endsWith(".apk", ignoreCase = true)

    private fun UnifiedZipEntry.isBaseApkMarker(): Boolean {
        val leafName = File(name).name
        return leafName.equals("base.apk", ignoreCase = true) || leafName.startsWith("base-master")
    }

    private fun UnifiedZipEntry.isBaseApkCandidate(): Boolean {
        val leafName = File(name).name
        return leafName.equals("base.apk", ignoreCase = true) ||
                leafName.startsWith("base-master", ignoreCase = true)
    }

    private fun List<UnifiedZipEntry>.summarizeNamesForLog(): String {
        if (isEmpty()) return "<none>"

        val shownEntries = take(ENTRY_LOG_LIMIT).joinToString { entry ->
            if (entry.name.length <= ENTRY_NAME_LOG_LIMIT) {
                entry.name
            } else {
                entry.name.take(ENTRY_NAME_LOG_LIMIT) + "..."
            }
        }
        val omittedCount = size - ENTRY_LOG_LIMIT
        return if (omittedCount > 0) "$shownEntries, ... (+$omittedCount more)" else shownEntries
    }

    private fun DataEntity.FileEntity.hasZipMagic(): Boolean = runCatching {
        getInputStream().use { input ->
            val buffer = ByteArray(4)
            input.read(buffer) == buffer.size && buffer.isZipMagicNumber()
        }
    }.getOrDefault(false)

    private fun DataEntity.FileEntity.readHeaderForLog(): String = runCatching {
        getInputStream().use { input ->
            val buffer = ByteArray(FILE_HEADER_LOG_BYTES)
            val bytesRead = input.read(buffer)
            if (bytesRead <= 0) {
                "<empty>"
            } else {
                buffer.take(bytesRead).joinToString(separator = " ") { byte ->
                    "%02X".format(byte.toInt() and 0xFF)
                }
            }
        }
    }.getOrElse { error ->
        "<unreadable:${error::class.java.simpleName}:${error.message}>"
    }
}
