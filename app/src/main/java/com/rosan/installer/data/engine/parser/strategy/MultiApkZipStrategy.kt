// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.parser.strategy

import com.rosan.installer.data.engine.parser.ApkParser
import com.rosan.installer.domain.engine.model.AnalyseExtraEntity
import com.rosan.installer.domain.engine.model.AppEntity
import com.rosan.installer.domain.engine.model.DataEntity
import com.rosan.installer.domain.settings.model.ConfigModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import java.util.zip.ZipFile

class MultiApkZipStrategy(
    private val apkParser: ApkParser
) : AnalysisStrategy {

    override suspend fun analyze(
        config: ConfigModel,
        data: DataEntity,
        zipFile: ZipFile?,
        extra: AnalyseExtraEntity
    ): List<AppEntity> = coroutineScope {
        requireNotNull(zipFile) { "MultiApkZipStrategy requires a valid ZipFile" }
        require(data is DataEntity.FileEntity) { "DataEntity must be FileEntity" }

        // Filter valid APK entries (exclude directories)
        val apkEntries = zipFile.entries().asSequence()
            .filter { !it.isDirectory && it.name.endsWith(".apk", ignoreCase = true) }
            .toList()

        Timber.d("Found ${apkEntries.size} APKs in zip: ${data.path}")

        // Parallel analysis: Extract and parse each APK concurrently
        apkEntries.map { entry ->
            async(Dispatchers.IO) {
                // Use ApkParser to handle extraction and deep analysis
                apkParser.parseZipEntryFull(
                    config,
                    zipFile,
                    entry,
                    data,
                    extra
                ).map { entity ->
                    // Enhance label if missing, using the filename inside zip
                    if (entity is AppEntity.BaseEntity && entity.label == null) {
                        entity.copy(label = entry.name.substringAfterLast('/').substringBeforeLast('.'))
                    } else {
                        entity
                    }
                }
            }
        }.awaitAll().flatten()
    }
}