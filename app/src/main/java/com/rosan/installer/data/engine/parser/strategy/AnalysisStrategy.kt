package com.rosan.installer.data.engine.parser.strategy

import com.rosan.installer.domain.engine.model.AnalyseExtraEntity
import com.rosan.installer.domain.engine.model.AppEntity
import com.rosan.installer.domain.engine.model.DataEntity
import com.rosan.installer.domain.settings.model.ConfigModel
import java.util.zip.ZipFile

interface AnalysisStrategy {
    suspend fun analyze(
        config: ConfigModel,
        data: DataEntity,
        zipFile: ZipFile?, // Nullable for raw single APKs
        extra: AnalyseExtraEntity
    ): List<AppEntity>
}