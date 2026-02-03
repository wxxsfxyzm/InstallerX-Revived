package com.rosan.installer.data.app.model.impl.analyser.strategy

import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.model.impl.analyser.ApkParser
import com.rosan.installer.data.app.repo.AnalysisStrategy
import com.rosan.installer.data.app.util.parseSplitMetadata
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
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

        Timber.d("ApksStrategy: Starting analysis for source: ${data.source}")

        val entries = zipFile.entries().asSequence().toList()
        Timber.d("ApksStrategy: Zip file contains ${entries.size} entries.")

        // 1. Identify Base and Splits
        // Use 'File(entry.name).name' to handle nested directories (e.g., "splits/base-master.apk")
        val baseEntry = entries.find { entry ->
            val fileName = File(entry.name).name
            fileName.equals("base.apk", true) || fileName.equals("base-master.apk", true)
        }

        if (baseEntry == null) {
            Timber.e("ApksStrategy: CRITICAL - Base APK not found. Checked for 'base.apk' or 'base-master.apk' in all directories.")
            // Log available entries to help debugging if this still fails
            entries.take(10).forEach { Timber.d("ApksStrategy: Available entry: ${it.name}") }
            return@coroutineScope emptyList()
        }

        Timber.d("ApksStrategy: Base APK identified: ${baseEntry.name}")

        // 2. Parse Base APK (Heavy operation - needs Icon/Label)
        val baseDeferred = async {
            Timber.d("ApksStrategy: Parsing base entry full details...")
            ApkParser.parseZipEntryFull(config, zipFile, baseEntry, data, extra)
        }

        // 3. Process Splits (Lightweight operation)
        val splitEntities = entries
            .filter { entry ->
                val fileName = File(entry.name).name

                // Condition 1: Must end with .apk
                // Condition 2: Must not be the base entry itself
                // Condition 3: Filter out files starting with "base-master" (e.g., base-master_2.apk)
                //              We assume only the exact "base-master.apk" (handled as baseEntry) is valid.
                val isValidSplit = entry.name.endsWith(".apk", true) &&
                        entry.name != baseEntry.name &&
                        !fileName.startsWith("base-master", true)

                if (!isValidSplit && entry.name.endsWith(".apk", true) && entry.name != baseEntry.name) {
                    Timber.d("ApksStrategy: Skipping invalid split file: ${entry.name}")
                }

                isValidSplit
            }
            .map { entry ->
                // Extract filename without extension, handling paths like "splits/base-xx.apk"
                val rawName = File(entry.name).nameWithoutExtension
                val splitName = rawName.removePrefix("split_")

                Timber.d("ApksStrategy: Found split: ${entry.name} -> Name: $splitName")
                entry to splitName
            }

        Timber.d("ApksStrategy: Waiting for base APK parsing to complete...")
        val baseResult = baseDeferred.await().firstOrNull() as? AppEntity.BaseEntity

        if (baseResult == null) {
            Timber.e("ApksStrategy: Base APK parsing returned null or invalid type.")
            return@coroutineScope emptyList()
        }

        Timber.d("ApksStrategy: Base parsed. Package: ${baseResult.packageName}, Version: ${baseResult.versionName}")

        val finalBase = baseResult.copy(sourceType = extra.dataType)

        val splits = splitEntities.map { (entry, name) ->
            val metadata = name.parseSplitMetadata()

            AppEntity.SplitEntity(
                packageName = finalBase.packageName,
                data = DataEntity.ZipFileEntity(entry.name, data as DataEntity.FileEntity),
                splitName = name,
                targetSdk = finalBase.targetSdk,
                minSdk = finalBase.minSdk,
                arch = null,
                sourceType = extra.dataType,
                type = metadata.type,
                filterType = metadata.filterType,
                configValue = metadata.configValue
            )
        }

        Timber.d("ApksStrategy: Analysis finished. Returning 1 base + ${splits.size} splits.")
        listOf(finalBase) + splits
    }
}