// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.widget.setting

import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.material3.MotionScheme
import kotlin.test.Test
import kotlin.test.assertTrue

class ListItemMotionSchemeTest {
    @Test
    fun `expressive spring can extrapolate a zero corner below zero`() {
        val spec = MotionScheme.expressive().fastSpatialSpec<Float>().vectorize(Float.VectorConverter)
        val start = AnimationVector1D(0f)
        val end = AnimationVector1D(1f)
        val velocity = AnimationVector1D(0f)
        val duration = spec.getDurationNanos(start, end, velocity)

        assertTrue(
            (0..1000).any { frame ->
                spec.getValueFromNanos(duration * frame / 1000, start, end, velocity).value > 1f
            },
        )
    }

    @Test
    fun `list item morph stays bounded even with reversed press velocity`() {
        val spec = ListItemMotionScheme(MotionScheme.expressive())
            .fastSpatialSpec<Float>().vectorize(Float.VectorConverter)

        for (initialProgress in listOf(0f, 0.25f, 0.75f, 1f)) {
            for (incomingVelocity in listOf(-20f, 0f, 20f)) {
                val start = AnimationVector1D(initialProgress)
                val end = AnimationVector1D(1f)
                val velocity = AnimationVector1D(incomingVelocity)
                val duration = spec.getDurationNanos(start, end, velocity)

                for (frame in 0..1000) {
                    val progress = spec.getValueFromNanos(duration * frame / 1000, start, end, velocity).value
                    assertTrue(progress in initialProgress..1f, "Invalid morph progress: $progress")
                    // A corner shrinking from 16px to 0px must remain nonnegative.
                    assertTrue(16f * (1f - progress) >= 0f)
                }
            }
        }
    }
}
