// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.widget.card

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedFluidBackground(baseColor: Color, enabled: Boolean, modifier: Modifier = Modifier) {
    if (!enabled) return

    Box(modifier = modifier) {
        AnimatedFluidBackgroundLayers(
            baseColor = baseColor,
        )
    }
}

@Composable
private fun AnimatedFluidBackgroundLayers(baseColor: Color) {
    val transition = rememberInfiniteTransition(label = "fluid_background_transition")

    val primaryFlow by transition.animateColor(
        initialValue = baseColor.copy(alpha = 0.9f),
        targetValue = baseColor.copy(alpha = 0.6f)
            .compositeOver(Color.Magenta.copy(alpha = 0.2f)),
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "primary_flow",
    )

    val secondaryFlow by transition.animateColor(
        initialValue = baseColor.copy(alpha = 0.7f)
            .compositeOver(Color.Cyan.copy(alpha = 0.25f)),
        targetValue = baseColor.copy(alpha = 0.85f)
            .compositeOver(Color.Blue.copy(alpha = 0.15f)),
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "secondary_flow",
    )

    val accentFlow by transition.animateColor(
        initialValue = baseColor.copy(alpha = 0.6f)
            .compositeOver(Color.Yellow.copy(alpha = 0.1f)),
        targetValue = baseColor.copy(alpha = 0.8f)
            .compositeOver(Color.Green.copy(alpha = 0.18f)),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 5200,
                easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f),
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "accent_flow",
    )

    val complementFlow by transition.animateColor(
        initialValue = baseColor.copy(alpha = 0.5f)
            .compositeOver(Color.Red.copy(alpha = 0.12f)),
        targetValue = baseColor.copy(alpha = 0.75f)
            .compositeOver(Color(0xFFFF6B35).copy(alpha = 0.2f)),
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "complement_flow",
    )

    var timeState by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { frameTimeNanos ->
                timeState = (frameTimeNanos / 1_000_000_000.0).toFloat()
            }
        }
    }

    val time1 = timeState * 0.1f
    val time2 = timeState * 0.133f
    val time3 = timeState * 0.167f
    val microTime = timeState * 0.2f

    val layerAlpha1 by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "layer_alpha_1",
    )

    val layerAlpha2 by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "layer_alpha_2",
    )

    val layerAlpha3 by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "layer_alpha_3",
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .alpha(layerAlpha1),
    ) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2
        val maxRadius = maxOf(width, height)

        val flowCenters = listOf(
            Offset(
                x = centerX +
                    width * 0.45f * sin(time1 * 0.8f + 0.5f) +
                    width * 0.15f * cos(time2 * 0.7f),
                y = centerY +
                    height * 0.4f * cos(time1 * 0.9f) +
                    height * 0.12f * sin(time2 * 1.1f + 1.2f),
            ),
            Offset(
                x = centerX +
                    width * 0.5f * cos(time1 * 0.6f + 2.1f) +
                    width * 0.18f * sin(time2 * 0.9f + 0.8f),
                y = centerY +
                    height * 0.42f * sin(time1 * 0.7f + 1.5f) +
                    height * 0.15f * cos(time2 * 0.8f + 2f),
            ),
            Offset(
                x = centerX +
                    width * 0.38f * sin(time1 * 0.75f + 3.8f) +
                    width * 0.2f * cos(microTime * 1.2f),
                y = centerY +
                    height * 0.35f * cos(time1 * 0.85f + 2.7f) +
                    height * 0.17f * sin(microTime * 1f + 1.1f),
            ),
        )

        val radii = listOf(
            maxRadius * 0.8f + maxRadius * 0.12f * sin(microTime * 0.8f),
            maxRadius * 0.75f + maxRadius * 0.15f * cos(microTime * 0.9f + 1f),
            maxRadius * 0.85f + maxRadius * 0.1f * sin(microTime * 1.1f + 2.3f),
        )

        val colors = listOf(primaryFlow, secondaryFlow, accentFlow)

        for (i in flowCenters.indices) {
            val fluidColor = colors[i] // Extract color variable
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        fluidColor,
                        fluidColor.copy(alpha = fluidColor.alpha * 0.6f),
                        fluidColor.copy(alpha = 0f),
                    ),
                    center = flowCenters[i],
                    radius = radii[i],
                ),
                center = flowCenters[i],
                radius = radii[i],
            )
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .alpha(layerAlpha2),
    ) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2
        val maxRadius = maxOf(width, height)

        val fluidCenters = (0..4).map { i ->
            val phase = i * PI.toFloat() / 2.5f
            Offset(
                x = centerX +
                    width * 0.35f * sin(time2 * 0.6f + phase) +
                    width * 0.2f * cos(time3 * 0.5f + phase * 1.5f) +
                    width * 0.08f * sin(microTime * 1.5f + phase * 0.8f),
                y = centerY +
                    height * 0.32f * cos(time2 * 0.7f + phase * 1.2f) +
                    height * 0.25f * sin(time3 * 0.6f + phase * 0.7f) +
                    height * 0.1f * cos(microTime * 1.3f + phase * 1.3f),
            )
        }

        val fluidRadii = (0..4).map { i ->
            val baseRadius = maxRadius * (0.55f + 0.15f * sin(i.toFloat()))
            baseRadius + maxRadius * 0.12f * cos(microTime * (0.8f + i * 0.2f))
        }

        val fluidColors = listOf(
            secondaryFlow.copy(alpha = secondaryFlow.alpha * 0.8f),
            accentFlow.copy(alpha = accentFlow.alpha * 0.7f),
            complementFlow.copy(alpha = complementFlow.alpha * 0.9f),
            primaryFlow.copy(alpha = primaryFlow.alpha * 0.6f),
            secondaryFlow.copy(alpha = secondaryFlow.alpha * 0.75f),
        )

        for (i in fluidCenters.indices) {
            val fluidColor = fluidColors[i] // Extract color variable
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        fluidColor,
                        fluidColor.copy(alpha = fluidColor.alpha * 0.4f),
                        fluidColor.copy(alpha = fluidColor.alpha * 0.1f),
                        fluidColor.copy(alpha = 0f),
                    ),
                    center = fluidCenters[i],
                    radius = fluidRadii[i],
                ),
                center = fluidCenters[i],
                radius = fluidRadii[i],
            )
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .alpha(layerAlpha3),
    ) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2
        val maxRadius = maxOf(width, height)

        val fastTime = microTime * 2.5f
        val mediumTime = time1 * 1.5f

        val texturePoints = (0..7).map { i ->
            val angle = i * 2 * PI.toFloat() / 8
            val dynamicRadius =
                width * 0.3f + width * 0.2f * sin(fastTime + angle * 1.5f)
            val turbulentOffset =
                width * 0.06f * cos(fastTime * 0.8f + angle * 2f)

            Offset(
                x = centerX +
                    dynamicRadius * cos(angle + mediumTime * 0.3f) +
                    turbulentOffset,
                y = centerY +
                    dynamicRadius * sin(angle + mediumTime * 0.3f) +
                    height * 0.05f * sin(fastTime * 1.2f + angle),
            )
        }

        for (i in texturePoints.indices) {
            val dynamicRadius =
                maxRadius * (0.2f + 0.1f * sin(fastTime + i * 0.5f))
            val opacity =
                0.25f + 0.15f * cos(fastTime * 0.7f + i * 0.3f)

            // Calculate base texture color beforehand
            val textureColor = when (i % 4) {
                0 -> primaryFlow
                1 -> secondaryFlow
                2 -> accentFlow
                else -> complementFlow
            }.copy(alpha = opacity)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        textureColor,
                        textureColor.copy(alpha = 0f),
                    ),
                    center = texturePoints[i],
                    radius = dynamicRadius,
                ),
                center = texturePoints[i],
                radius = dynamicRadius,
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val maxRadius = maxOf(width, height)

        val glowTime = time1 * 0.5f
        val glowCenter = Offset(width / 2, height / 2)
        val glowRadius =
            maxRadius * (0.75f + 0.15f * sin(glowTime * 0.6f))
        val glowIntensity =
            0.08f + 0.04f * cos(glowTime * 0.8f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    baseColor.copy(alpha = glowIntensity),
                    baseColor.copy(alpha = glowIntensity * 0.5f),
                    Color.Transparent,
                ),
                center = glowCenter,
                radius = glowRadius,
            ),
            center = glowCenter,
            radius = glowRadius,
        )
    }
}
