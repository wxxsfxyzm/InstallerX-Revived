package com.rosan.installer.data.updater.repo

import com.rosan.installer.data.settings.model.room.entity.ConfigEntity

interface AppUpdater {
    suspend fun performInAppUpdate(url: String, config: ConfigEntity)
}