// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.animation.predictiveback

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.rosan.installer.domain.settings.model.preferences.PredictiveBackAnimation
import com.rosan.installer.domain.settings.model.preferences.PredictiveBackExitDirection
import top.yukonga.miuix.kmp.nav.transition.NavGesture
import top.yukonga.miuix.kmp.nav.transition.NavMotion
import top.yukonga.miuix.kmp.nav.transition.NavRole
import top.yukonga.miuix.kmp.nav.transition.NavSettle
import top.yukonga.miuix.kmp.nav.transition.NavSettlePhase
import top.yukonga.miuix.kmp.nav.transition.NavSettleSpec
import top.yukonga.miuix.kmp.nav.transition.NavSwipeEdge
import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.NavTransitionScope
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.nav.transition.navDirectionalTransition
import top.yukonga.miuix.kmp.nav.transition.navGraphicsTransition
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

fun installerNavTransition(animation: PredictiveBackAnimation): NavTransition = when (animation) {
    PredictiveBackAnimation.None -> NavTransitions.None
    PredictiveBackAnimation.MIUIX -> NavTransitions.MiuixDefault
    PredictiveBackAnimation.AOSP -> CrossActivityTransition
    PredictiveBackAnimation.Scale -> scaleTransition(PredictiveBackExitDirection.ALWAYS_RIGHT)
    PredictiveBackAnimation.Classic -> ClassicScaleTransition
}

fun installerNavTransition(
    animation: PredictiveBackAnimation,
    exitDirection: PredictiveBackExitDirection,
): NavTransition = when (animation) {
    PredictiveBackAnimation.None -> NavTransitions.None
    PredictiveBackAnimation.MIUIX -> NavTransitions.MiuixDefault
    PredictiveBackAnimation.AOSP -> CrossActivityTransition
    PredictiveBackAnimation.Scale -> scaleTransition(exitDirection)
    PredictiveBackAnimation.Classic -> ClassicScaleTransition
}

private val FastOutExtraSlowIn: Easing = run {
    val knotX = 0.166666f
    val knotY = 0.4f
    val first = CubicBezierEasing(0.05f / knotX, 0f, 0.133333f / knotX, 0.06f / knotY)
    val second = CubicBezierEasing(
        (0.208333f - knotX) / (1f - knotX),
        (0.82f - knotY) / (1f - knotY),
        (0.25f - knotX) / (1f - knotX),
        (1f - knotY) / (1f - knotY),
    )
    Easing { fraction ->
        if (fraction < knotX) {
            knotY * first.transform(fraction / knotX)
        } else {
            knotY + (1f - knotY) * second.transform((fraction - knotX) / (1f - knotX))
        }
    }
}

private val BackGestureEasing: Easing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)

private const val BOUNCE_STIFFNESS = 200f
private const val BOUNCE_DAMPING = 0.75f
private const val BOUNCE_MAX_KICK = 1000f
private const val BOUNCE_MIN_KICK = 120f

private fun bounceScale(settle: NavSettle?, gesture: NavGesture?): Float {
    if (settle == null || settle.phase != NavSettlePhase.Commit || gesture == null) return 1f
    val factor = if (gesture.swipeEdge != NavSwipeEdge.None) 2f else 1f
    val floorKick = if (gesture.progress < 0.1f) BOUNCE_MIN_KICK else 0f
    val kick = (abs(settle.releaseVelocity) * 100f * (1f - CROSS_ACTIVITY_MIN_SCALE) * factor)
        .coerceIn(floorKick, BOUNCE_MAX_KICK)
    if (kick <= 0f) return 1f
    val omega = sqrt(BOUNCE_STIFFNESS)
    val omegaD = omega * sqrt(1f - BOUNCE_DAMPING * BOUNCE_DAMPING)
    val t = settle.elapsedMillis / 1000f
    val overlay = -(kick / omegaD) * exp(-BOUNCE_DAMPING * omega * t) * sin(omegaD * t)
    return ((100f + overlay) / 100f).coerceAtMost(1f)
}

