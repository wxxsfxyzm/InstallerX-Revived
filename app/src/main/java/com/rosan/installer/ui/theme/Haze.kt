package com.rosan.installer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun rememberMaterial3HazeStyle(): HazeStyle = HazeStyle(
    backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
    tint = HazeTint(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f))
)

@Composable
fun HazeState?.getM3TopBarColor() = this?.let { Color.Transparent } ?: MaterialTheme.colorScheme.surfaceContainer

@Composable
fun rememberMiuixHazeStyle(): HazeStyle = HazeStyle(
    backgroundColor = MiuixTheme.colorScheme.surface,
    tint = HazeTint(MiuixTheme.colorScheme.surface.copy(alpha = 0.8f))
)

@Composable
fun HazeState?.getMiuixAppBarColor() = this?.let { Color.Transparent } ?: MiuixTheme.colorScheme.surface

/**
 * Apply a standard glassmorphism blur effect using Haze.
 * @param state The HazeState to coordinate with the source.
 * @param style The custom HazeStyle.
 */
fun Modifier.installerHazeEffect(
    state: HazeState?,
    style: HazeStyle,
    enabled: Boolean = true
): Modifier = state?.let {
    this.hazeEffect(it) {
        this.style = style
        this.blurEnabled = enabled
        this.blurRadius = 30.dp
        this.noiseFactor = 0f
    }
} ?: this