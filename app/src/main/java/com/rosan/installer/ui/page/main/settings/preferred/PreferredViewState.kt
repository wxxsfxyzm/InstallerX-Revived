package com.rosan.installer.ui.page.main.settings.preferred

import com.rosan.installer.data.settings.model.datastore.entity.NamedPackage
import com.rosan.installer.data.settings.model.datastore.entity.SharedUid
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity

data class PreferredViewState(
    val progress: Progress = Progress.Loading,
    val authorizer: ConfigEntity.Authorizer = ConfigEntity.Authorizer.Shizuku,
    val customizeAuthorizer: String = "",
    val installMode: ConfigEntity.InstallMode = ConfigEntity.InstallMode.Dialog,
    val adbVerifyEnabled: Boolean = true,
    val isIgnoringBatteryOptimizations: Boolean = false,
    val showDialogInstallExtendedMenu: Boolean = false,
    val showSmartSuggestion: Boolean = false,
    val disableNotificationForDialogInstall: Boolean = false,
    val showDialogWhenPressingNotification: Boolean = true,
    val dhizukuAutoCloseCountDown: Int = 3,
    val versionCompareInSingleLine: Boolean = false,
    val showExpressiveUI: Boolean = true,
    val showLiveActivity: Boolean = false,
    val showMiuixUI: Boolean = false,
    val preferSystemIcon: Boolean = false,
    val showLauncherIcon: Boolean = true,
    val managedInstallerPackages: List<NamedPackage> = emptyList(),
    val managedBlacklistPackages: List<NamedPackage> = emptyList(),
    val managedSharedUserIdBlacklist: List<SharedUid> = emptyList(),
    val managedSharedUserIdExemptedPackages: List<NamedPackage> = emptyList(),
) {
    val authorizerCustomize = authorizer == ConfigEntity.Authorizer.Customize

    sealed class Progress {
        data object Loading : Progress()
        data object Loaded : Progress()
    }
}