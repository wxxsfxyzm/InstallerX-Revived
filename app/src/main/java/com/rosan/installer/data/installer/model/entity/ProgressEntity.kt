package com.rosan.installer.data.installer.model.entity

sealed class ProgressEntity {
    data object Ready : ProgressEntity()
    data object Error : ProgressEntity()
    data object Finish : ProgressEntity()

    data object InstallResolving : ProgressEntity()
    data object InstallResolvedFailed : ProgressEntity()
    data object InstallResolveSuccess : ProgressEntity()

    /**
     * The new state for caching files, now with progress.
     * @param progress A value from 0.0f to 1.0f. A value of -1.0f can indicate an indeterminate progress.
     */
    data class InstallPreparing(val progress: Float) : ProgressEntity()

    data object InstallAnalysing : ProgressEntity()
    data object InstallAnalysedFailed : ProgressEntity()
    data class InstallAnalysedUnsupported(val reason: String) : ProgressEntity()
    data object InstallAnalysedSuccess : ProgressEntity()

    data class Installing(val current: Int = 1, val total: Int = 1, val appLabel: String? = null) : ProgressEntity()
    data class InstallCompleted(val results: List<InstallResult>) : ProgressEntity()
    data object InstallConfirming : ProgressEntity()
    data class InstallingModule(val output: List<String>) : ProgressEntity()
    data object InstallFailed : ProgressEntity()
    data object InstallSuccess : ProgressEntity()

    data object UninstallResolving : ProgressEntity()
    data object UninstallResolveFailed : ProgressEntity()
    data object UninstallReady : ProgressEntity()

    data object Uninstalling : ProgressEntity()
    data object UninstallSuccess : ProgressEntity()
    data object UninstallFailed : ProgressEntity()
}