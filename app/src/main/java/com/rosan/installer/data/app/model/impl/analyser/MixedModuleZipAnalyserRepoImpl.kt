package com.rosan.installer.data.app.model.impl.analyser

import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.repo.FileAnalyserRepo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Analyzes ZIP files that are both a Module (contain module.prop) and
 * contain separate APK files (e.g., a manager app). It delegates analysis
 * to both ModuleZipAnalyserRepoImpl and MultiApkZipAnalyserRepoImpl.
 */
object MixedModuleZipAnalyserRepoImpl : FileAnalyserRepo {
    override suspend fun doWork(
        config: ConfigEntity,
        data: List<DataEntity>,
        extra: AnalyseExtraEntity
    ): List<AppEntity> = coroutineScope {
        // Analyze as Module
        val moduleAnalysis = async {
            ModuleZipAnalyserRepoImpl.doWork(config, data, extra)
        }

        // Analyze as a ZIP containing APKs
        val multiApkAnalysis = async {
            MultiApkZipAnalyserRepoImpl.doWork(config, data, extra)
        }

        // Combine results
        moduleAnalysis.await() + multiApkAnalysis.await()
    }
}