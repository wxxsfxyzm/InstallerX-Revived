// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.color

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.materialkolor.PaletteStyle as MaterialKolorPaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.material3.dynamicColorScheme as materialKolorColorScheme

/** Generate a Material color scheme from [keyColor]. */
@Stable
fun dynamicColorScheme(
    keyColor: Color,
    isDark: Boolean,
    style: ColorPaletteStyle = ColorPaletteStyle.TonalSpot,
    specVersion: ColorSpecVersion = ColorSpecVersion.Spec2025,
    contrastLevel: Double = 0.0,
): ColorScheme = materialKolorColorScheme(
    seedColor = keyColor,
    isDark = isDark,
    style = style.toMaterialKolor(),
    contrastLevel = contrastLevel,
    specVersion = specVersion.toMaterialKolor(),
)

/** Return the system accent color when dynamic color is on and supported, or the seed color otherwise. */
@Composable
fun ColorThemeConfig.rememberKeyColor(): Color = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    colorResource(id = android.R.color.system_accent1_500)
} else {
    seedColor
}

/** Generate the color scheme for [config], apply AMOLED surfaces, and animate color changes. */
@Composable
fun rememberColorScheme(config: ColorThemeConfig): ColorScheme {
    val keyColor = config.rememberKeyColor()
    val baseColorScheme = remember(keyColor, config.isDark, config.paletteStyle, config.effectiveSpec) {
        dynamicColorScheme(
            keyColor = keyColor,
            isDark = config.isDark,
            style = config.paletteStyle,
            specVersion = config.effectiveSpec,
        )
    }

    // Only callers that opt in use AMOLED backgrounds, and only in dark mode.
    val colorScheme = if (config.useAmoled && config.isDark) {
        baseColorScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceContainer = Color.Black,
        )
    } else {
        baseColorScheme
    }

    return colorScheme.animateAsState()
}

/**
 * Animate all colors inside a ColorScheme smoothly,
 * including the fixed container roles in Material 3.
 */
@Composable
fun ColorScheme.animateAsState(): ColorScheme {
    @Composable
    fun animateColor(color: Color): Color = animateColorAsState(
        targetValue = color,
        animationSpec = spring(),
        label = "theme_color_animation",
    ).value

    return ColorScheme(
        primary = animateColor(primary),
        onPrimary = animateColor(onPrimary),
        primaryContainer = animateColor(primaryContainer),
        onPrimaryContainer = animateColor(onPrimaryContainer),
        inversePrimary = animateColor(inversePrimary),
        secondary = animateColor(secondary),
        onSecondary = animateColor(onSecondary),
        secondaryContainer = animateColor(secondaryContainer),
        onSecondaryContainer = animateColor(onSecondaryContainer),
        tertiary = animateColor(tertiary),
        onTertiary = animateColor(onTertiary),
        tertiaryContainer = animateColor(tertiaryContainer),
        onTertiaryContainer = animateColor(onTertiaryContainer),
        background = animateColor(background),
        onBackground = animateColor(onBackground),
        surface = animateColor(surface),
        onSurface = animateColor(onSurface),
        surfaceVariant = animateColor(surfaceVariant),
        onSurfaceVariant = animateColor(onSurfaceVariant),
        surfaceTint = animateColor(surfaceTint),
        inverseSurface = animateColor(inverseSurface),
        inverseOnSurface = animateColor(inverseOnSurface),
        error = animateColor(error),
        onError = animateColor(onError),
        errorContainer = animateColor(errorContainer),
        onErrorContainer = animateColor(onErrorContainer),
        outline = animateColor(outline),
        outlineVariant = animateColor(outlineVariant),
        scrim = animateColor(scrim),
        surfaceBright = animateColor(surfaceBright),
        surfaceDim = animateColor(surfaceDim),
        surfaceContainer = animateColor(surfaceContainer),
        surfaceContainerHigh = animateColor(surfaceContainerHigh),
        surfaceContainerHighest = animateColor(surfaceContainerHighest),
        surfaceContainerLow = animateColor(surfaceContainerLow),
        surfaceContainerLowest = animateColor(surfaceContainerLowest),
        primaryFixed = animateColor(primaryFixed),
        primaryFixedDim = animateColor(primaryFixedDim),
        onPrimaryFixed = animateColor(onPrimaryFixed),
        onPrimaryFixedVariant = animateColor(onPrimaryFixedVariant),
        secondaryFixed = animateColor(secondaryFixed),
        secondaryFixedDim = animateColor(secondaryFixedDim),
        onSecondaryFixed = animateColor(onSecondaryFixed),
        onSecondaryFixedVariant = animateColor(onSecondaryFixedVariant),
        tertiaryFixed = animateColor(tertiaryFixed),
        tertiaryFixedDim = animateColor(tertiaryFixedDim),
        onTertiaryFixed = animateColor(onTertiaryFixed),
        onTertiaryFixedVariant = animateColor(onTertiaryFixedVariant),
    )
}

private fun ColorPaletteStyle.toMaterialKolor(): MaterialKolorPaletteStyle = when (this) {
    ColorPaletteStyle.TonalSpot -> MaterialKolorPaletteStyle.TonalSpot
    ColorPaletteStyle.Neutral -> MaterialKolorPaletteStyle.Neutral
    ColorPaletteStyle.Vibrant -> MaterialKolorPaletteStyle.Vibrant
    ColorPaletteStyle.Expressive -> MaterialKolorPaletteStyle.Expressive
    ColorPaletteStyle.Rainbow -> MaterialKolorPaletteStyle.Rainbow
    ColorPaletteStyle.FruitSalad -> MaterialKolorPaletteStyle.FruitSalad
    ColorPaletteStyle.Monochrome -> MaterialKolorPaletteStyle.Monochrome
    ColorPaletteStyle.Fidelity -> MaterialKolorPaletteStyle.Fidelity
    ColorPaletteStyle.Content -> MaterialKolorPaletteStyle.Content
    ColorPaletteStyle.Cmf -> MaterialKolorPaletteStyle.Cmf()
}

private fun ColorSpecVersion.toMaterialKolor(): ColorSpec.SpecVersion = when (this) {
    ColorSpecVersion.Spec2021 -> ColorSpec.SpecVersion.SPEC_2021
    ColorSpecVersion.Spec2025 -> ColorSpec.SpecVersion.SPEC_2025
    ColorSpecVersion.Spec2026 -> ColorSpec.SpecVersion.SPEC_2026
}
