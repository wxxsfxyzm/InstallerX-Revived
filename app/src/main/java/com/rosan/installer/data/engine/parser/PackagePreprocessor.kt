// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.parser

import com.rosan.installer.domain.engine.model.AppEntity
import com.rosan.installer.domain.engine.model.DataEntity
import com.rosan.installer.domain.engine.model.DataType
import com.rosan.installer.domain.engine.model.InstalledAppInfo
import com.rosan.installer.domain.engine.model.PackageIdentityStatus
import com.rosan.installer.domain.engine.model.SignatureMatchStatus
import com.rosan.installer.domain.engine.model.sourcePath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.util.zip.ZipFile

object PackagePreprocessor {

    data class ProcessedGroup(
        val packageName: String,
        val entities: List<AppEntity>,
        val installedInfo: InstalledAppInfo?
    )

    data class SessionTypeInfo(
        val isMultiAppSession: Boolean,
        val sessionType: DataType,
        val isFromSingleFile: Boolean
    )

    /**
     * Process raw data in parallel: Group -> Deduplicate -> Get installed system info.
     */
    suspend fun process(rawEntities: List<AppEntity>): List<ProcessedGroup> = coroutineScope {
        rawEntities
            .groupBy { it.packageName }
            .map { (packageName, entities) ->
                async(Dispatchers.IO) {
                    // 1. Parallel Deduplication (IO intensive)
                    val deduplicated = deduplicateEntities(entities)
                    // 2. Parallel fetching of system info (IPC/IO intensive)
                    val installedInfo = InstalledAppInfo.buildByPackageName(packageName)

                    ProcessedGroup(packageName, deduplicated, installedInfo)
                }
            }.awaitAll()
    }

    /**
     * Determine session type (used for UI display logic).
     */
    fun determineSessionType(
        groups: List<ProcessedGroup>,
        rawEntities: List<AppEntity>
    ): SessionTypeInfo {
        val allEntities = groups.flatMap { it.entities }

        // 1. Determine the number of source files
        val sourcePaths = allEntities.mapNotNull { it.data.sourcePath() }.distinct()
        val isFromSingleFile = sourcePaths.size == 1

        // 2. Get main container type
        val firstType = allEntities.firstOrNull()?.sourceType

        // 3. Determine if it is multi-app mode
        val isMultiPackage = groups.size > 1
        val hasMultipleBases = !isMultiPackage && allEntities.count { it is AppEntity.BaseEntity } > 1

        // Special logic: If it is a mixed module, force it not to be treated as multi-app mode to maintain integrity.
        val isMixedModule = firstType == DataType.MIXED_MODULE_APK || firstType == DataType.MIXED_MODULE_ZIP

        val isMultiAppSession = !isMixedModule && (isMultiPackage || hasMultipleBases)

        // 4. Correct the final container type display
        val finalContainerType = when {
            isMultiAppSession && firstType == DataType.MULTI_APK_ZIP -> DataType.MULTI_APK_ZIP
            isMultiAppSession -> DataType.MULTI_APK
            else -> firstType ?: DataType.APK
        }

        return SessionTypeInfo(isMultiAppSession, finalContainerType, isFromSingleFile)
    }

    /**
     * Helper method for signature verification status.
     */
    fun checkSignature(
        baseEntity: AppEntity.BaseEntity?,
        installedInfo: InstalledAppInfo?
    ): SignatureMatchStatus {
        return when {
            installedInfo == null -> SignatureMatchStatus.NOT_INSTALLED
            baseEntity?.signatureHash.isNullOrBlank() || installedInfo.signatureHash.isNullOrBlank() ->
                SignatureMatchStatus.UNKNOWN_ERROR

            baseEntity.signatureHash == installedInfo.signatureHash ->
                SignatureMatchStatus.MATCH

            else ->
                SignatureMatchStatus.MISMATCH
        }
    }

