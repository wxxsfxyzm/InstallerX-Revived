// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.miuix.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.rosan.installer.ui.effects.FloatingBottomBarColors
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun miuixFloatingBottomBarColors(
    containerColor: Color = MiuixTheme.colorScheme.surfaceContainer,
    indicatorColor: Color = MiuixTheme.colorScheme.primary,
    contentColor: Color = MiuixTheme.colorScheme.onSurface,
    activeContentColor: Color = indicatorColor,
): FloatingBottomBarColors = FloatingBottomBarColors(containerColor, indicatorColor, contentColor, activeContentColor)