private fun shapedTopProgress(progress: Float, gesture: NavGesture?): Float =
    if (gesture == null) progress else 1f - BackGestureEasing.transform((1f - progress).coerceIn(0f, 1f))

private const val OPEN_FADE_START = 0.12f
private const val OPEN_FADE_SPAN = 0.71f
private const val CLOSE_FADE_START = 0.21f
private const val CLOSE_FADE_SPAN = 0.74f
private const val CLASSIC_FADE_DURATION = 83f
private const val OPEN_FADE_OFFSET = 50f
private const val CLOSE_FADE_OFFSET = 35f

private val ClassicMotion = NavMotion(
    programmatic = NavSettleSpec.Tween(durationMillis = 450, easing = FastOutExtraSlowIn),
)

private val ScaleMotion = NavMotion(
    commit = NavSettleSpec.Tween(durationMillis = 200, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
    cancel = NavSettleSpec.Spring(stiffness = 1500f),
    programmatic = NavSettleSpec.Tween(durationMillis = 200, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
)

private val ScaleAospExitMotion = NavMotion(
    commit = NavSettleSpec.Tween(durationMillis = 450, easing = FastOutExtraSlowIn),
    cancel = NavSettleSpec.Spring(stiffness = 1500f),
    programmatic = NavSettleSpec.Tween(durationMillis = 200, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
)

private val ClassicScalePop: NavTransition = object : NavTransition {
    override val opaqueDepth: Float = 1f

    override val motion: NavMotion = ScaleMotion

    override fun scrimFraction(scope: NavTransitionScope): Float = coverProgress(scope.relativeDepth)

    override fun Modifier.transformEntry(scope: NavTransitionScope): Modifier {
        val zIndex = if (scope.relativeDepth > 0f) 1f else 0f
        return graphicsLayer {
            val depth = scope.relativeDepth
            val widthPx = scope.layoutSize.width.toFloat()
            if (depth <= 0f) {
                val progress = topProgress(depth)
                scaleX = 0.9f + 0.1f * progress
                scaleY = scaleX
                alpha = progress
            } else {
                translationX = -coverProgress(depth) * widthPx
            }
        }.zIndex(zIndex)
    }
}

private val ClassicScaleTransition: NavTransition = navDirectionalTransition(
    push = NavTransitions.MiuixDefault,
    pop = ClassicScalePop,
    predictivePop = ClassicScalePop,
)

private fun scaleTransition(exitDirection: PredictiveBackExitDirection): NavTransition {
    val pop = navGraphicsTransition(
        opaqueDepth = 1f,
        motion = ScaleAospExitMotion,
        scrim = { scope ->
            when {
                scope.settle?.phase == NavSettlePhase.Commit -> (1f - (scope.settle?.elapsedMillis ?: 0f) / 450f).coerceIn(0f, 1f)
                scope.gesture != null -> 1f
                else -> coverProgress(scope.relativeDepth)
            }
        },
    ) { scope ->
        val depth = scope.relativeDepth
        val widthPx = scope.layoutSize.width.toFloat()
        val driftPx = with(scope.density) { CrossActivityDrift.toPx() }
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
            scaleX = pageScale
            scaleY = pageScale
            transformOrigin = TransformOrigin(
                pivotFractionX = if (gesture?.swipeEdge == NavSwipeEdge.Left) 0.8f else 0.2f,
                pivotFractionY = gesturePivotY(gesture, scope.layoutSize.height.toFloat()),
            )
            translationX = if (gesture != null && scope.settle == null) {
                0f
            } else if (outgoingCommit) {
                val releaseProgress = (1f - gesture.progress).coerceAtLeast(0.01f)
                val post = (1f - progress / releaseProgress).coerceIn(0f, 1f)
                sign * post * driftPx
            } else {
                sign * (1f - progress) * widthPx
            }
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

private val ClassicActivityOpen: NavTransition = navGraphicsTransition(
    motion = ClassicMotion,
    scrim = { 0f },
) { scope ->
    val depth = scope.relativeDepth
    val driftPx = with(scope.density) { CrossActivityDrift.toPx() }
    if (depth <= 0f) {
        val progress = topProgress(depth)
        translationX = (1f - progress) * driftPx
        alpha = if (scope.role == NavRole.Incoming) {
            val settle = scope.settle
            if (settle != null) {
                ((settle.elapsedMillis - OPEN_FADE_OFFSET) / CLASSIC_FADE_DURATION).coerceIn(0f, 1f)
            } else {
                ((progress - OPEN_FADE_START) / OPEN_FADE_SPAN).coerceIn(0f, 1f)
            }
        } else {
            1f
        }
    } else {
        translationX = -coverProgress(depth) * driftPx
    }
}

private val ClassicActivityClose: NavTransition = navGraphicsTransition(
    motion = ClassicMotion,
    scrim = { 0f },
) { scope ->
    val depth = scope.relativeDepth
    val driftPx = with(scope.density) { CrossActivityDrift.toPx() }
    if (depth <= 0f) {
        val progress = topProgress(depth)
        translationX = (1f - progress) * driftPx
        alpha = if (scope.role == NavRole.Outgoing) {
            val settle = scope.settle
            if (settle != null) {
                (1f - (settle.elapsedMillis - CLOSE_FADE_OFFSET) / CLASSIC_FADE_DURATION).coerceIn(0f, 1f)
            } else {
                ((progress - CLOSE_FADE_START) / CLOSE_FADE_SPAN).coerceIn(0f, 1f)
            }
        } else {
            1f
        }
    } else {
        translationX = -coverProgress(depth) * driftPx
    }
}

private val CrossActivityPredictive: NavTransition = navGraphicsTransition(
    opaqueDepth = 1f,
    motion = NavMotion(
        commit = NavSettleSpec.Tween(durationMillis = 450, easing = FastOutExtraSlowIn),
        cancel = NavSettleSpec.Spring(stiffness = 1500f),
    ),
    scrim = { scope ->
        val settle = scope.settle
        val gesture = scope.gesture
        when {
            settle?.phase == NavSettlePhase.Commit -> (1f - settle.elapsedMillis / 450f).coerceIn(0f, 1f)
            gesture != null -> (scope.relativeDepth.coerceIn(0f, 1f) / (1f - gesture.progress).coerceAtLeast(0.01f)).coerceIn(0f, 1f)
            else -> scope.relativeDepth.coerceIn(0f, 1f)
        }
    },
) { scope ->
    val depth = scope.relativeDepth
    val gesture = scope.gesture
    val settle = scope.settle
    val committing = settle?.phase == NavSettlePhase.Commit
    val widthPx = scope.layoutSize.width.toFloat()
    val driftPx = with(scope.density) { CrossActivityDrift.toPx() }
    val bounce = bounceScale(settle, gesture)
    val hugMax = (
        widthPx * (1f - CROSS_ACTIVITY_MIN_SCALE) / 2f -
            with(scope.density) { CrossActivityEdgeMargin.toPx() }
        ).coerceAtLeast(0f)
    val hugs = gesture?.swipeEdge != NavSwipeEdge.Right
    if (depth <= 0f) {
        val progress = topProgress(depth)
        if (scope.role == NavRole.Outgoing && committing && gesture != null) {
            val releaseProgress = (1f - gesture.progress).coerceAtLeast(0.01f)
            val post = (1f - progress / releaseProgress).coerceIn(0f, 1f)
            val releaseEasedProgress = shapedTopProgress(releaseProgress, gesture)
            val committedScale = CROSS_ACTIVITY_MIN_SCALE + (1f - CROSS_ACTIVITY_MIN_SCALE) * releaseEasedProgress
            val grown = committedScale + (1f - committedScale) * post
            scaleX = grown * bounce
            scaleY = scaleX
            var tx = if (hugs) (1f - releaseEasedProgress) * hugMax else 0f
            tx += post * driftPx
            alpha = (1f - 5f * (settle.elapsedMillis / 450f)).coerceAtLeast(0f)
            translationX = tx
            translationY = crossActivityYShift(gesture, scope.layoutSize.height.toFloat(), scaleX, scope.density)
        } else {
            val easedProgress = shapedTopProgress(progress, gesture)
            scaleX = (CROSS_ACTIVITY_MIN_SCALE + (1f - CROSS_ACTIVITY_MIN_SCALE) * easedProgress) * bounce
            scaleY = scaleX
            translationX = if (hugs) (1f - easedProgress) * hugMax else 0f
            alpha = when {
                scope.role == NavRole.Outgoing && gesture != null -> {
                    val releaseProgress = (1f - gesture.progress).coerceAtLeast(0.01f)
                    (1f - (1f - progress / releaseProgress).coerceIn(0f, 1f) * 3.5f).coerceAtLeast(0f)
                }
                gesture != null -> 1f
                else -> (progress / 0.2f).coerceIn(0f, 1f)
            }
            translationY = crossActivityYShift(gesture, scope.layoutSize.height.toFloat(), scaleX, scope.density)
        }
    } else {
        val cover = coverProgress(depth)
        val post = if (gesture != null) {
            val releaseProgress = gesture.progress
            if (releaseProgress >= 1f) 1f else (((1f - cover) - releaseProgress) / (1f - releaseProgress)).coerceIn(0f, 1f)
        } else {
            1f - cover
        }
        translationX = -(1f - post) * driftPx
        if (gesture != null) {
            val travel = if (committing) gesture.progress else (1f - cover)
            val eased = BackGestureEasing.transform(travel.coerceIn(0f, 1f))
            val liveScale = CROSS_ACTIVITY_MIN_SCALE + (1f - CROSS_ACTIVITY_MIN_SCALE) * (1f - eased)
            scaleX = (liveScale + (1f - liveScale) * post) * bounce
            scaleY = scaleX
        }
        translationY = crossActivityYShift(gesture, scope.layoutSize.height.toFloat(), scaleX, scope.density)
    }
}

private val CrossActivityTransition: NavTransition = navDirectionalTransition(
    push = ClassicActivityOpen,
    pop = ClassicActivityClose,
    predictivePop = CrossActivityPredictive,
)

private fun crossActivityYShift(
    gesture: NavGesture?,
    height: Float,
    scale: Float,
    density: Density,
): Float {
    if (gesture == null || height <= 0f) return 0f
    val rawDelta = gesture.touchY - gesture.initialTouchY
    val half = height / 2f
    val ratio = min(half, abs(rawDelta)) / half
    val damped = 1f - (1f - ratio) * (1f - ratio)
    val marginPx = with(density) { CrossActivityEdgeMargin.toPx() }
    val maxShift = ((height - height * scale) / 2f - marginPx).coerceAtLeast(0f)
    return maxShift * damped * (if (rawDelta < 0f) -1f else 1f)
}

private fun gesturePivotY(gesture: NavGesture?, height: Float): Float =
    if (gesture != null && height > 0f) {
        (gesture.touchY / height).coerceIn(0.1f, 0.9f)
    } else {
        0.5f
    }

private fun exitDirectionSign(
    direction: PredictiveBackExitDirection,
    scope: NavTransitionScope,
): Float {
    return when (direction) {
        PredictiveBackExitDirection.FOLLOW_GESTURE -> if (scope.gesture?.swipeEdge == NavSwipeEdge.Left) 1f else -1f
        PredictiveBackExitDirection.ALWAYS_RIGHT -> 1f
        PredictiveBackExitDirection.ALWAYS_LEFT -> -1f
    }
}

private fun topProgress(depth: Float): Float = (1f + depth).coerceIn(0f, 1f)

private fun coverProgress(depth: Float): Float = depth.coerceIn(0f, 1f)

private const val SCALE_MIN = 0.85f
private const val CROSS_ACTIVITY_MIN_SCALE = 0.9f
private val CrossActivityDrift = 96.dp
private val CrossActivityEdgeMargin = 8.dp
