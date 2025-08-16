package com.rosan.installer.ui.page.settings.preferred

import com.rosan.installer.data.settings.model.datastore.entity.NamedPackage
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity

data class PreferredViewState(
    val progress: Progress = Progress.Loading,
    val authorizer: ConfigEntity.Authorizer = ConfigEntity.Authorizer.Shizuku,
    val customizeAuthorizer: String = "",
    val installMode: ConfigEntity.InstallMode = ConfigEntity.InstallMode.Dialog,
    val adbVerifyEnabled: Boolean = true,
    val showDialogInstallExtendedMenu: Boolean = false,
    val showIntelligentSuggestion: Boolean = false,
    val disableNotificationForDialogInstall: Boolean = false,
    val showDialogWhenPressingNotification: Boolean = true,
    val dhizukuAutoCloseCountDown: Int = 3,
    val showRefreshedUI: Boolean = true,
    val managedPackages: List<NamedPackage> = emptyList()
) {
    val authorizerCustomize = authorizer == ConfigEntity.Authorizer.Customize

    sealed class Progress {
        object Loading : Progress()
        object Loaded : Progress()
    }
}