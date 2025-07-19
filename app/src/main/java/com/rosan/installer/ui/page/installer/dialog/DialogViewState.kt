package com.rosan.installer.ui.page.installer.dialog

import com.rosan.installer.data.installer.model.entity.InstallResult

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

    // 用于显示批量安装的最终结果
    data class InstallCompleted(val results: List<InstallResult>) : DialogViewState()
}