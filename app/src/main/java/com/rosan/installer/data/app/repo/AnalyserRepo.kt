package com.rosan.installer.data.app.repo

import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.model.entity.PackageAnalysisResult
import com.rosan.installer.domain.settings.model.ConfigModel

interface AnalyserRepo {
    suspend fun doWork(
        config: ConfigModel,
        data: List<DataEntity>,
        extra: AnalyseExtraEntity
    ): List<PackageAnalysisResult>
}