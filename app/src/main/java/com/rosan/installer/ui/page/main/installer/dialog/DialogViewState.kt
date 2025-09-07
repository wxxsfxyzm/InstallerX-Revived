package com.rosan.installer.ui.page.main.installer.dialog

import com.rosan.installer.data.installer.model.entity.InstallResult

sealed class DialogViewState {
    data object Ready : DialogViewState()

    data object Resolving : DialogViewState()
    data object ResolveFailed : DialogViewState()

    // The new state for caching files, now with progress.
    data class Preparing(val progress: Float) : DialogViewState()

    data object Analysing : DialogViewState()
    data object AnalyseFailed : DialogViewState()

    data object InstallChoice : DialogViewState()
    data object InstallPrepare : DialogViewState()
    data object InstallExtendedMenu : DialogViewState()
    data object InstallExtendedSubMenu : DialogViewState()
    data object Installing : DialogViewState()
    data object InstallSuccess : DialogViewState()
    data object InstallFailed : DialogViewState()
    data object InstallRetryDowngradeUsingUninstall : DialogViewState()
    data class InstallCompleted(val results: List<InstallResult>) : DialogViewState()

    data object UninstallReady : DialogViewState()
    data object UninstallResolveFailed : DialogViewState()
    data object Uninstalling : DialogViewState()
    data object UninstallSuccess : DialogViewState()
    data object UninstallFailed : DialogViewState()
}