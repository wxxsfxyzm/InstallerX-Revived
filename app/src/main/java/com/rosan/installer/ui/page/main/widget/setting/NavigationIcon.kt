// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.widget.setting

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.library.widgets.material3.ExpressiveBackButton as LibraryExpressiveBackButton

@Composable
fun ExpressiveBackButton(
    modifier: Modifier = Modifier,
    icon: ImageVector = AppIcons.ArrowBack,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    contentDescription: String = stringResource(id = R.string.back),
    onClick: () -> Unit,
) {
    LibraryExpressiveBackButton(
        modifier = modifier,
        icon = icon,
        containerColor = containerColor,
        contentColor = contentColor,
        contentDescription = contentDescription,
        onClick = onClick,
    )
}