    // Internal: Deduplication logic
    private suspend fun deduplicateEntities(entities: List<AppEntity>): List<AppEntity> = coroutineScope {
        val bases = entities.filterIsInstance<AppEntity.BaseEntity>()
        // If there is only one Base, no deduplication calculation is needed, return directly
        if (bases.size <= 1) return@coroutineScope entities

        // Calculate fingerprint
        val hashedBases = bases.map { entity ->
            async { calculateFingerprint(entity) to entity }
        }.awaitAll()

        // Deduplicate based on fingerprint
        val distinctBases = hashedBases
            .distinctBy { it.first } // pair.first is hash
            .map { it.second }

        val others = entities.filter { it !is AppEntity.BaseEntity }
        distinctBases + others
    }

    private fun calculateFingerprint(entity: AppEntity.BaseEntity): String {
        return when (val data = entity.data) {
            is DataEntity.FileEntity -> {
                // Full file hash (slower but accurate)
                File(data.path).calculateSHA256() ?: "${data.path}_${entity.versionCode}"
            }

            is DataEntity.ZipFileEntity -> {
                // Zip Entry optimization: Use CRC + Size only, no decompression, extremely fast
                try {
                    ZipFile(data.parent.path).use { zip ->
                        zip.getEntry(data.name)?.let { "${it.crc}|${it.size}" }
                    } ?: "${data.name}_${entity.versionCode}"
                } catch (_: Exception) {
                    "${data.name}_${entity.versionCode}"
                }
            }

            else -> "${entity.packageName}_${entity.versionCode}"
        }
    }

    /**
     * Checks if the incoming base APK is identical to the installed APK.
     * Only works for DataType.APK and assumes the data is a FileEntity.
     */
    suspend fun checkPackageIdentity(
        baseEntity: AppEntity.BaseEntity?,
        installedInfo: InstalledAppInfo?,
        sessionType: DataType
    ): PackageIdentityStatus = coroutineScope {
        // 1. Fast applicability checks
        if (sessionType != DataType.APK) {
            return@coroutineScope PackageIdentityStatus.NOT_APPLICABLE
        }

        if (baseEntity == null || installedInfo == null) {
            return@coroutineScope PackageIdentityStatus.NOT_APPLICABLE
        }

        if (baseEntity.versionCode != installedInfo.versionCode ||
            baseEntity.versionName != installedInfo.versionName
        ) {
            return@coroutineScope PackageIdentityStatus.NOT_APPLICABLE
        }

        // 2. Validate installed APK path.
        val installedApkPath = installedInfo.sourceDir
        if (installedApkPath.isNullOrBlank()) {
            return@coroutineScope PackageIdentityStatus.ERROR
        }
        val installedApkFile = File(installedApkPath)
        if (!installedApkFile.exists() || !installedApkFile.isFile) {
            return@coroutineScope PackageIdentityStatus.ERROR
        }

        // 3. Extract the new APK file.
        // Safely cast to FileEntity as guaranteed by the caller constraints.
        val fileData = baseEntity.data as? DataEntity.FileEntity
            ?: return@coroutineScope PackageIdentityStatus.ERROR

        val newApkFile = File(fileData.path)

        // 4. Fast-fail optimization: compare file sizes first.
        if (newApkFile.length() != installedApkFile.length()) {
            return@coroutineScope PackageIdentityStatus.DIFFERENT
        }

        // 5. Heavy validation: Full SHA-256 Hash Comparison.
        val newAppHashDeferred = async(Dispatchers.IO) { newApkFile.calculateSHA256() }
        val installedAppHashDeferred = async(Dispatchers.IO) { installedApkFile.calculateSHA256() }

        val newAppHash = newAppHashDeferred.await()
        val installedAppHash = installedAppHashDeferred.await()

        if (newAppHash != null && newAppHash == installedAppHash) {
            return@coroutineScope PackageIdentityStatus.IDENTICAL
        } else if (newAppHash != null && installedAppHash != null) {
            return@coroutineScope PackageIdentityStatus.DIFFERENT
        }

        return@coroutineScope PackageIdentityStatus.ERROR
    }
}