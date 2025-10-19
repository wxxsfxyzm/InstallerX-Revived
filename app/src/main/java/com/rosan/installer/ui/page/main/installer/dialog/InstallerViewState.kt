package com.rosan.installer.ui.page.main.installer.dialog

import com.rosan.installer.data.installer.model.entity.InstallResult

sealed class InstallerViewState {
    data object Ready : InstallerViewState()

    data object Resolving : InstallerViewState()
    data object ResolveFailed : InstallerViewState()

    // The new state for caching files, now with progress.
    data class Preparing(val progress: Float) : InstallerViewState()

    data object Analysing : InstallerViewState()
    data object AnalyseFailed : InstallerViewState()

    data object InstallChoice : InstallerViewState()
    data object InstallPrepare : InstallerViewState()
    data object InstallExtendedMenu : InstallerViewState()
    data object InstallExtendedSubMenu : InstallerViewState()
    data object Installing : InstallerViewState()
    data object InstallSuccess : InstallerViewState()
    data object InstallFailed : InstallerViewState()
    data object InstallRetryDowngradeUsingUninstall : InstallerViewState()
    data class InstallCompleted(val results: List<InstallResult>) : InstallerViewState()

    data object UninstallReady : InstallerViewState()
    data object UninstallResolveFailed : InstallerViewState()
    data object Uninstalling : InstallerViewState()
    data object UninstallSuccess : InstallerViewState()
    data object UninstallFailed : InstallerViewState()
}