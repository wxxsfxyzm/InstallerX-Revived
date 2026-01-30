package com.rosan.installer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
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