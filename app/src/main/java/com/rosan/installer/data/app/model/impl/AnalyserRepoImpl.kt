package com.rosan.installer.data.app.model.impl

import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.model.entity.PackageAnalysisResult
import com.rosan.installer.data.app.model.enums.DataType
import com.rosan.installer.data.app.model.enums.SessionMode
import com.rosan.installer.data.app.model.impl.analyser.FileTypeDetector
import com.rosan.installer.data.app.model.impl.analyser.UnifiedContainerAnalyser
import com.rosan.installer.data.app.model.impl.processor.PackagePreprocessor
import com.rosan.installer.data.app.model.impl.processor.SelectionStrategy
import com.rosan.installer.data.app.repo.AnalyserRepo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import java.util.zip.ZipException

object AnalyserRepoImpl : AnalyserRepo {
    override suspend fun doWork(
        config: ConfigEntity,
        data: List<DataEntity>,
        extra: AnalyseExtraEntity
    ): List<PackageAnalysisResult> = coroutineScope {
        if (data.isEmpty()) return@coroutineScope emptyList()

        Timber.d("AnalyserRepo: Step 0 - Starting analysis for ${data.size} items.")

        // Step 1: Analyze all inputs
        val rawEntities = data.map { entity ->
            async(Dispatchers.IO) {
                Timber.d("AnalyserRepo: analyzing source -> ${entity.source}")
                val result = analyzeSingleSource(config, entity, extra)
                Timber.d("AnalyserRepo: source result -> ${entity.source} yielded ${result.size} entities: ${result.map { it.packageName }}") // [Log 2] 该文件解析出了什么
                result
            }
        }.awaitAll().flatten()

        Timber.d("AnalyserRepo: Step 1 Finished. Total rawEntities count: ${rawEntities.size}")
        rawEntities.forEachIndexed { index, app ->
            Timber.d("  RawEntity[$index]: pkg=${app.packageName}, path=${(app.data as? DataEntity.FileEntity)?.path}")
        }

        if (rawEntities.isEmpty()) {
            Timber.w("Analysis yielded no valid entities.")
            return@coroutineScope emptyList()
        }

        // Step 2: Group, Deduplicate
        val processedGroups = PackagePreprocessor.process(rawEntities)

        Timber.d("AnalyserRepo: Step 2 Processed. Groups count: ${processedGroups.size}")
        processedGroups.forEach { group ->
            Timber.d("  Group: ${group.packageName} contains ${group.entities.size} entities")
        }
        val hasMultipleBasesInAnyGroup = processedGroups.any { group ->
            group.entities.count { it is AppEntity.BaseEntity } > 1
        }
        val detectedMode = if (processedGroups.size > 1 || hasMultipleBasesInAnyGroup) {
            SessionMode.Batch
        } else {
            SessionMode.Single
        }
        // Step 3: Determine Session Context
        val sessionDataType = PackagePreprocessor.determineSessionType(processedGroups, rawEntities)
        Timber.d("AnalyserRepo: Step 3 SessionType -> ${sessionDataType.sessionType}")

        // Step 4: Apply Selection Strategy and Build Result
        val finalResults = processedGroups.map { group ->
            val selectableEntities = SelectionStrategy.select(
                splitChooseAll = config.splitChooseAll,
                apkChooseAll = config.apkChooseAll,
                entities = group.entities,
                sessionDataType.sessionType
            )

            Timber.d("AnalyserRepo: Step 4 Strategy for ${group.packageName} -> Input: ${group.entities.size}, Selected: ${selectableEntities.size}")

            if (selectableEntities.isEmpty()) {
                Timber.w("AnalyserRepo: WARNING! ${group.packageName} has 0 entities after selection!")
            }

            val signatureStatus = PackagePreprocessor.checkSignature(
                group.entities.firstOrNull { it is AppEntity.BaseEntity } as? AppEntity.BaseEntity,
                group.installedInfo
            )

            PackageAnalysisResult(
                packageName = group.packageName,
                appEntities = selectableEntities,
                installedAppInfo = group.installedInfo,
                signatureMatchStatus = signatureStatus,
                sessionMode = detectedMode
            )
        }

        Timber.d("AnalyserRepo: Final Result Count -> ${finalResults.size}")

        return@coroutineScope finalResults
    }

    private suspend fun analyzeSingleSource(
        config: ConfigEntity,
        data: DataEntity,
        extra: AnalyseExtraEntity
    ): List<AppEntity> =
        try {
            // Detect type efficiently
            val fileType = FileTypeDetector.detect(data, extra)
            Timber.d("AnalyserRepo: FileType -> $fileType")
            if (fileType == DataType.NONE) return emptyList()
            // Delegate to the Unified Analyzer
            UnifiedContainerAnalyser.analyze(config, data, fileType, extra.copy(dataType = fileType))
        } catch (e: Exception) {
            Timber.e(e, "Fatal error analyzing source: ${data.source}")
            if (e is ZipException) throw e
            else emptyList()
        }
}