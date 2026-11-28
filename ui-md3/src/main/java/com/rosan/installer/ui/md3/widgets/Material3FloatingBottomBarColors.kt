// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.md3.widgets

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.rosan.installer.ui.effects.FloatingBottomBarColors

@Composable
fun material3FloatingBottomBarColors(
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    activeContentColor: Color = indicatorColor,
): FloatingBottomBarColors = FloatingBottomBarColors(containerColor, indicatorColor, contentColor, activeContentColor)
