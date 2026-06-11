// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.isDynamicColor

@Composable
fun miuixSheetCardColor(): Color =
    if (isDynamicColor) {
        MiuixTheme.colorScheme.surfaceContainer
    } else if (InstallerTheme.isDark) {
        miuixSheetCardColorDark
    } else {
        Color.White
    }

@Composable
fun miuixSheetCardColors(
    contentColor: Color = MiuixTheme.colorScheme.onSurface
): CardColors =
    CardColors(
        color = miuixSheetCardColor(),
        contentColor = contentColor
    )
