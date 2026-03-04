// InstallerTheme.kt
// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.theme

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.core.view.WindowCompat
import com.rosan.installer.ui.theme.material.PaletteStyle
import com.rosan.installer.ui.theme.material.ThemeColorSpec
import com.rosan.installer.ui.theme.material.ThemeMode
import com.rosan.installer.ui.theme.material.animateAsState
import com.rosan.installer.ui.theme.material.dynamicColorScheme
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemeColorSpec as MiuixColorSpec
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle as MiuixPaletteStyle

private val LocalIsDark = staticCompositionLocalOf { false }
private val LocalPaletteStyle = staticCompositionLocalOf { PaletteStyle.Expressive }
private val LocalThemeColorSpec = staticCompositionLocalOf { ThemeColorSpec.SPEC_2025 }
private val LocalSeedColor = staticCompositionLocalOf { Color.Unspecified }
private val LocalThemeMode = staticCompositionLocalOf { ThemeMode.SYSTEM }
private val LocalUseMiuixMonet = staticCompositionLocalOf { false }
private val LocalUseDynamicColor = staticCompositionLocalOf { false }
private val LocalIsExpressive = staticCompositionLocalOf { true }

val LocalInstallerColorScheme = staticCompositionLocalOf<ColorScheme> { error("No ColorScheme provided") }

object InstallerTheme {
    val colorScheme: ColorScheme
        @Composable @ReadOnlyComposable get() = LocalInstallerColorScheme.current

    val isDark: Boolean
        @Composable @ReadOnlyComposable get() = LocalIsDark.current

    val seedColor: Color
        @Composable @ReadOnlyComposable get() = LocalSeedColor.current

    val paletteStyle: PaletteStyle
        @Composable @ReadOnlyComposable get() = LocalPaletteStyle.current

    val colorSpec: ThemeColorSpec
        @Composable @ReadOnlyComposable get() = LocalThemeColorSpec.current

    val themeMode: ThemeMode
        @Composable @ReadOnlyComposable get() = LocalThemeMode.current

    val useMiuixMonet: Boolean
        @Composable @ReadOnlyComposable get() = LocalUseMiuixMonet.current

    val useDynamicColor: Boolean
        @Composable @ReadOnlyComposable get() = LocalUseDynamicColor.current

    val isExpressive: Boolean
        @Composable @ReadOnlyComposable get() = LocalIsExpressive.current
}

@Composable
fun InstallerTheme(
    useMiuix: Boolean,
    isExpressive: Boolean, // Added explicit parameter to drive standard vs expressive branching
    themeMode: ThemeMode,
    paletteStyle: PaletteStyle,
    colorSpec: ThemeColorSpec,
    useDynamicColor: Boolean,
    useMiuixMonet: Boolean,
    seedColor: Color,
    content: @Composable () -> Unit
) {
    val preservedContent = remember {
        movableContentOf<@Composable () -> Unit> { targetContent ->
            targetContent()
        }
    }

    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val keyColor = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        colorResource(id = android.R.color.system_accent1_500)
    else seedColor

    // 1. Generate the base scheme with spec support
    val baseColorScheme = remember(keyColor, isDark, paletteStyle, colorSpec) {
        dynamicColorScheme(
            keyColor = keyColor,
            isDark = isDark,
            style = paletteStyle,
            colorSpec = colorSpec
        )
    }

    // 2. Wrap it with smooth transitions
    val animatedColorScheme = baseColorScheme.animateAsState()

    CompositionLocalProvider(
        LocalIsDark provides isDark,
        LocalPaletteStyle provides paletteStyle,
        LocalSeedColor provides seedColor,
        LocalInstallerColorScheme provides animatedColorScheme,
        LocalThemeMode provides themeMode,
        LocalUseMiuixMonet provides useMiuixMonet,
        LocalUseDynamicColor provides useDynamicColor,
        LocalThemeColorSpec provides colorSpec,
        LocalIsExpressive provides isExpressive // Expose to the tree
    ) {
        // Strict branching for the base design system prevents standard Material Design pages
        // from being polluted by Expressive's MotionScheme or Typography.
        when {
            useMiuix -> {
                InstallerMiuixTheme(
                    darkTheme = isDark,
                    themeMode = themeMode,
                    useDynamicColor = useDynamicColor,
                    useMiuixMonet = useMiuixMonet,
                    seedColor = seedColor,
                    paletteStyle = paletteStyle,
                    colorSpec = colorSpec
                ) {
                    preservedContent(content)
                }
            }

            isExpressive -> {
                InstallerMaterialExpressiveTheme(
                    darkTheme = isDark,
                    colorScheme = animatedColorScheme
                ) {
                    preservedContent(content)
                }
            }

            else -> {
                InstallerMaterialTheme(
                    darkTheme = isDark,
                    colorScheme = animatedColorScheme
                ) {
                    preservedContent(content)
                }
            }
        }
    }
}

