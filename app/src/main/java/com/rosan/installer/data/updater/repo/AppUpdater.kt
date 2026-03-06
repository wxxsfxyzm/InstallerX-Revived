package com.rosan.installer.data.updater.repo

import com.rosan.installer.domain.settings.model.ConfigModel

interface AppUpdater {
    suspend fun performInAppUpdate(url: String, config: ConfigModel)
}