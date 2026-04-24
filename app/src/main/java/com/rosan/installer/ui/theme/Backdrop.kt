// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRenderEffectSupported
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Remember a LayerBackdrop with a solid background to prevent alpha-blending artifacts.
 * @param enableBlur Whether the blur effect is globally enabled.
 * @return A LayerBackdrop instance if supported and enabled, null otherwise.
 */
@Composable
fun rememberMiuixBlurBackdrop(enableBlur: Boolean): LayerBackdrop? {
    if (!enableBlur || !isRenderEffectSupported()) return null
    val surfaceColor = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

/**
 * Determine the app bar background color based on the Backdrop availability.
 * @return Transparent if Backdrop is active, otherwise the default surface color.
 */
@Composable
fun LayerBackdrop?.getMiuixAppBarColor(): Color =
    this?.let { Color.Transparent } ?: MiuixTheme.colorScheme.surface

/**
 * Apply a standard glassmorphism blur effect using Miuix Backdrop.
 * @param backdrop The LayerBackdrop providing the visual source.
 * @param enabled Whether the effect is locally enabled for this component.
 * @param blurRadius The radius of the Gaussian blur.
 * @param shape The clipping shape for the blurred area.
 */
@Composable
fun Modifier.installerMiuixBlurEffect(
    backdrop: LayerBackdrop?,
    enabled: Boolean = true,
    blurRadius: Float = 25f,
    shape: Shape = RectangleShape
): Modifier {
    // Return early if disabled or backdrop is unavailable
    if (!enabled || backdrop == null) return this

    // Grab the current theme surface color for blending
    val blendColor = MiuixTheme.colorScheme.surface.copy(alpha = 0.8f)

    return this.then(
        Modifier.textureBlur(
            backdrop = backdrop,
            shape = shape,
            blurRadius = blurRadius,
            colors = BlurColors(
                blendColors = listOf(
                    BlendColorEntry(color = blendColor)
                )
            )
        )
    )
}

/**
 * Remember a LayerBackdrop for Material 3 with a surfaceContainer background
 * to prevent alpha-blending artifacts.
 *
 * @param enableBlur Whether the blur effect is globally enabled.
 * @return A LayerBackdrop instance if supported and enabled, null otherwise.
 */
@Composable
fun rememberMaterial3BlurBackdrop(enableBlur: Boolean): LayerBackdrop? {
    if (!enableBlur || !isRenderEffectSupported()) return null
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainer
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

/**
 * Determine the app bar background color for Material 3 based on the Backdrop availability.
 *
 * @return Transparent if Backdrop is active, otherwise the surfaceContainer color.
 */
@Composable
fun LayerBackdrop?.getMaterial3AppBarColor(): Color =
    this?.let { Color.Transparent } ?: MaterialTheme.colorScheme.surfaceContainer

/**
 * Apply a standard glassmorphism blur effect using Material 3 color schemes.
 *
 * @param backdrop The LayerBackdrop providing the visual source.
 * @param enabled Whether the effect is locally enabled for this component.
 * @param blurRadius The radius of the Gaussian blur.
 * @param shape The clipping shape for the blurred area.
 */
@Composable
fun Modifier.installerMaterial3BlurEffect(
    backdrop: LayerBackdrop?,
    enabled: Boolean = true,
    blurRadius: Float = 25f,
    shape: Shape = RectangleShape
): Modifier {
    // Return early if disabled or backdrop is unavailable
    if (!enabled || backdrop == null) return this

    // Grab the current M3 surfaceContainer color for blending with 80% opacity
    val blendColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f)

    return this.then(
        Modifier.textureBlur(
            backdrop = backdrop,
            shape = shape,
            blurRadius = blurRadius,
            colors = BlurColors(
                blendColors = listOf(
                    BlendColorEntry(color = blendColor)
                )
            )
        )
    )
}
