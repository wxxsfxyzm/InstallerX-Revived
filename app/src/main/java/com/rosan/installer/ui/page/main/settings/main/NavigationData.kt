package com.rosan.installer.ui.page.main.settings.main

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import dev.chrisbanes.haze.HazeState

data class NavigationData(
    val icon: ImageVector,
    val label: String,
    val content: @Composable (PaddingValues, HazeState?) -> Unit
)