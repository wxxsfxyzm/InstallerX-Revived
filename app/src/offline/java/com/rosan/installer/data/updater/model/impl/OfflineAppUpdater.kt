package com.rosan.installer.data.updater.model.impl

import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.data.updater.repo.AppUpdater
import timber.log.Timber

class OfflineAppUpdater : AppUpdater {
    override suspend fun performInAppUpdate(url: String, config: ConfigEntity) {
        Timber.d("Offline build: performInAppUpdate called but ignored.")
    }
}