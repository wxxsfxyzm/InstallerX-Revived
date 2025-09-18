package com.rosan.installer.ui.page.miuix.settings.preferred.subpage.home

import androidx.compose.ui.graphics.vector.ImageVector

data class HomeCardItem(
    val icon: ImageVector? = null,
    val label: String,
    val content: String? = null,
    val onClick: (() -> Unit)? = null
)