package com.rosan.installer.ui.page.installer.dialog

import com.rosan.installer.data.installer.model.entity.InstallResult

sealed class DialogViewState {
    data object Ready : DialogViewState()
    data object Resolving : DialogViewState()
    data object ResolveFailed : DialogViewState()
    data object Analysing : DialogViewState()
    data object AnalyseFailed : DialogViewState()
    data object InstallChoice : DialogViewState()
    data object InstallPrepare : DialogViewState()
    data object InstallExtendedMenu : DialogViewState()
    data object InstallExtendedSubMenu : DialogViewState()
    data object Installing : DialogViewState()
    data object InstallFailed : DialogViewState()
    data object InstallSuccess : DialogViewState()
    data object Uninstalling : DialogViewState()

    // 用于显示批量安装的最终结果
    data class InstallCompleted(val results: List<InstallResult>) : DialogViewState()
}