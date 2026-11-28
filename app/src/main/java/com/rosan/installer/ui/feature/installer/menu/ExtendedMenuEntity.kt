package com.rosan.installer.ui.feature.installer.menu

import androidx.compose.ui.graphics.vector.ImageVector
import com.rosan.installer.domain.engine.model.install.InstallOption

data class ExtendedMenuEntity(
    val selected: Boolean = false,
    val action: InstallExtendedMenuAction,
    val menuItem: ExtendedMenuItemEntity,
    val subMenuId: InstallExtendedSubMenuId? = null,
)

data class ExtendedMenuItemEntity(
    val nameResourceId: Int,
    val description: String? = null,
    val descriptionResourceId: Int? = null,
    val icon: ImageVector? = null,
    val action: InstallOption? = null,
    // val subMenu: List<ExtendedMenuEntity>? = null
)
