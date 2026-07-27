// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.animation.predictiveback

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.rosan.installer.domain.settings.model.preferences.PredictiveBackExitDirection
import top.yukonga.miuix.kmp.nav.transition.NavGesture
import top.yukonga.miuix.kmp.nav.transition.NavMotion
import top.yukonga.miuix.kmp.nav.transition.NavRole
import top.yukonga.miuix.kmp.nav.transition.NavSettlePhase
import top.yukonga.miuix.kmp.nav.transition.NavSettleSpec
import top.yukonga.miuix.kmp.nav.transition.NavSwipeEdge
import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.NavTransitionScope
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.nav.transition.navDirectionalTransition
import top.yukonga.miuix.kmp.nav.transition.navGraphicsTransition

private const val SCALE_MIN = 0.85f
private val ScaleExitDrift = 96.dp

private val ScaleExitMotion = NavMotion(
    commit = NavSettleSpec.Tween(durationMillis = 450, easing = FastOutExtraSlowIn),
    cancel = NavSettleSpec.Spring(stiffness = 1500f),
    programmatic = NavSettleSpec.Tween(
        durationMillis = 200,
        easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    ),
)

internal fun scaleNavTransition(exitDirection: PredictiveBackExitDirection): NavTransition {
    val pop = navGraphicsTransition(
        opaqueDepth = 1f,
        motion = ScaleExitMotion,
        scrim = { scope ->
            when {
                scope.settle?.phase == NavSettlePhase.Commit ->
                    (1f - (scope.settle?.elapsedMillis ?: 0f) / 450f).coerceIn(0f, 1f)

                scope.gesture != null -> 1f
                else -> coverProgress(scope.relativeDepth)
            }
        },
    ) { scope ->
        val depth = scope.relativeDepth
        val widthPx = scope.layoutSize.width.toFloat()
        val heightPx = scope.layoutSize.height.toFloat()
        val driftPx = with(scope.density) { ScaleExitDrift.toPx() }
        val gesture = scope.gesture
        val sign = exitDirectionSign(exitDirection, scope)
        val committing = scope.settle?.phase == NavSettlePhase.Commit
        val outgoingCommit = scope.role == NavRole.Outgoing && committing && gesture != null
        if (depth <= 0f) {
            val progress = topProgress(depth)
            val pageScale = if (outgoingCommit) {
                val releaseProgress = (1f - gesture.progress).coerceAtLeast(0.01f)
                val post = (1f - progress / releaseProgress).coerceIn(0f, 1f)
                val releaseEasedProgress = shapedTopProgress(releaseProgress, gesture)
                val committedScale = SCALE_MIN + (1f - SCALE_MIN) * releaseEasedProgress
                committedScale + (SCALE_MIN - committedScale) * post
            } else {
                val easedProgress = shapedTopProgress(progress, gesture)
                SCALE_MIN + (1f - SCALE_MIN) * easedProgress
            }
            val pivotX = if (gesture?.swipeEdge == NavSwipeEdge.Left) 0.8f else 0.2f
            val pivotY = gesturePivotY(gesture, heightPx)
            scaleX = snapScaleToPixelExtent(pageScale, widthPx)
            scaleY = scaleX
            transformOrigin = TransformOrigin(
                pivotFractionX = pivotX,
                pivotFractionY = pivotY,
            )
            val rawTranslationX = if (gesture != null && scope.settle == null) {
                0f
            } else if (outgoingCommit) {
                val releaseProgress = (1f - gesture.progress).coerceAtLeast(0.01f)
                val post = (1f - progress / releaseProgress).coerceIn(0f, 1f)
                sign * post * driftPx
            } else {
                sign * (1f - progress) * widthPx
            }
            translationX = snapTranslationToPixelEdge(
                translation = rawTranslationX,
                scale = scaleX,
                extent = widthPx,
                pivotFraction = pivotX,
            )
            translationY = snapTranslationToPixelEdge(
                translation = 0f,
                scale = scaleY,
                extent = heightPx,
                pivotFraction = pivotY,
            )
            if (outgoingCommit) {
                val elapsedMillis = scope.settle?.elapsedMillis ?: 0f
                alpha = (1f - 5f * (elapsedMillis / 450f)).coerceAtLeast(0f)
            }
        }
    }
    return navDirectionalTransition(
        push = NavTransitions.MiuixDefault,
        pop = pop,
        predictivePop = pop,
    )
}

private fun shapedTopProgress(progress: Float, gesture: NavGesture?): Float =
    if (gesture == null) progress else 1f - BackGestureEasing.transform((1f - progress).coerceIn(0f, 1f))

private fun exitDirectionSign(
    direction: PredictiveBackExitDirection,
    scope: NavTransitionScope,
): Float = when (direction) {
    PredictiveBackExitDirection.FOLLOW_GESTURE ->
        if (scope.gesture?.swipeEdge == NavSwipeEdge.Left) 1f else -1f

    PredictiveBackExitDirection.ALWAYS_RIGHT -> 1f
    PredictiveBackExitDirection.ALWAYS_LEFT -> -1f
}

private fun gesturePivotY(gesture: NavGesture?, height: Float): Float =
    if (gesture != null && height > 0f) {
        (gesture.touchY / height).coerceIn(0.1f, 0.9f)
    } else {
        0.5f
    }
