package com.rosan.installer.ui.page.settings.config.edit

import com.rosan.installer.data.settings.model.room.entity.ConfigEntity

sealed class EditViewAction {
    object Init : EditViewAction()
    data class ChangeDataName(val name: String) : EditViewAction()
    data class ChangeDataDescription(val description: String) : EditViewAction()
    data class ChangeDataAuthorizer(val authorizer: ConfigEntity.Authorizer) : EditViewAction()
    data class ChangeDataCustomizeAuthorizer(val customizeAuthorizer: String) : EditViewAction()
    data class ChangeDataInstallMode(val installMode: ConfigEntity.InstallMode) : EditViewAction()
    data class ChangeDataDeclareInstaller(val declareInstaller: Boolean) : EditViewAction()
    data class ChangeDataInstaller(val installer: String) : EditViewAction()
    data class ChangeDataAutoDelete(val autoDelete: Boolean) : EditViewAction()
    data class ChangeDisplaySdk(val displaySdk: Boolean) : EditViewAction()
    data class ChangeDataForAllUser(val forAllUser: Boolean) : EditViewAction()
    data class ChangeDataAllowTestOnly(val allowTestOnly: Boolean) : EditViewAction()
    data class ChangeDataAllowDowngrade(val allowDowngrade: Boolean) : EditViewAction()
    data class ChangeDataAllowRestrictedPermissions(val allowRestrictedPermissions: Boolean) : EditViewAction()
    data class ChangeDataBypassLowTargetSdk(val bypassLowTargetSdk: Boolean) : EditViewAction()
    data class ChangeDataAllowAllRequestedPermissions(val allowAllRequestedPermissions: Boolean) : EditViewAction()

    object LoadData : EditViewAction()
    object SaveData : EditViewAction()
}
