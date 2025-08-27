package com.rosan.installer.ui.page.installer.dialog

sealed class DialogParamsType(val id: String) {
    data object IconWorking : DialogParamsType("icon_working")
    data object IconPausing : DialogParamsType("icon_pausing")
    data object IconMenu : DialogParamsType("icon_menu")

    data object ButtonsCancel : DialogParamsType("buttons_cancel")

    data object InstallerReady : DialogParamsType("installer_ready")
    data object InstallerResolving : DialogParamsType("installer_resolving")
    data object InstallerResolveFailed : DialogParamsType("installer_resolve_failed")
    data object InstallerPreparing : DialogParamsType("installer_preparing")
    data object InstallerAnalysing : DialogParamsType("installer_analysing")
    data object InstallerAnalyseFailed : DialogParamsType("installer_analyse_failed")
    data object InstallChoice : DialogParamsType("installer_choice")
    data object InstallExtendedMenu : DialogParamsType("installer_extended_menu")
    data object InstallExtendedSubMenu : DialogParamsType("install_extended_sub_menu")
    data object InstallerPrepare : DialogParamsType("installer_prepare")
    data object InstallerPrepareEmpty : DialogParamsType("installer_prepare_empty")
    data object InstallerPrepareTooMany : DialogParamsType("installer_prepare_too_many")
    data object InstallerInfo : DialogParamsType("installer_info")
    data object InstallerPrepareInstall : DialogParamsType("installer_prepare")
    data object InstallerInstalling : DialogParamsType("installer_installing")
    data object InstallerInstallSuccess : DialogParamsType("install_success")
    data object InstallerInstallFailed : DialogParamsType("install_failed")
    data object InstallerInstallCompleted : DialogParamsType("install_completed")
    data object InstallerUninstallInfo : DialogParamsType("uninstaller_info")
    data object InstallerUninstallReady : DialogParamsType("uninstaller_ready")
    data object InstallerUninstallSuccess : DialogParamsType("uninstaller_uninstall_success")
    data object InstallerUninstallFailed : DialogParamsType("uninstaller_uninstall_failed")
}