package com.rosan.installer.data.installer.model.entity

import androidx.compose.ui.graphics.vector.ImageVector
import com.rosan.installer.ui.page.installer.dialog.inner.InstallExtendedMenuAction
import com.rosan.installer.ui.page.installer.dialog.inner.InstallExtendedSubMenuId

data class ExtendedMenuEntity(
    val selected: Boolean = false,
    val action: InstallExtendedMenuAction,
    val menuItem: ExtendedMenuItemEntity,
    val subMenuId: InstallExtendedSubMenuId? = null
)

data class ExtendedMenuItemEntity(
    val name: String,
    val description: String? = null,
    val icon: ImageVector? = null,
    val action: String? = null,
    // val subMenu: List<ExtendedMenuEntity>? = null
)