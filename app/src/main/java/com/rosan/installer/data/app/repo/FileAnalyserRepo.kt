package com.rosan.installer.data.app.repo

import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity

/**
 * Defines the contract for a "worker" analyser.
 * Its responsibility is to parse a specific file type (like APK, APKS)
 * and return a list of entities found within that file.
 */
interface FileAnalyserRepo {
    suspend fun doWork(
        config: ConfigEntity,
        data: List<DataEntity>,
        extra: AnalyseExtraEntity
    ): List<AppEntity>
}