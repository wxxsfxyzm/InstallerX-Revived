// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.widget.setting

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

internal class ListItemMotionScheme(delegate: MotionScheme) : MotionScheme by delegate {
    // Material 3 1.5.0-beta01 interpolates corners with unbounded FastSpatial progress.
    // A spring can overshoot zero-radius corners, including when a press is reversed.
    // Tween ignores incoming velocity and keeps the shape between its valid endpoints.
    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = tween(durationMillis = 180)
}

@Composable
internal fun ListItemMotion(content: @Composable () -> Unit) {
    val motionScheme = MaterialTheme.motionScheme
    val listItemMotionScheme = remember(motionScheme) { ListItemMotionScheme(motionScheme) }
    MaterialTheme(motionScheme = listItemMotionScheme, content = content)
}
