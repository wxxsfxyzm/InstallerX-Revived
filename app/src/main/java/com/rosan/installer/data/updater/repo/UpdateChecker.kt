package com.rosan.installer.data.updater.repo

import com.rosan.installer.data.app.model.entity.DataEntity

interface UpdateChecker {
    data class CheckResult(
        val hasUpdate: Boolean,
        val remoteVersion: String,
        val releaseUrl: String,
        val downloadUrl: String
    )

    /**
     * Check for updates
     * @return CheckResult if update available, null otherwise
     */
    fun check(): CheckResult?

    /**
     * Download the update
     * @param url The url to download from
     */
    fun download(url: String): DataEntity?
}