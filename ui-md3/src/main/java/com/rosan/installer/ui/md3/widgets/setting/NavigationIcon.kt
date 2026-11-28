// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.md3.widgets.setting

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A standardized back button for navigation.
 *
 * This composable encapsulates the specific styling for the navigation icon,
 * making it reusable across different screens.
 *
 * @param modifier The Modifier to be applied to this button.
 * @param icon The vector asset to be displayed inside the button.
 * @param containerColor The background color of the button.
 * @param contentColor The color of the icon inside the button.
 * @param contentDescription The content description for accessibility.
 * @param onClick The lambda to be executed when the button is clicked.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveBackButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        // Consistent shapes for the button.
        shapes = IconButtonDefaults.shapes(
            shape = CircleShape,
        ),
        // Consistent colors for the button.
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
        )
    }
}
