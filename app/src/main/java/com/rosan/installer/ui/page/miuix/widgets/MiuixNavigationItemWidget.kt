// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.widgets

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.preference.ArrowPreference

/**
 * A setting pkg that navigates to a secondary page, built upon BaseWidget.
 * It includes an icon, title, description, and a trailing arrow.
 *
 * @param icon The leading icon for the pkg.
 * @param title The main title text of the pkg.
 * @param description The supporting description text.
 * @param onClick The callback to be invoked when this pkg is clicked.
 */
@Composable
fun MiuixNavigationItemWidget(
    icon: ImageVector? = null,
    title: String,
    description: String,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    onClick: () -> Unit
) {
    // Call the BaseWidget and pass the parameters accordingly.
    ArrowPreference(
        title = title,
        summary = description,
        insideMargin = insideMargin,
        onClick = onClick
    )
}