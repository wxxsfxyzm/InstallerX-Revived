// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 wxxsfxyzm
package com.rosan.installer.ui.animation.predictiveback

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigationevent.NavigationEvent.Companion.EDGE_LEFT
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.NavigationEventTransitionState.InProgress
import com.rosan.installer.domain.settings.model.PredictiveBackExitDirection
import com.rosan.installer.ui.util.rememberDeviceCornerRadius
import timber.log.Timber

class AOSPCrossActivityAnimation(
    private val exitDirection: PredictiveBackExitDirection = PredictiveBackExitDirection.ALWAYS_RIGHT
) : PredictiveBackAnimationHandler {
    private var exitingPageKey: String? = null
    private val exitAnimatable = Animatable(0f)

    override suspend fun onBackPressed(
        transitionState: NavigationEventTransitionState?,
        currentPageKey: NavKey?,
    ) {
        if (transitionState is InProgress) {
            exitAnimatable.snapTo(0f)

            exitingPageKey = currentPageKey.toString()
            Timber.d("[BackAnim] onBackPressed: exitingPageKey='$exitingPageKey'")

            exitAnimatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 450, easing = LinearEasing)
            )
            Timber.d("[BackAnim] animateTo(1f) complete, value=${exitAnimatable.value}")
        }
    }

    @Composable
    override fun Modifier.predictiveBackAnimationDecorator(
        transitionState: NavigationEventTransitionState?,
        contentPageKey: Any,
        currentPageKey: NavKey?,
    ): Modifier = composed { // 【核心修复】使用 composed { } 包装，确保 DisposableEffect 安全挂载
        val windowInfo = LocalWindowInfo.current
        val containerHeightPx = windowInfo.containerSize.height
        val pageKey = contentPageKey.toString()
        val deviceCornerRadius = rememberDeviceCornerRadius()

        // 完美生命周期清理：当旧页面被 Navigation 真正从 UI 树拔除时，无缝清理状态
        DisposableEffect(pageKey) {
            onDispose {
                if (exitingPageKey == pageKey) {
                    exitingPageKey = null
                }
            }
        }

        val enteringStartOffsetPx = with(LocalDensity.current) { 96.dp.toPx() }

        val linearProgress = exitAnimatable.value.coerceAtMost(1f)
        // 与 AOSP 保持一致的 Interpolators.EMPHASIZED
        val emphasizedProgress = CubicBezierEasing(0.2f, 0f, 0f, 1f).transform(linearProgress)

        val progressInProgress = (transitionState as? InProgress)
        val edge = progressInProgress?.latestEvent?.swipeEdge ?: 0
        val touchY = progressInProgress?.latestEvent?.touchY
        val gestureProgress = progressInProgress?.latestEvent?.progress ?: 0f

        val directionMultiplier = when (exitDirection) {
            PredictiveBackExitDirection.FOLLOW_GESTURE -> if (edge == EDGE_LEFT) 1f else -1f
            PredictiveBackExitDirection.ALWAYS_RIGHT -> 1f
            PredictiveBackExitDirection.ALWAYS_LEFT -> -1f
        }

        val isExitingPage = exitingPageKey != null && exitingPageKey == pageKey
        val isCurrentNavTarget = exitingPageKey == null && pageKey == currentPageKey.toString()

        // 【核心修复】无论当前是拖拽还是松手，统一用手势进度计算 dragScale！
        // 这样松手瞬间，接力棒交接的 scale 值完全一致，彻底杜绝跳闪。
        val maxScale = 0.85f
        val dragScale = 1f - (1f - maxScale) * gestureProgress

        val currentPivotY = if (touchY != null && containerHeightPx > 0) {
            (touchY / containerHeightPx).coerceIn(0.1f, 0.9f)
        } else 0.5f
        val currentPivotX = if (edge == EDGE_LEFT) 0.8f else 0.2f

        this
            .graphicsLayer {
                transformOrigin = TransformOrigin(currentPivotX, currentPivotY)

                when {
                    isExitingPage -> {
                        // --- 退出上层页 (Post-commit) ---
                        // AOSP逻辑：继续从拖拽最后的大小缩小至 0.85，向右偏移，并在前 20% 迅速透明
                        val computedScaleX = dragScale + (maxScale - dragScale) * emphasizedProgress
                        val computedTranslationX = enteringStartOffsetPx * directionMultiplier * emphasizedProgress
                        val computedAlpha = if (linearProgress >= 0.2f) 0f else (1f - linearProgress * 5f).coerceAtLeast(0f)

                        scaleX = computedScaleX
                        scaleY = computedScaleX
                        translationX = computedTranslationX
                        alpha = computedAlpha
                    }

                    isCurrentNavTarget -> {
                        // --- 拖拽中的上层页 (Pre-commit) ---
                        // 抛弃 Navigation 自带的 animatedScale，直接使用精准的 dragScale
                        scaleX = dragScale
                        scaleY = dragScale
                        translationX = 0f
                        alpha = 1f
                    }

                    else -> {
                        // --- 进入的下层页 (Pre/Post-commit) ---
                        val initialTranslationX = -enteringStartOffsetPx * directionMultiplier

                        if (exitingPageKey != null) {
                            // 松手后：从 dragScale 放大回 1.0f，同时从 -96dp 滑动到 0
                            scaleX = dragScale + (1f - dragScale) * emphasizedProgress
                            scaleY = dragScale + (1f - dragScale) * emphasizedProgress
                            translationX = initialTranslationX * (1f - emphasizedProgress)
                            alpha = 1f
                        } else if (transitionState is InProgress) {
                            // 拖拽中：跟随一起缩放，固定在 -96dp 偏移处
                            scaleX = dragScale
                            scaleY = dragScale
                            translationX = initialTranslationX
                            alpha = 1f
                        }
                    }
                }
            }
            .clip(
                if (isExitingPage || isCurrentNavTarget) RoundedCornerShape(deviceCornerRadius)
                else RoundedCornerShape(0.dp)
            )
    }

    override fun AnimatedContentTransitionScope<Scene<NavKey>>.onPredictivePopTransitionSpec(
        swipeEdge: Int
    ): ContentTransform = ContentTransform(
        targetContentEnter = EnterTransition.None,
        initialContentExit = ExitTransition.None,
        sizeTransform = null
    )

    override fun AnimatedContentTransitionScope<Scene<NavKey>>.onPopTransitionSpec(): ContentTransform =
        ContentTransform(
            targetContentEnter = slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn(),
            initialContentExit = scaleOut(targetScale = 0.9f) + fadeOut(),
            sizeTransform = null
        )

    override fun AnimatedContentTransitionScope<Scene<NavKey>>.onTransitionSpec(): ContentTransform =
        ContentTransform(
            targetContentEnter = slideInHorizontally(initialOffsetX = { it }),
            initialContentExit = fadeOut(),
            sizeTransform = null
        )
}
