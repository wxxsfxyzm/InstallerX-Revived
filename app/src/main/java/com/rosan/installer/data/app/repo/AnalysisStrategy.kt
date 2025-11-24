package com.rosan.installer.data.app.repo

import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import java.util.zip.ZipFile

interface AnalysisStrategy {
    suspend fun analyze(
        config: ConfigEntity,
        data: DataEntity,
        zipFile: ZipFile?, // Nullable for raw single APKs
        extra: AnalyseExtraEntity
    ): List<AppEntity>
}