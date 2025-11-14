package com.rosan.installer.ui.theme.m3color

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.kyant.m3color.hct.Hct
import com.kyant.m3color.scheme.SchemeContent
import com.kyant.m3color.scheme.SchemeExpressive
import com.kyant.m3color.scheme.SchemeFidelity
import com.kyant.m3color.scheme.SchemeFruitSalad
import com.kyant.m3color.scheme.SchemeMonochrome
import com.kyant.m3color.scheme.SchemeNeutral
import com.kyant.m3color.scheme.SchemeRainbow
import com.kyant.m3color.scheme.SchemeTonalSpot
import com.kyant.m3color.scheme.SchemeVibrant

@Stable
fun dynamicColorScheme(
    keyColor: Color,
    isDark: Boolean,
    style: PaletteStyle = PaletteStyle.TonalSpot,
    contrastLevel: Double = 0.0
): ColorScheme {
    val hct = Hct.fromInt(keyColor.toArgb())
    val scheme = when (style) {
        PaletteStyle.TonalSpot -> SchemeTonalSpot(hct, isDark, contrastLevel)
        PaletteStyle.Neutral -> SchemeNeutral(hct, isDark, contrastLevel)
        PaletteStyle.Vibrant -> SchemeVibrant(hct, isDark, contrastLevel)
        PaletteStyle.Expressive -> SchemeExpressive(hct, isDark, contrastLevel)
        PaletteStyle.Rainbow -> SchemeRainbow(hct, isDark, contrastLevel)
        PaletteStyle.FruitSalad -> SchemeFruitSalad(hct, isDark, contrastLevel)
        PaletteStyle.Monochrome -> SchemeMonochrome(hct, isDark, contrastLevel)
        PaletteStyle.Fidelity -> SchemeFidelity(hct, isDark, contrastLevel)
        PaletteStyle.Content -> SchemeContent(hct, isDark, contrastLevel)
    }

    return ColorScheme(
        primary = scheme.primary.toColor(),
        onPrimary = scheme.onPrimary.toColor(),
        primaryContainer = scheme.primaryContainer.toColor(),
        onPrimaryContainer = scheme.onPrimaryContainer.toColor(),
        inversePrimary = scheme.inversePrimary.toColor(),
        secondary = scheme.secondary.toColor(),
        onSecondary = scheme.onSecondary.toColor(),
        secondaryContainer = scheme.secondaryContainer.toColor(),
        onSecondaryContainer = scheme.onSecondaryContainer.toColor(),
        tertiary = scheme.tertiary.toColor(),
        onTertiary = scheme.onTertiary.toColor(),
        tertiaryContainer = scheme.tertiaryContainer.toColor(),
        onTertiaryContainer = scheme.onTertiaryContainer.toColor(),
        background = scheme.background.toColor(),
        onBackground = scheme.onBackground.toColor(),
        surface = scheme.surface.toColor(),
        onSurface = scheme.onSurface.toColor(),
        surfaceVariant = scheme.surfaceVariant.toColor(),
        onSurfaceVariant = scheme.onSurfaceVariant.toColor(),
        surfaceTint = scheme.primary.toColor(),
        inverseSurface = scheme.inverseSurface.toColor(),
        inverseOnSurface = scheme.inverseOnSurface.toColor(),
        error = scheme.error.toColor(),
        onError = scheme.onError.toColor(),
        errorContainer = scheme.errorContainer.toColor(),
        onErrorContainer = scheme.onErrorContainer.toColor(),
        outline = scheme.outline.toColor(),
        outlineVariant = scheme.outlineVariant.toColor(),
        scrim = scheme.scrim.toColor(),
        surfaceBright = scheme.surfaceBright.toColor(),
        surfaceContainer = scheme.surfaceContainer.toColor(),
        surfaceContainerHigh = scheme.surfaceContainerHigh.toColor(),
        surfaceContainerHighest = scheme.surfaceContainerHighest.toColor(),
        surfaceContainerLow = scheme.surfaceContainerLow.toColor(),
        surfaceContainerLowest = scheme.surfaceContainerLowest.toColor(),
        surfaceDim = scheme.surfaceDim.toColor()
    )
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Int.toColor(): Color = Color(this)