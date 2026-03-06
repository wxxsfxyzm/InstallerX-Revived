// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.parser

import com.rosan.installer.data.engine.parser.strategy.ApkmStrategy
import com.rosan.installer.data.engine.parser.strategy.ApksStrategy
import com.rosan.installer.data.engine.parser.strategy.ModuleStrategy
import com.rosan.installer.data.engine.parser.strategy.MultiApkZipStrategy
import com.rosan.installer.data.engine.parser.strategy.SingleApkStrategy
import com.rosan.installer.data.engine.parser.strategy.XApkStrategy
import com.rosan.installer.domain.engine.model.AnalyseExtraEntity
import com.rosan.installer.domain.engine.model.AppEntity
import com.rosan.installer.domain.engine.model.DataEntity
import com.rosan.installer.domain.engine.model.DataType
import com.rosan.installer.domain.settings.model.ConfigModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.zip.ZipFile

/**
 * A unified entry point for analyzing any package format.
 * It manages the lifecycle of the ZipFile (if applicable) to ensure it's opened only once.
 */
object UnifiedContainerAnalyser {

    private val strategies = mapOf(
        DataType.APK to SingleApkStrategy,
        DataType.APKS to ApksStrategy,
        DataType.APKM to ApkmStrategy,
        DataType.XAPK to XApkStrategy,
        DataType.MULTI_APK_ZIP to MultiApkZipStrategy,
        DataType.MODULE_ZIP to ModuleStrategy,
        DataType.MIXED_MODULE_ZIP to ModuleStrategy,
        DataType.MIXED_MODULE_APK to ModuleStrategy
    )

    suspend fun analyze(
        config: ConfigModel,
        data: DataEntity,
        type: DataType,
        extra: AnalyseExtraEntity
    ): List<AppEntity> = withContext(Dispatchers.IO) {
        val strategy = strategies[type] ?: return@withContext emptyList()

        // If it's a file entity and NOT a raw APK, treat it as a Zip Container
        if (data is DataEntity.FileEntity && type != DataType.APK) {
            ZipFile(data.path).use { zip ->
                strategy.analyze(config, data, zip, extra)
            }
        } else {
            // Single APK or other non-zip stream sources
            strategy.analyze(config, data, null, extra)
        }
    }
}