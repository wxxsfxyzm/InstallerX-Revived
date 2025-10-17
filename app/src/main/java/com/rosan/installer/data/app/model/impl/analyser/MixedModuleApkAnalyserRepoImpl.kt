package com.rosan.installer.data.app.model.impl.analyser

import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.repo.FileAnalyserRepo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Analyzes files that are both a valid APK and a valid Module (contain both
 * AndroidManifest.xml and module.prop). It delegates the analysis to both
 * ApkAnalyserRepoImpl and ModuleZipAnalyserRepoImpl and merges their results.
 */
object MixedModuleApkAnalyserRepoImpl : FileAnalyserRepo {
    override suspend fun doWork(
        config: ConfigEntity,
        data: List<DataEntity>,
        extra: AnalyseExtraEntity
    ): List<AppEntity> = coroutineScope {
        // Analyze as APK
        val apkAnalysis = async {
            ApkAnalyserRepoImpl.doWork(config, data, extra)
        }

        // Analyze as Module
        val moduleAnalysis = async {
            ModuleZipAnalyserRepoImpl.doWork(config, data, extra)
        }

        // Combine results
        apkAnalysis.await() + moduleAnalysis.await()
    }
}