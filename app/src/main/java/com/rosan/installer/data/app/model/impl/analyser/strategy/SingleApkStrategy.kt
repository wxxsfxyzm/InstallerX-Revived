package com.rosan.installer.data.app.model.impl.analyser.strategy

import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.model.impl.analyser.ApkParser
import com.rosan.installer.data.app.repo.AnalysisStrategy
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import java.util.zip.ZipFile

object SingleApkStrategy : AnalysisStrategy {
    override suspend fun analyze(
        config: ConfigEntity,
        data: DataEntity,
        zipFile: ZipFile?,
        extra: AnalyseExtraEntity
    ): List<AppEntity> = ApkParser.parseFull(config, data, extra)
}
