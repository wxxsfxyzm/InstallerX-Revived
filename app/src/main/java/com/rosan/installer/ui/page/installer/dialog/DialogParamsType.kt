package com.rosan.installer.ui.page.installer.dialog

sealed class DialogParamsType(val id: String) {
    object IconWorking : DialogParamsType("icon_working")
    object IconPausing : DialogParamsType("icon_pausing")
    object IconMenu : DialogParamsType("icon_menu")

    object ButtonsCancel : DialogParamsType("buttons_cancel")

    object InstallerReady : DialogParamsType("installer_ready")
    object InstallerResolving : DialogParamsType("installer_resolving")
    object InstallerResolveFailed : DialogParamsType("installer_resolve_failed")
    object InstallerAnalysing : DialogParamsType("installer_analysing")
    object InstallerAnalyseFailed : DialogParamsType("installer_analyse_failed")
    object InstallChoice : DialogParamsType("installer_choice")
    object InstallExtendedMenu : DialogParamsType("installer_extended_menu")
    object InstallerPrepare : DialogParamsType("installer_prepare")
    object InstallerPrepareEmpty : DialogParamsType("installer_prepare_empty")
    object InstallerPrepareTooMany : DialogParamsType("installer_prepare_too_many")
    object InstallerInfo :
        DialogParamsType("installer_info")

    object InstallerPrepareInstall :
        DialogParamsType("installer_prepare")

    object InstallerInstalling : DialogParamsType("installer_installing")

    object InstallerInstallSuccess : DialogParamsType("install_success")
    object InstallerInstallFailed : DialogParamsType("install_failed")
}