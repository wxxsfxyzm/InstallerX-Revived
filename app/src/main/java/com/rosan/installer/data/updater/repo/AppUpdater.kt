package com.rosan.installer.data.updater.repo

import com.rosan.installer.data.settings.local.room.entity.ConfigEntity

interface AppUpdater {
    suspend fun performInAppUpdate(url: String, config: ConfigEntity)
}