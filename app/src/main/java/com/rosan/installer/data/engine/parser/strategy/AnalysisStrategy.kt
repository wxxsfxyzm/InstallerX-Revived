// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.parser.strategy

import com.rosan.installer.data.engine.parser.UnifiedZipFile
import com.rosan.installer.domain.engine.model.AnalyseExtraEntity
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.settings.model.config.ConfigModel

interface AnalysisStrategy {
    suspend fun analyze(
        config: ConfigModel,
        data: DataEntity,
        zipFile: UnifiedZipFile?,
        extra: AnalyseExtraEntity
    ): List<AppEntity>
}
