package com.rosan.installer.ui.page.installer.dialog.inner

sealed class InstallExtendedMenuAction {
    object SubMenu : InstallExtendedMenuAction()
    object InstallOption : InstallExtendedMenuAction()
    object TextField : InstallExtendedMenuAction()
}

sealed class InstallExtendedSubMenuId(val id: String) {
    data object PermissionList : InstallExtendedSubMenuId("permission_list")
}
