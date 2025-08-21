package com.rosan.installer.data.installer.model.entity

sealed class ProgressEntity {
    data object Finish : ProgressEntity()

    /**
     * The new state for caching files, now with progress.
     * @param progress A value from 0.0f to 1.0f. A value of -1.0f can indicate an indeterminate progress.
     */
    data class Preparing(val progress: Float) : ProgressEntity()
    data object Ready : ProgressEntity()
    data object Error : ProgressEntity()

    data object Resolving : ProgressEntity()
    data object ResolvedFailed : ProgressEntity()
    data object ResolveSuccess : ProgressEntity()

    data object Analysing : ProgressEntity()
    data object AnalysedFailed : ProgressEntity()
    data class AnalysedUnsupported(val reason: String) : ProgressEntity()
    data object AnalysedSuccess : ProgressEntity()

    data object Installing : ProgressEntity()
    data object InstallFailed : ProgressEntity()
    data object InstallSuccess : ProgressEntity()

    data object Uninstalling : ProgressEntity()
    data object UninstallSuccess : ProgressEntity()
    data object UninstallFailed : ProgressEntity()
}