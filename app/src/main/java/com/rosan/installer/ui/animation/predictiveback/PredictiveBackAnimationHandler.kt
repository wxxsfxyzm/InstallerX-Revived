// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.animation.predictiveback

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventTransitionState

interface PredictiveBackAnimationHandler {
    /**
     * Callback invoked when the back event is committed (e.g., gesture completed or button clicked).
     *
     * **Implementation Requirements:**
     * - Implementation must check the current state of [transitionState].
     * - If a predictive back animation is active (in-progress), this method must play the animations
     * to avoid page disappear without any Exit animations
     * - This serves as the terminal lifecycle hook before the Navigation Manager
     * officially removes the page from the backstack.
     *
     * @param transitionState The state tracking the current predictive back gesture/animation.
     * @param currentPageKey The [NavKey] of the page currently being popped.
     */
    suspend fun onBackPressed(
        transitionState: NavigationEventTransitionState?,
        currentPageKey: NavKey?,
    )

    /**
     * A UI decorator applied to every page during the rendering process.
     * * Allows for custom modifications to the page layout or graphics layer.
     *
     * @param transitionState The current state of the predictive back transition.
     * @param contentPageKey The [NavKey] of the page being decorated.
     * @param currentPageKey The [NavKey]'s toString of the page currently at the top of the stack.
     * @return the Modifier will apply to the Box of the content
     */
    @Composable
    fun Modifier.predictiveBackAnimationDecorator(
        transitionState: NavigationEventTransitionState?,
        contentPageKey: Any,
        currentPageKey: NavKey?,
    ): Modifier

    /**
     * Defines the transition specs specifically for a predictive back (swipe) gesture.
     * @param swipeEdge The edge from which the swipe gesture originated (Left or Right).
     * @return A [ContentTransform] defining the enter/exit animations.
     */
    fun AnimatedContentTransitionScope<Scene<NavKey>>.onPredictivePopTransitionSpec(
        @NavigationEvent.SwipeEdge swipeEdge: Int
    ): ContentTransform

    /**
     * Defines the transition specs for a standard pop navigation (e.g., non-gesture back).
     */
    fun AnimatedContentTransitionScope<Scene<NavKey>>.onPopTransitionSpec(): ContentTransform

    /**
     * Defines the default transition specs for forward navigation (push).
     */
    fun AnimatedContentTransitionScope<Scene<NavKey>>.onTransitionSpec(): ContentTransform
}