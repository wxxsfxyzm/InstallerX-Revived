package com.rosan.installer.data.updater.model.impl

import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.updater.repo.UpdateChecker
import timber.log.Timber

class OfflineUpdateChecker : UpdateChecker {
    override fun check(): UpdateChecker.CheckResult? {
        Timber.d("Update check disabled: Offline build")
        return null
    }

    override fun download(url: String): DataEntity? {
        Timber.w("Download is not supported in Offline build")
        return null
    }
}