// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.rosan.installer.ui.library.effect

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.rosan.installer.ui.theme.InstallerTheme
import top.yukonga.miuix.kmp.blur.asComposeShader
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Suppress("SuspiciousIndentation")
@Composable
inline fun BgEffectBackground(
    dynamicBackground: Boolean,
    modifier: Modifier = Modifier,
    bgModifier: Modifier = Modifier,
    effectBackground: Boolean = true,
    alpha: Float = 1f,
    content: @Composable (BoxScope.() -> Unit),
) {
    val shaderSupported = isRuntimeShaderSupported()
    val painter = remember { if (shaderSupported) BgEffectPainter() else null }

    var currentBrush by remember { mutableStateOf<ShaderBrush?>(null) }
    val isDark = InstallerTheme.isDark

    var targetSize by remember { mutableStateOf(IntSize.Zero) }
    val logoHeight = with(LocalDensity.current) { 600.dp.toPx() }

    LaunchedEffect(targetSize, isDark, effectBackground, dynamicBackground) {
        if (painter == null) return@LaunchedEffect
        if (!effectBackground) return@LaunchedEffect
        if (targetSize.width <= 0 || targetSize.height <= 0) return@LaunchedEffect
        painter.showRuntimeShader(
            logoHeight,
            targetSize.height.toFloat(),
            targetSize.width.toFloat(),
            isDark,
        )

        var startTime: Long? = null
        while (dynamicBackground) {
            withFrameNanos { frameTime ->
                if (startTime == null) {
                    startTime = frameTime
                }
                val animTime = ((frameTime - startTime) / 1_000_000_000f) % 62.831852f
                painter.setAnimTime(animTime)
                painter.setResolution(
                    floatArrayOf(
                        targetSize.width.toFloat(),
                        targetSize.height.toFloat(),
                    ),
                )
                painter.updateMaterials()
                currentBrush = ShaderBrush(painter.runtimeShader.asComposeShader())
            }
        }
    }

    Box(
        modifier = modifier.onSizeChanged {
            targetSize = it
        },
    ) {
        val surface = MiuixTheme.colorScheme.surface
        currentBrush?.let { brush ->
            if (!effectBackground) return@let
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .then(bgModifier),
            ) {
                drawRect(surface)
                drawRect(brush, alpha = alpha)
            }
        }
        content()
    }
}
