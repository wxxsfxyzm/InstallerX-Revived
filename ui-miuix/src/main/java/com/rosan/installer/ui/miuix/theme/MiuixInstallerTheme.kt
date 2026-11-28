// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.miuix.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.rosan.installer.ui.color.ColorPaletteStyle
import com.rosan.installer.ui.color.ColorSpecVersion
import com.rosan.installer.ui.color.ColorThemeConfig
import com.rosan.installer.ui.color.ColorThemeMode
import com.rosan.installer.ui.color.rememberKeyColor
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec as MiuixColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle as MiuixPaletteStyle

/**
 * Create a Miuix theme controller for [config].
 *
 * @param useMonet Whether Miuix generates its palette from the key color. When off, Miuix uses its default colors.
 */
@Composable
fun rememberMiuixThemeController(config: ColorThemeConfig, useMonet: Boolean): ThemeController {
    val isDark = config.isDark
    return if (useMonet) {
        val keyColor = config.rememberKeyColor()
        val colorSchemeMode = when (config.mode) {
            ColorThemeMode.System -> ColorSchemeMode.MonetSystem
            ColorThemeMode.Light -> ColorSchemeMode.MonetLight
            ColorThemeMode.Dark -> ColorSchemeMode.MonetDark
        }
        val style = config.paletteStyle.toMiuix()
        // Pass the saved preference; Miuix resolves palette support and forces SPEC_2026 for Cmf.
        // SPEC_2026 is only an effective spec in our UI, so fall back to the default preference.
        val preferredColorSpec = when (config.preferredSpec) {
            ColorSpecVersion.Spec2021 -> MiuixColorSpec.Spec2021
            else -> MiuixColorSpec.Spec2025
        }

        remember(colorSchemeMode, keyColor, style, preferredColorSpec, isDark) {
            ThemeController(
                colorSchemeMode = colorSchemeMode,
                keyColor = keyColor,
                paletteStyle = style,
                colorSpec = preferredColorSpec,
                isDark = isDark,
            )
        }
    } else {
        val colorSchemeMode = when (config.mode) {
            ColorThemeMode.System -> ColorSchemeMode.System
            ColorThemeMode.Light -> ColorSchemeMode.Light
            ColorThemeMode.Dark -> ColorSchemeMode.Dark
        }

        remember(colorSchemeMode, isDark) {
            ThemeController(
                colorSchemeMode = colorSchemeMode,
                isDark = isDark,
            )
        }
    }
}

/**
 * Apply the Miuix theme from [controller].
 *
 * @param useAmoledSurface Whether to replace the surface color with pure black.
 */
@Composable
fun MiuixInstallerTheme(
    controller: ThemeController,
    useAmoledSurface: Boolean = false,
    content: @Composable () -> Unit,
) {
    MiuixTheme(controller = controller) {
        val colors = MiuixTheme.colorScheme
        // Keep the controller's dynamic-color mode and the card palette intact.
        MiuixTheme(colors = if (useAmoledSurface) colors.copy(surface = Color.Black) else colors, content = content)
    }
}

private fun ColorPaletteStyle.toMiuix(): MiuixPaletteStyle = when (this) {
    ColorPaletteStyle.TonalSpot -> MiuixPaletteStyle.TonalSpot
    ColorPaletteStyle.Neutral -> MiuixPaletteStyle.Neutral
    ColorPaletteStyle.Vibrant -> MiuixPaletteStyle.Vibrant
    ColorPaletteStyle.Expressive -> MiuixPaletteStyle.Expressive
    ColorPaletteStyle.Rainbow -> MiuixPaletteStyle.Rainbow
    ColorPaletteStyle.FruitSalad -> MiuixPaletteStyle.FruitSalad
    ColorPaletteStyle.Monochrome -> MiuixPaletteStyle.Monochrome
    ColorPaletteStyle.Fidelity -> MiuixPaletteStyle.Fidelity
    ColorPaletteStyle.Content -> MiuixPaletteStyle.Content
    ColorPaletteStyle.Cmf -> MiuixPaletteStyle.Cmf
}
