package com.rosan.installer.domain.engine.repository

import com.rosan.installer.domain.engine.model.AnalyseExtraEntity
import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.engine.model.packageinfo.PackageAnalysisResult
import com.rosan.installer.domain.settings.model.config.ConfigModel

interface AnalyserRepository {
    suspend fun doWork(
        config: ConfigModel,
        data: List<DataEntity>,
        extra: AnalyseExtraEntity
    ): List<PackageAnalysisResult>
}