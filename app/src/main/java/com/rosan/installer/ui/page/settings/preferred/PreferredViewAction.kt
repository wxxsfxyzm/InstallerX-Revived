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
}