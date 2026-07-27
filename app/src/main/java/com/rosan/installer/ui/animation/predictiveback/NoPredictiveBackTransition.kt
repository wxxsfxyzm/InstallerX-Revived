// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.animation.predictiveback

import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastRoundToInt
import top.yukonga.miuix.kmp.nav.runtime.NavProgrammaticEasing
import top.yukonga.miuix.kmp.nav.transition.NavMotion
import top.yukonga.miuix.kmp.nav.transition.NavRole
import top.yukonga.miuix.kmp.nav.transition.NavSettle
import top.yukonga.miuix.kmp.nav.transition.NavSettlePhase
import top.yukonga.miuix.kmp.nav.transition.NavSettleSpec
import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.NavTransitionScope
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.nav.transition.navDirectionalTransition
import top.yukonga.miuix.kmp.nav.transition.navGraphicsTransition

private const val NO_PREDICTIVE_POP_DURATION_MILLIS = 450

/**
 * Keeps the page at the point where back interrupted it instead of handing the in-flight push to
 * predictive progress. A committed back slowly plays the page out from that point; cancellation
 * lets the interrupted push finish entering.
 */
private val NoPredictivePop: NavTransition = navGraphicsTransition(
    opaqueDepth = 1f,
    motion = NavMotion(
        commit = NavSettleSpec.Tween(
            durationMillis = NO_PREDICTIVE_POP_DURATION_MILLIS,
            easing = NavProgrammaticEasing,
        ),
        cancel = NavSettleSpec.Tween(
            durationMillis = NO_PREDICTIVE_POP_DURATION_MILLIS,
            easing = NavProgrammaticEasing,
        ),
    ),
    scrim = { scope -> 1f - noPredictiveVisualProgress(scope) },
) { scope ->
    applyNoPredictiveTransform(scope, noPredictiveVisualProgress(scope))
}

internal val NoPredictiveBackTransition: NavTransition = navDirectionalTransition(
    push = NavTransitions.MiuixDefault,
    pop = NavTransitions.MiuixDefault,
    predictivePop = NoPredictivePop,
)

/**
 * Reconstructs the grab anchor hidden by the shared depth driver. While the finger is active the
 * visual progress stays at that anchor. Commit and cancel then animate from the anchor rather than
 * from the finger's predictive progress.
 */
private fun noPredictiveVisualProgress(scope: NavTransitionScope): Float {
    val gesture = scope.gesture ?: return 0f
    val topDepth = when (scope.role) {
        NavRole.Covered -> scope.relativeDepth - 1f
        NavRole.Top if scope.settle?.phase == NavSettlePhase.Commit ->
            scope.relativeDepth - 1f

        else -> scope.relativeDepth
    }
    val totalProgress = -topDepth
    val settle = scope.settle
    return when (settle?.phase) {
        null -> (totalProgress - gesture.progress).coerceIn(0f, 1f)
        NavSettlePhase.Commit -> {
            val settleProgress = noPredictiveSettleProgress(settle)
            val remaining = 1f - settleProgress
            if (remaining <= 0.001f) {
                1f
            } else {
                val anchor = (
                    (totalProgress - settleProgress) / remaining - gesture.progress
                    ).coerceIn(0f, 1f)
                anchor + (1f - anchor) * settleProgress
            }
        }

        NavSettlePhase.Cancel -> {
            val settleProgress = noPredictiveSettleProgress(settle)
            val remaining = 1f - settleProgress
            if (remaining <= 0.001f) {
                0f
            } else {
                val anchor = (totalProgress / remaining - gesture.progress).coerceIn(0f, 1f)
                anchor * remaining
            }
        }

        NavSettlePhase.Programmatic ->
            (totalProgress - gesture.progress).coerceIn(0f, 1f)
    }
}

private fun noPredictiveSettleProgress(settle: NavSettle): Float {
    val fraction = (settle.elapsedMillis / NO_PREDICTIVE_POP_DURATION_MILLIS).coerceIn(0f, 1f)
    return NavProgrammaticEasing.transform(fraction).coerceIn(0f, 1f)
}

private fun GraphicsLayerScope.applyNoPredictiveTransform(
    scope: NavTransitionScope,
    progress: Float,
) {
    val widthPx = scope.layoutSize.width.toFloat()
    val direction = if (scope.layoutDirection == LayoutDirection.Rtl) -1f else 1f
    val isLowerEntry = scope.role == NavRole.Covered ||
        scope.role == NavRole.Top && scope.settle?.phase == NavSettlePhase.Commit
    if (isLowerEntry) {
        translationX = -direction * (1f - progress) * widthPx * 0.25f
        alpha = 0.9f + 0.1f * progress
    } else {
        translationX = (direction * progress * widthPx).fastRoundToInt().toFloat()
    }
}
