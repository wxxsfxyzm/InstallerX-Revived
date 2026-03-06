// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.data.engine.repository

import com.rosan.installer.data.engine.parser.FileTypeDetector
import com.rosan.installer.data.engine.parser.PackagePreprocessor
import com.rosan.installer.data.engine.parser.UnifiedContainerAnalyser
import com.rosan.installer.domain.engine.model.AnalyseExtraEntity
import com.rosan.installer.domain.engine.model.AppEntity
import com.rosan.installer.domain.engine.model.DataEntity
import com.rosan.installer.domain.engine.model.DataType
import com.rosan.installer.domain.engine.model.PackageAnalysisResult
import com.rosan.installer.domain.engine.model.SessionMode
import com.rosan.installer.domain.engine.repository.AnalyserRepository
import com.rosan.installer.domain.engine.usecase.SelectOptimalSplitsUseCase
import com.rosan.installer.domain.settings.model.ConfigModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import java.util.zip.ZipException

class AnalyserRepositoryImpl(
    private val selectOptimalSplitsUseCase: SelectOptimalSplitsUseCase
) : AnalyserRepository {
    override suspend fun doWork(
        config: ConfigModel,
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
            val selectableEntities = selectOptimalSplitsUseCase(
                splitChooseAll = config.splitChooseAll,
                apkChooseAll = config.apkChooseAll,
                entities = group.entities,
                sessionType = sessionDataType.sessionType
            )

            Timber.d("AnalyserRepo: Step 4 Strategy for ${group.packageName} -> Input: ${group.entities.size}, Selected: ${selectableEntities.size}")

            if (selectableEntities.isEmpty()) {
                Timber.w("AnalyserRepo: WARNING! ${group.packageName} has 0 entities after selection!")
            }

            val baseEntity = group.entities.firstOrNull { it is AppEntity.BaseEntity } as? AppEntity.BaseEntity

            // Execute the signature check.
            val signatureStatus = PackagePreprocessor.checkSignature(
                baseEntity,
                group.installedInfo
            )

            // Execute the identity check.
            val identityStatus = PackagePreprocessor.checkPackageIdentity(
                baseEntity,
                group.installedInfo,
                sessionDataType.sessionType
            )

            PackageAnalysisResult(
                packageName = group.packageName,
                appEntities = selectableEntities,
                installedAppInfo = group.installedInfo,
                signatureMatchStatus = signatureStatus,
                identityStatus = identityStatus,
                sessionMode = detectedMode
            )
        }

        Timber.d("AnalyserRepo: Final Result Count -> ${finalResults.size}")

        return@coroutineScope finalResults
    }

    private suspend fun analyzeSingleSource(
        config: ConfigModel,
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