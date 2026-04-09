// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.animation.predictiveback

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigationevent.NavigationEventTransitionState
import com.rosan.installer.ui.util.rememberDeviceCornerRadius

// TODO Add an config page for user to select predictiveBack implement
class KernelSUOfficialPredictiveBackAnimation : PredictiveBackAnimationHandler {

    // Explicitly track the keys of the top and bottom pages to accurately manage
    // the z-index hierarchy and apply the dimming overlay to the correct layer.
    private var topPageKey by mutableStateOf<String?>(null)
    private var bottomPageKey by mutableStateOf<String?>(null)

    override suspend fun onBackPressed(
        transitionState: NavigationEventTransitionState?,
        currentPageKey: NavKey?
    ) {
        // Deliberately empty. Predictive back gesture progress is natively handled
        // and synchronized by the Compose transition engine within the Decorator.
    }

    @Composable
    override fun Modifier.predictiveBackAnimationDecorator(
        transitionState: NavigationEventTransitionState?,
        contentPageKey: Any,
        currentPageKey: NavKey?,
    ): Modifier {
        val navContent = LocalNavAnimatedContentScope.current
        val transition = navContent.transition
        val pageKey = contentPageKey.toString()

        // Determine if the current composition represents the background (bottom) page.
        // Fallback to evaluating against the current active page key if explicit tracking is unset.
        val isBottomPage = when {
            bottomPageKey != null -> pageKey == bottomPageKey
            topPageKey != null -> pageKey != topPageKey
            else -> pageKey != currentPageKey?.toString()
        }

        // Apply device-specific corner radius exclusively to the foreground (top) page.
        val deviceCornerRadius = rememberDeviceCornerRadius()
        val cornerRadius = if (!isBottomPage) deviceCornerRadius else 0.dp

        // Animate the alpha of the physical dimming overlay.
        // This leverages the native transition state to perfectly sync with both
        // standard navigation changes and predictive back gesture interpolations.
        val dimAlpha by transition.animateFloat(
            transitionSpec = { tween(durationMillis = 500, easing = FastOutSlowInEasing) },
            label = "DimOverlayAlpha"
        ) { state ->
            // Apply a maximum 0.5f alpha black scrim strictly to the bottom page
            // when it is transitioning in or out (not fully visible).
            if (isBottomPage && state != EnterExitState.Visible) {
                0.5f
            } else {
                0f
            }
        }

        return this
            .clip(RoundedCornerShape(cornerRadius))
            .drawWithContent {
                drawContent()
                // Render the physical dimming scrim over the fully opaque content
                if (dimAlpha > 0f) {
                    drawRect(color = Color.Black, alpha = dimAlpha)
                }
            }
    }

    override fun AnimatedContentTransitionScope<Scene<NavKey>>.onPredictivePopTransitionSpec(
        swipeEdge: Int
    ): ContentTransform {
        // Track states for a pop transition: the initial state is exiting (top),
        // and the target state is entering (bottom).
        topPageKey = initialState.key.toString()
        bottomPageKey = targetState.key.toString()

        return ContentTransform(
            targetContentEnter = slideInHorizontally(
                initialOffsetX = { -it / 5 },
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            ),
            initialContentExit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            ),
            targetContentZIndex = -1f,
            sizeTransform = null
        )
    }

    override fun AnimatedContentTransitionScope<Scene<NavKey>>.onPopTransitionSpec(): ContentTransform {
        topPageKey = initialState.key.toString()
        bottomPageKey = targetState.key.toString()

        return ContentTransform(
            targetContentEnter = slideInHorizontally(
                initialOffsetX = { -it / 5 },
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            ),
            initialContentExit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            ),
            targetContentZIndex = -1f,
            sizeTransform = null
        )
    }

    override fun AnimatedContentTransitionScope<Scene<NavKey>>.onTransitionSpec(): ContentTransform {
        // Track states for a push transition: the new target state becomes the top,
        // pushing the initial state to the bottom.
        topPageKey = targetState.key.toString()
        bottomPageKey = initialState.key.toString()

        return ContentTransform(
            targetContentEnter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            ),
            initialContentExit = slideOutHorizontally(
                targetOffsetX = { -it / 5 },
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            ),
            targetContentZIndex = 1f,
            sizeTransform = null
        )
    }
}