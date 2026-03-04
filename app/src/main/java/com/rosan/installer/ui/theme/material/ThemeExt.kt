// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.theme.material

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.PaletteStyle as MaterialKolorPaletteStyle

@Stable
fun dynamicColorScheme(
    keyColor: Color,
    isDark: Boolean,
    style: PaletteStyle = PaletteStyle.TonalSpot,
    contrastLevel: Double = 0.0,
    colorSpec: ThemeColorSpec = ThemeColorSpec.SPEC_2025
): ColorScheme {
    // Map local domain PaletteStyle to materialkolor's PaletteStyle
    val mkStyle = when (style) {
        PaletteStyle.TonalSpot -> MaterialKolorPaletteStyle.TonalSpot
        PaletteStyle.Neutral -> MaterialKolorPaletteStyle.Neutral
        PaletteStyle.Vibrant -> MaterialKolorPaletteStyle.Vibrant
        PaletteStyle.Expressive -> MaterialKolorPaletteStyle.Expressive
        PaletteStyle.Rainbow -> MaterialKolorPaletteStyle.Rainbow
        PaletteStyle.FruitSalad -> MaterialKolorPaletteStyle.FruitSalad
        PaletteStyle.Monochrome -> MaterialKolorPaletteStyle.Monochrome
        PaletteStyle.Fidelity -> MaterialKolorPaletteStyle.Fidelity
        PaletteStyle.Content -> MaterialKolorPaletteStyle.Content
    }

    // Apply SPEC_2025 only if explicitly requested AND supported by the style
    val specVersion = when (colorSpec) {
        ThemeColorSpec.SPEC_2025 -> if (style.supportsSpec2025) ColorSpec.SpecVersion.SPEC_2025 else ColorSpec.SpecVersion.SPEC_2021
        ThemeColorSpec.SPEC_2021 -> ColorSpec.SpecVersion.SPEC_2021
    }

    // Generate and return the color scheme using materialkolor
    return dynamicColorScheme(
        seedColor = keyColor,
        isDark = isDark,
        style = mkStyle,
        contrastLevel = contrastLevel,
        specVersion = specVersion
    )
}

/**
 * Extension function to animate all colors inside a ColorScheme smoothly,
 * including the newly added fixed container roles in Material 3.
 */
@Composable
fun ColorScheme.animateAsState(): ColorScheme {
    // Helper function to reduce boilerplate for state animation
    @Composable
    fun animateColor(color: Color): Color = animateColorAsState(
        targetValue = color,
        animationSpec = spring(),
        label = "theme_color_animation"
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

        // New fixed color roles for Material 3
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
        onTertiaryFixedVariant = animateColor(onTertiaryFixedVariant)
    )
}