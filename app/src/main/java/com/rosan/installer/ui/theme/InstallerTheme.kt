// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.theme

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.rosan.installer.domain.settings.model.preferences.ThemeState
import com.rosan.installer.domain.settings.model.preferences.theme.MiuixOptions
import com.rosan.installer.domain.settings.model.preferences.theme.PaletteStyle
import com.rosan.installer.domain.settings.model.preferences.theme.ThemeColorSpec
import com.rosan.installer.domain.settings.model.preferences.theme.ThemeMode
import com.rosan.installer.domain.settings.model.preferences.theme.UiFamily
import com.rosan.installer.ui.color.ColorThemeConfig
import com.rosan.installer.ui.color.rememberColorScheme
import com.rosan.installer.ui.family.host
import top.yukonga.miuix.kmp.material.isRenderEffectSupported

private val LocalUiFamily = staticCompositionLocalOf { UiFamily.MATERIAL3 }
private val LocalUseMiuixOs4 = staticCompositionLocalOf { false }
private val LocalIsDark = staticCompositionLocalOf { false }
private val LocalPaletteStyle = staticCompositionLocalOf { PaletteStyle.Expressive }
private val LocalThemeColorSpec = staticCompositionLocalOf { ThemeColorSpec.SPEC_2025 }
private val LocalSeedColor = staticCompositionLocalOf { Color.Unspecified }
private val LocalThemeMode = staticCompositionLocalOf { ThemeMode.SYSTEM }
private val LocalUseMiuixMonet = staticCompositionLocalOf { false }
private val LocalUseDynamicColor = staticCompositionLocalOf { false }

val LocalInstallerColorScheme = staticCompositionLocalOf<ColorScheme> { error("No ColorScheme provided") }
private val LocalColorThemeConfig = staticCompositionLocalOf<ColorThemeConfig> { error("No ColorThemeConfig provided") }

object InstallerTheme {
    val uiFamily: UiFamily
        @Composable @ReadOnlyComposable
        get() = LocalUiFamily.current

    /** Whether the Miuix OS4 style is active after checking the UI family, the option, and device support. */
    val useMiuixOs4: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalUseMiuixOs4.current

    val colorScheme: ColorScheme
        @Composable @ReadOnlyComposable
        get() = LocalInstallerColorScheme.current

    /** Color input shared by every UI family. */
    val colorConfig: ColorThemeConfig
        @Composable @ReadOnlyComposable
        get() = LocalColorThemeConfig.current

    val isDark: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalIsDark.current

    val seedColor: Color
        @Composable @ReadOnlyComposable
        get() = LocalSeedColor.current

    val paletteStyle: PaletteStyle
        @Composable @ReadOnlyComposable
        get() = LocalPaletteStyle.current

    val colorSpec: ThemeColorSpec
        @Composable @ReadOnlyComposable
        get() = LocalThemeColorSpec.current

    val themeMode: ThemeMode
        @Composable @ReadOnlyComposable
        get() = LocalThemeMode.current

    val useMiuixMonet: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalUseMiuixMonet.current

    val useDynamicColor: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalUseDynamicColor.current
}

@Composable
fun InstallerTheme(
    uiFamily: UiFamily,
    miuixOptions: MiuixOptions,
    themeMode: ThemeMode,
    paletteStyle: PaletteStyle,
    colorSpec: ThemeColorSpec,
    useDynamicColor: Boolean,
    seedColor: Color,
    useAmoled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val preservedContent = remember {
        movableContentOf<@Composable () -> Unit> { targetContent ->
            targetContent()
        }
    }

    ProvideInstallerTheme(
        uiFamily = uiFamily,
        miuixOptions = miuixOptions,
        themeMode = themeMode,
        paletteStyle = paletteStyle,
        colorSpec = colorSpec,
        useDynamicColor = useDynamicColor,
        seedColor = seedColor,
        useAmoled = useAmoled,
    ) {
        UpdateStatusBarAppearance(InstallerTheme.isDark)
        uiFamily.host().rememberPageTheme(useAmoled).apply {
            preservedContent(content)
        }
    }
}

internal typealias SettingsPageTheme = @Composable (content: @Composable () -> Unit) -> Unit

/** Keep theme preferences and the palette above navigation, and apply the UI family inside each entry. */
@Composable
internal fun InstallerSettingsTheme(
    settings: ThemeState,
    content: @Composable (backdropColor: Color, pageTheme: SettingsPageTheme) -> Unit,
) {
    ProvideInstallerTheme(
        uiFamily = settings.uiFamily,
        miuixOptions = settings.miuixOptions,
        themeMode = settings.themeMode,
        paletteStyle = settings.paletteStyle,
        colorSpec = settings.colorSpec,
        useDynamicColor = settings.useDynamicColor,
        seedColor = Color(settings.seedColor),
        useAmoled = settings.useAmoled,
    ) {
        UpdateStatusBarAppearance(InstallerTheme.isDark)
        val pageTheme = settings.uiFamily.host().rememberPageTheme(settings.useAmoled)
        content(pageTheme.backdropColor, pageTheme.apply)
    }
}

@Composable
private fun ProvideInstallerTheme(
    uiFamily: UiFamily,
    miuixOptions: MiuixOptions,
    themeMode: ThemeMode,
    paletteStyle: PaletteStyle,
    colorSpec: ThemeColorSpec,
    useDynamicColor: Boolean,
    seedColor: Color,
    useAmoled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorConfig = colorThemeConfig(
        themeMode = themeMode,
        isDark = isDark,
        seedColor = seedColor,
        useDynamicColor = useDynamicColor,
        paletteStyle = paletteStyle,
        colorSpec = colorSpec,
        useAmoled = useAmoled,
    )
    val colorScheme = rememberColorScheme(colorConfig)

    CompositionLocalProvider(
        LocalUseMiuixOs4 provides (
            uiFamily == UiFamily.MIUIX && miuixOptions.useOs4 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && isRenderEffectSupported()
            ),
        LocalIsDark provides isDark,
        LocalPaletteStyle provides paletteStyle,
        LocalSeedColor provides seedColor,
        LocalInstallerColorScheme provides colorScheme,
        LocalColorThemeConfig provides colorConfig,
        LocalThemeMode provides themeMode,
        LocalUiFamily provides uiFamily,
        LocalUseMiuixMonet provides miuixOptions.useMonet,
        LocalUseDynamicColor provides useDynamicColor,
        LocalThemeColorSpec provides colorSpec,
    ) {
        // Disable navigation bar contrast enforced for Android 10 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            NavigationBarContrastHandler()
        }
        content()
    }
}

@Composable
private fun UpdateStatusBarAppearance(darkTheme: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as ComponentActivity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
private fun NavigationBarContrastHandler() {
    val configuration = LocalConfiguration.current
    val activity = LocalActivity.current

    DisposableEffect(configuration) {
        val window = activity?.window
        window?.isNavigationBarContrastEnforced = false

        onDispose { /** Keep empty as we want this behavior to persist **/ }
    }
}
