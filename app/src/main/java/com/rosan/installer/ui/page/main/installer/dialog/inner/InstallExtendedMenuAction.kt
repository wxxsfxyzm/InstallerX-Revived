package com.rosan.installer.ui.page.main.installer.dialog.inner

sealed class InstallExtendedMenuAction {
    data object PermissionList : InstallExtendedMenuAction()
    data object CustomizeRequester : InstallExtendedMenuAction()
    data object CustomizeInstaller : InstallExtendedMenuAction()
    data object CustomizeUser : InstallExtendedMenuAction()
    data object InstallOption : InstallExtendedMenuAction()
    data object TextField : InstallExtendedMenuAction()
}

sealed class InstallExtendedSubMenuId(val id: String) {
    data object PermissionList : InstallExtendedSubMenuId("permission_list")
}
