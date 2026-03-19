// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.parser.strategy

import com.rosan.installer.data.engine.parser.ApkParser
import com.rosan.installer.domain.engine.model.AnalyseExtraEntity
import com.rosan.installer.domain.engine.model.AppEntity
import com.rosan.installer.domain.engine.model.DataEntity
import com.rosan.installer.domain.settings.model.ConfigModel
import java.util.zip.ZipFile

class SingleApkStrategy(
    private val apkParser: ApkParser
) : AnalysisStrategy {
    override suspend fun analyze(
        config: ConfigModel,
        data: DataEntity,
        zipFile: ZipFile?,
        extra: AnalyseExtraEntity
    ): List<AppEntity> = apkParser.parseFull(data, extra)
}
