package com.rosan.installer.data.installer.model.entity

import androidx.compose.ui.graphics.vector.ImageVector
import com.rosan.installer.data.app.util.InstallOption
import com.rosan.installer.ui.page.installer.dialog.inner.InstallExtendedMenuAction
import com.rosan.installer.ui.page.installer.dialog.inner.InstallExtendedSubMenuId

data class ExtendedMenuEntity(
    val selected: Boolean = false,
    val action: InstallExtendedMenuAction,
    val menuItem: ExtendedMenuItemEntity,
    val subMenuId: InstallExtendedSubMenuId? = null
)

data class ExtendedMenuItemEntity(
    val nameResourceId: Int,
    val description: String? = null,
    val descriptionResourceId: Int? = null,
    val icon: ImageVector? = null,
    val action: InstallOption? = null,
    // val subMenu: List<ExtendedMenuEntity>? = null
)