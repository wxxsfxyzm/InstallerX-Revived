package com.rosan.installer.data.app.model.impl.analyser.strategy

import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.model.impl.analyser.ApkParser
import com.rosan.installer.data.app.repo.AnalysisStrategy
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.util.zip.ZipFile

object ApksStrategy : AnalysisStrategy {
    override suspend fun analyze(
        config: ConfigEntity,
        data: DataEntity,
        zipFile: ZipFile?,
        extra: AnalyseExtraEntity
    ): List<AppEntity> = coroutineScope {
        requireNotNull(zipFile) { "APKS requires a valid ZipFile" }

        // 1. Identify Base and Splits efficiently
        val entries = zipFile.entries().asSequence().toList()
        val baseEntry = entries.find { it.name.equals("base.apk", true) || it.name.equals("base-master.apk", true) }
            ?: return@coroutineScope emptyList()

        // 2. Parse Base APK (Heavy operation - needs Icon/Label)
        // We must extract base.apk to temp to use AssetManager for full details
        val baseDeferred = async {
            ApkParser.parseZipEntryFull(config, zipFile, baseEntry, data, extra)
        }

        // 3. Process Splits (Lightweight operation)
        // We infer details from filename where possible to avoid heavy parsing
        val splitEntities = entries
            .filter { it.name.endsWith(".apk", true) && it != baseEntry }
            .map { entry ->
                val splitName = File(entry.name).nameWithoutExtension.removePrefix("split_")
                // We defer waiting for the base package name to avoid blocking,
                // or we pass a "Lazy" builder if strictly needed.
                // For simplicity here, we wait for base to get packageName/version.
                entry to splitName
            }

        val baseResult = baseDeferred.await().firstOrNull() as? AppEntity.BaseEntity
            ?: return@coroutineScope emptyList()

        val finalBase = baseResult.copy(sourceType = extra.dataType)

        val splits = splitEntities.map { (entry, name) ->
            AppEntity.SplitEntity(
                packageName = finalBase.packageName,
                data = DataEntity.ZipFileEntity(entry.name, data as DataEntity.FileEntity),
                splitName = name,
                targetSdk = finalBase.targetSdk,
                minSdk = finalBase.minSdk,
                arch = null, // Can be parsed from name if needed
                sourceType = extra.dataType
            )
        }

        listOf(finalBase) + splits
    }
}