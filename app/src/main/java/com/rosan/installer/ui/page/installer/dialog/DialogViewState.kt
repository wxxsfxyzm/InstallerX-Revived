package com.rosan.installer.ui.page.installer.dialog

sealed class DialogViewState {
    object Ready : DialogViewState()
    object Resolving : DialogViewState()
    object ResolveFailed : DialogViewState()
    object Analysing : DialogViewState()
    object AnalyseFailed : DialogViewState()
    object InstallChoice : DialogViewState()
    object InstallPrepare : DialogViewState()
    object InstallExtendedMenu : DialogViewState()
    object InstallExtendedSubMenu : DialogViewState()
    object Installing : DialogViewState()
    object InstallFailed : DialogViewState()
    object InstallSuccess : DialogViewState()
}