package com.rosan.installer.data.app.repo

import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataEntity
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