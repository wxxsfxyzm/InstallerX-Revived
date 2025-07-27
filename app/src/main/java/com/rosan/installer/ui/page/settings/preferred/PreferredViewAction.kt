package com.rosan.installer.ui.page.settings.preferred

import com.rosan.installer.data.settings.model.room.entity.ConfigEntity

sealed class PreferredViewAction {
    object Init : PreferredViewAction()
    data class ChangeGlobalAuthorizer(val authorizer: ConfigEntity.Authorizer) :
        PreferredViewAction()

    data class ChangeGlobalCustomizeAuthorizer(val customizeAuthorizer: String) :
        PreferredViewAction()

    data class ChangeGlobalInstallMode(val installMode: ConfigEntity.InstallMode) :
        PreferredViewAction()

    data class ChangeShowDialogInstallExtendedMenu(val showMenu: Boolean) :
        PreferredViewAction()

    data class ChangeShowIntelligentSuggestion(val showIntelligentSuggestion: Boolean) :
        PreferredViewAction()

    data class ChangeShowDisableNotificationForDialogInstall(val showDisableNotification: Boolean) :
        PreferredViewAction()

    data class ChangeShowDialogWhenPressingNotification(val showDialog: Boolean) :
        PreferredViewAction()

    data class ChangeDhizukuAutoCloseCountDown(val countDown: Int) :
        PreferredViewAction()
}