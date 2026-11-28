// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.widget.card

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.rosan.installer.domain.settings.model.preferences.theme.PaletteStyle
import com.rosan.installer.domain.settings.model.preferences.theme.ThemeColorSpec
import com.rosan.installer.ui.color.RawColor
import com.rosan.installer.ui.feature.settings.preferred.theme.rememberPreviewColorScheme
import com.rosan.installer.ui.md3.widgets.card.ColorSwatch
import com.rosan.installer.ui.util.getDisplayName

@Composable
fun ColorSwatchPreview(
    rawColor: RawColor,
    currentStyle: PaletteStyle,
    colorSpec: ThemeColorSpec,
    isSelected: Boolean,
    textStyle: TextStyle,
    textColor: Color,
    onClick: () -> Unit,
) {
    val scheme = rememberPreviewColorScheme(rawColor, currentStyle, colorSpec)
    val label = rawColor.getDisplayName()
    ColorSwatch(
        baseColor = rawColor.color,
        scheme = scheme,
        isSelected = isSelected,
        label = label.takeUnless { it === rawColor.key },
        textStyle = textStyle,
        textColor = textColor,
        onClick = onClick,
    )
}
