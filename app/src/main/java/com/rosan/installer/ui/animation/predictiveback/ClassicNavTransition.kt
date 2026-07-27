// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.animation.predictiveback

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import top.yukonga.miuix.kmp.nav.transition.NavMotion
import top.yukonga.miuix.kmp.nav.transition.NavSettleSpec
import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.NavTransitionScope
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.nav.transition.navDirectionalTransition

private val ClassicScaleMotion = NavMotion(
    commit = NavSettleSpec.Tween(
        durationMillis = 200,
        easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    ),
    cancel = NavSettleSpec.Spring(stiffness = 1500f),
    programmatic = NavSettleSpec.Tween(
        durationMillis = 200,
        easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    ),
)

private val ClassicScalePop: NavTransition = object : NavTransition {
    override val opaqueDepth: Float = 1f

    override val motion: NavMotion = ClassicScaleMotion

    override fun scrimFraction(scope: NavTransitionScope): Float = coverProgress(scope.relativeDepth)

    override fun Modifier.transformEntry(scope: NavTransitionScope): Modifier {
        val zIndex = if (scope.relativeDepth > 0f) 1f else 0f
        return graphicsLayer {
            val depth = scope.relativeDepth
            val widthPx = scope.layoutSize.width.toFloat()
            val heightPx = scope.layoutSize.height.toFloat()
            if (depth <= 0f) {
                val progress = topProgress(depth)
                scaleX = snapScaleToPixelExtent(0.9f + 0.1f * progress, widthPx)
                scaleY = scaleX
                translationX = snapTranslationToPixelEdge(0f, scaleX, widthPx)
                translationY = snapTranslationToPixelEdge(0f, scaleY, heightPx)
                alpha = progress
            } else {
                translationX = snapTranslationToPixelEdge(
                    translation = -coverProgress(depth) * widthPx,
                    scale = 1f,
                    extent = widthPx,
                )
            }
        }.zIndex(zIndex)
    }
}

internal val ClassicNavTransition: NavTransition = navDirectionalTransition(
    push = NavTransitions.MiuixDefault,
    pop = ClassicScalePop,
    predictivePop = ClassicScalePop,
)
