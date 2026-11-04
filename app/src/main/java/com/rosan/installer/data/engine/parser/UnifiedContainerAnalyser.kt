// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.parser

import com.rosan.installer.data.engine.parser.strategy.ApkmStrategy
import com.rosan.installer.data.engine.parser.strategy.ApksStrategy
import com.rosan.installer.data.engine.parser.strategy.ModuleStrategy
import com.rosan.installer.data.engine.parser.strategy.MultiApkZipStrategy
import com.rosan.installer.data.engine.parser.strategy.SingleApkStrategy
import com.rosan.installer.data.engine.parser.strategy.XApkStrategy
import com.rosan.installer.data.engine.signature.LightweightApkSignatureReader
import com.rosan.installer.domain.engine.exception.DescriptorAnalysisUnsupportedException
import com.rosan.installer.domain.engine.model.AnalyseExtraEntity
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.packageinfo.AppSignatureInfo
import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.engine.model.source.DataType
import com.rosan.installer.domain.settings.model.config.ConfigModel
import com.rosan.installer.domain.settings.model.preferences.NetworkAppNameSource
import java.io.IOException
import java.util.zip.ZipEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * A unified entry point for analyzing any package format.
 * It manages the lifecycle of the unified ZIP layer so each source is opened only once.
 */
class UnifiedContainerAnalyser(
    singleApkStrategy: SingleApkStrategy,
    apksStrategy: ApksStrategy,
    apkmStrategy: ApkmStrategy,
    xapkStrategy: XApkStrategy,
    multiApkZipStrategy: MultiApkZipStrategy,
    moduleStrategy: ModuleStrategy,
    private val unifiedZipFileProvider: UnifiedZipFileProvider,
    private val lightweightApkSignatureReader: LightweightApkSignatureReader,
) {

    private val strategies = mapOf(
        DataType.APK to singleApkStrategy,
        DataType.APKS to apksStrategy,
        DataType.APKM to apkmStrategy,
        DataType.XAPK to xapkStrategy,
        DataType.MULTI_APK_ZIP to multiApkZipStrategy,
        DataType.MODULE_ZIP to moduleStrategy,
        DataType.MIXED_MODULE_ZIP to moduleStrategy,
        DataType.MIXED_MODULE_APK to moduleStrategy,
    )

    suspend fun analyze(config: ConfigModel, data: DataEntity, type: DataType, extra: AnalyseExtraEntity): List<AppEntity> = withContext(Dispatchers.IO) {
        val strategy = strategies[type] ?: return@withContext emptyList()
        val analysisExtra = prepareStreamingContainerAnalysis(data, type, extra)

        val entities = if (data is DataEntity.FileEntity) {
            val zipFile = try {
                unifiedZipFileProvider.open(data, type.allowsLocalHeaderFallback)
            } catch (e: IOException) {
                // Detection may classify a non-ZIP file as APK by its extension; the platform
                // asset loader is more lenient than our ZIP backends, so let ApkParser try
                // without a ZIP view instead of failing here. Containers still need the view.
                if (type != DataType.APK) throw e
                Timber.w(e, "ZIP view unavailable for APK analysis, parsing without it: ${data.path}")
                null
            }
            zipFile?.use { archive ->
                val parsed = strategy.analyze(config, data, archive, analysisExtra)
                readStreamingBaseSignatures(parsed, data, type, archive, extra)
            }
                ?: strategy.analyze(config, data, null, analysisExtra)
        } else {
            strategy.analyze(config, data, null, analysisExtra)
        }
        applyNetworkAppName(entities, data)
    }

    /** Analyzes with the archive retained by file-type detection; the caller owns its lifecycle. */
    internal suspend fun analyzeWithArchive(
        config: ConfigModel,
        data: DataEntity,
        type: DataType,
        archive: UnifiedZipFile?,
        extra: AnalyseExtraEntity,
    ): List<AppEntity> = withContext(Dispatchers.IO) {
        val strategy = strategies[type] ?: return@withContext emptyList()
        val analysisExtra = prepareStreamingContainerAnalysis(data, type, extra)
        val parsed = strategy.analyze(config, data, archive, analysisExtra)
        applyNetworkAppName(readStreamingBaseSignatures(parsed, data, type, archive, extra), data)
    }

    private fun readStreamingBaseSignatures(
        entities: List<AppEntity>,
        data: DataEntity,
        type: DataType,
        archive: UnifiedZipFile?,
        extra: AnalyseExtraEntity,
    ): List<AppEntity> {
        if (archive == null || data !is DataEntity.FileDescriptorEntity || !data.isNetworkSource ||
            type == DataType.APK || !extra.checkAppSignature
        ) {
            return entities
        }
        return entities.map { entity ->
            if (entity !is AppEntity.BaseEntity) return@map entity
            val signature = readStreamingBaseSignature(archive, entity.data) {
                lightweightApkSignatureReader.read(it, checkSourceStamp = extra.checkSourceStamp)
            }
            if (signature == null) entity else entity.copy(signatureInfo = signature, signatureHash = null)
        }
    }
}

internal fun readStreamingBaseSignature(
    archive: UnifiedZipFile,
    data: DataEntity,
    readSignature: (DataEntity.FileDescriptorEntity) -> AppSignatureInfo,
): AppSignatureInfo? = try {
    val descriptor = when (data) {
        is DataEntity.FileDescriptorEntity -> data

        is DataEntity.ZipFileEntity -> {
            val entry = archive.getEntry(data.name)
            if (entry?.compressionMethod == ZipEntry.STORED) archive.toDataEntity(entry, data.parent) as? DataEntity.FileDescriptorEntity else null
        }

        else -> null
    }
    descriptor?.takeIf { it.isNetworkSource }?.let(readSignature)
} catch (error: CancellationException) {
    throw error
} catch (error: Exception) {
    // Signature declarations are optional; never download the archive to obtain them.
    Timber.w(error, "Unable to read streamed base APK signing block")
    null
}

internal fun applyNetworkAppName(entities: List<AppEntity>, source: DataEntity): List<AppEntity> {
    if (source !is DataEntity.FileEntity || source.networkAppNameSource != NetworkAppNameSource.ServerFilename) return entities
    if (entities.filterIsInstance<AppEntity.BaseEntity>().map { it.packageName }.distinct().size > 1) return entities
    val name = source.networkFileName?.takeIf { it.isNotBlank() } ?: return entities
    return entities.map { if (it is AppEntity.BaseEntity) it.copy(label = name) else it }
}

internal fun prepareStreamingContainerAnalysis(data: DataEntity, type: DataType, extra: AnalyseExtraEntity): AnalyseExtraEntity {
    if (data !is DataEntity.FileDescriptorEntity || !data.isNetworkSource || type == DataType.APK) return extra
    val splitArchive = type == DataType.APKS || type == DataType.APKM || type == DataType.XAPK
    if (!splitArchive) {
        throw DescriptorAnalysisUnsupportedException(data, IOException("Unsupported streaming container: $type"))
    }
    // Do not materialize a remote archive just for pre-install signature verification.
    // Android verifies every selected APK when the installation session is committed.
    return extra.copy(checkAppSignature = false)
}
