package com.rosan.installer.ui.page.main.installer

data class InstallerViewSettings(
    val uiExpressive: Boolean = true,
    val preferSystemIconForUpdates: Boolean = false,
    val autoCloseCountDown: Int = 3,
    val showExtendedMenu: Boolean = false,
    val showSmartSuggestion: Boolean = true,
    val disableNotificationOnDismiss: Boolean = false,
    val versionCompareInSingleLine: Boolean = false,
    val sdkCompareInMultiLine: Boolean = false,
    val showOPPOSpecial: Boolean = false,
    val autoSilentInstall: Boolean = false,
    val enableModuleInstall: Boolean = false,
    val useDynColorFollowPkgIcon: Boolean = false
)