package com.rosan.installer.ui.page.settings.preferred

import com.rosan.installer.data.settings.model.room.entity.ConfigEntity

data class PreferredViewState(
    val authorizer: ConfigEntity.Authorizer = ConfigEntity.Authorizer.Shizuku,
    val customizeAuthorizer: String = "",
    val installMode: ConfigEntity.InstallMode = ConfigEntity.InstallMode.Dialog,
    val showDialogInstallExtendedMenu: Boolean = false,
    val showIntelligentSuggestion: Boolean = false,
    val disableNotificationForDialogInstall: Boolean = false,
    val showDialogWhenPressingNotification: Boolean = true,
    val dhizukuAutoCloseCountDown: Int = 3
) {
    val authorizerCustomize = authorizer == ConfigEntity.Authorizer.Customize
}