@Composable
fun InstallerMaterialTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorScheme: ColorScheme,
    compatStatusBarColor: Boolean = true,
    content: @Composable () -> Unit
) {
    if (compatStatusBarColor) {
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as ComponentActivity).window
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    // Uses the standard Material Design 3 theme baseline
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InstallerMaterialExpressiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorScheme: ColorScheme,
    compatStatusBarColor: Boolean = true,
    content: @Composable () -> Unit
) {
    if (compatStatusBarColor) {
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as ComponentActivity).window
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    // Applies Material 3 Expressive defaults including the expressive MotionScheme
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = Typography,
        content = content
    )
}

@Composable
fun InstallerMiuixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeMode: ThemeMode,
    useMiuixMonet: Boolean,
    useDynamicColor: Boolean = false,
    compatStatusBarColor: Boolean = true,
    seedColor: Color,
    paletteStyle: PaletteStyle,
    colorSpec: ThemeColorSpec,
    content: @Composable () -> Unit
) {
    if (compatStatusBarColor) {
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as ComponentActivity).window
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    val controller = if (useMiuixMonet) {
        // --- Monet Engine Path ---
        val keyColor = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            colorResource(id = android.R.color.system_accent1_500)
        else seedColor

        val colorSchemeMode = when (themeMode) {
            ThemeMode.SYSTEM -> ColorSchemeMode.MonetSystem
            ThemeMode.LIGHT -> ColorSchemeMode.MonetLight
            ThemeMode.DARK -> ColorSchemeMode.MonetDark
        }

        val style = when (paletteStyle) {
            PaletteStyle.TonalSpot -> MiuixPaletteStyle.TonalSpot
            PaletteStyle.Neutral -> MiuixPaletteStyle.Neutral
            PaletteStyle.Vibrant -> MiuixPaletteStyle.Vibrant
            PaletteStyle.Expressive -> MiuixPaletteStyle.Expressive
            PaletteStyle.Rainbow -> MiuixPaletteStyle.Rainbow
            PaletteStyle.FruitSalad -> MiuixPaletteStyle.FruitSalad
            PaletteStyle.Monochrome -> MiuixPaletteStyle.Monochrome
            PaletteStyle.Fidelity -> MiuixPaletteStyle.Fidelity
            PaletteStyle.Content -> MiuixPaletteStyle.Content
        }

        val colorSpecVersion = when (colorSpec) {
            ThemeColorSpec.SPEC_2025 -> if (paletteStyle.supportsSpec2025) MiuixColorSpec.Spec2025 else MiuixColorSpec.Spec2021
            ThemeColorSpec.SPEC_2021 -> MiuixColorSpec.Spec2021
        }

        remember(colorSchemeMode, keyColor, paletteStyle, colorSpecVersion, darkTheme) {
            ThemeController(
                colorSchemeMode = colorSchemeMode,
                keyColor = keyColor,
                paletteStyle = style,
                colorSpec = colorSpecVersion,
                isDark = darkTheme
            )
        }
    } else {
        // --- Default Miuix Theme Path ---
        val colorSchemeMode = when (themeMode) {
            ThemeMode.SYSTEM -> ColorSchemeMode.System
            ThemeMode.LIGHT -> ColorSchemeMode.Light
            ThemeMode.DARK -> ColorSchemeMode.Dark
        }

        remember(colorSchemeMode, darkTheme) {
            ThemeController(
                colorSchemeMode = colorSchemeMode,
                isDark = darkTheme
            )
        }
    }

    MiuixTheme(
        controller = controller,
        content = content
    )
}