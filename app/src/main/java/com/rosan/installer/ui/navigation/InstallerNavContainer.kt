// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.navigation

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.rosan.installer.domain.settings.model.preferences.PredictiveBackAnimation
import com.rosan.installer.domain.settings.model.preferences.ThemeState
import com.rosan.installer.ui.animation.predictiveback.installerNavTransition
import com.rosan.installer.ui.family.host
import com.rosan.installer.ui.feature.settings.SettingsSharedViewModel
import com.rosan.installer.ui.page.miuix.widgets.InstallerMiuixComponentHost
import com.rosan.installer.ui.theme.InstallerTheme
import com.rosan.installer.ui.theme.SettingsPageTheme
import com.rosan.installer.ui.util.rememberDeviceCornerRadius
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.nav.core.NavCornerClipMode
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection

@Composable
internal fun InstallerNavContainer(uiState: ThemeState, backdropColor: Color, pageTheme: SettingsPageTheme) {
    val sharedViewModel: SettingsSharedViewModel = koinViewModel(
        viewModelStoreOwner = LocalActivity.current as ComponentActivity,
    )

    val backStack = rememberNavBackStack<Route>(Route.Main)
    val navigator = remember(backStack) { Navigator(backStack) }
    val onBack = remember(navigator) { { navigator.pop() } }
    val host = uiState.uiFamily.host()

    CompositionLocalProvider(
        LocalNavigator provides navigator,
    ) {
        val navCornerRadius = rememberDeviceCornerRadius(defaultRadius = 0.dp)
        val roundAllCorners = uiState.predictiveBackAnimation == PredictiveBackAnimation.AOSP ||
            uiState.predictiveBackAnimation == PredictiveBackAnimation.Scale ||
            uiState.predictiveBackAnimation == PredictiveBackAnimation.Classic
        val effects = remember(navCornerRadius, roundAllCorners, backdropColor) {
            NavDisplayEffects(
                enableCornerClip = true,
                cornerClipRadius = if (roundAllCorners && navCornerRadius <= 0.dp) 32.dp else navCornerRadius,
                cornerClipMode = if (roundAllCorners) NavCornerClipMode.All else NavCornerClipMode.Leading,
                dimAmount = 0.5f,
                backdropColor = backdropColor,
                blockInputDuringTransition = false,
            )
        }
        val transition = remember(uiState.predictiveBackAnimation, uiState.predictiveBackExitDirection) {
            installerNavTransition(
                animation = uiState.predictiveBackAnimation,
                exitDirection = uiState.predictiveBackExitDirection,
            )
        }
        val swipeBackDirection = when (LocalLayoutDirection.current) {
            LayoutDirection.Rtl -> NavSwipeDirection.RightToLeft
            LayoutDirection.Ltr -> NavSwipeDirection.LeftToRight
        }
        val interceptPredictiveBack =
            uiState.predictiveBackAnimation == PredictiveBackAnimation.None && backStack.size > 1

        NavDisplay(
            backStack = backStack,
            modifier = Modifier.fillMaxSize().background(backdropColor),
            onBack = onBack,
            transition = transition,
            effects = effects,
        ) {
            entry<Route.Main> { key ->
                InstallerNavEntry(pageTheme, interceptPredictiveBack, onBack) {
                    host.RouteContent(key, uiState, sharedViewModel)
                }
            }
            entry<Route.EditConfig>(swipeDismiss = NavSwipeDirection.None) { key ->
                InstallerNavEntry(pageTheme, interceptPredictiveBack, onBack, hostGlassDropdowns = true) {
                    host.RouteContent(key, uiState, sharedViewModel)
                }
            }
            entry<Route.ApplyConfig>(swipeDismiss = swipeBackDirection) { key ->
                InstallerNavEntry(pageTheme, interceptPredictiveBack, onBack) {
                    host.RouteContent(key, uiState, sharedViewModel)
                }
            }
            entry<Route.About>(swipeDismiss = swipeBackDirection) { key ->
                InstallerNavEntry(pageTheme, interceptPredictiveBack, onBack) {
                    host.RouteContent(key, uiState, sharedViewModel)
                }
            }
            entry<Route.OpenSourceLicense>(swipeDismiss = swipeBackDirection) { key ->
                InstallerNavEntry(pageTheme, interceptPredictiveBack, onBack) {
                    host.RouteContent(key, uiState, sharedViewModel)
                }
            }
            entry<Route.Theme>(swipeDismiss = swipeBackDirection) { key ->
                InstallerNavEntry(pageTheme, interceptPredictiveBack, onBack) {
                    host.RouteContent(key, uiState, sharedViewModel)
                }
            }
            entry<Route.Network>(swipeDismiss = swipeBackDirection) { key ->
                InstallerNavEntry(pageTheme, interceptPredictiveBack, onBack, hostGlassDropdowns = true) {
                    host.RouteContent(key, uiState, sharedViewModel)
                }
            }
            entry<Route.InstallerGlobal>(swipeDismiss = swipeBackDirection) { key ->
                InstallerNavEntry(pageTheme, interceptPredictiveBack, onBack, hostGlassDropdowns = true) {
                    host.RouteContent(key, uiState, sharedViewModel)
                }
            }
            entry<Route.AuthorizerCust>(swipeDismiss = swipeBackDirection) { key ->
                InstallerNavEntry(pageTheme, interceptPredictiveBack, onBack) {
                    host.RouteContent(key, uiState, sharedViewModel)
                }
            }
            entry<Route.DialogSettings>(swipeDismiss = swipeBackDirection) { key ->
                InstallerNavEntry(pageTheme, interceptPredictiveBack, onBack, hostGlassDropdowns = true) {
                    host.RouteContent(key, uiState, sharedViewModel)
                }
            }
            entry<Route.NotificationSettings>(swipeDismiss = swipeBackDirection) { key ->
                InstallerNavEntry(pageTheme, interceptPredictiveBack, onBack, hostGlassDropdowns = true) {
                    host.RouteContent(key, uiState, sharedViewModel)
                }
            }
            entry<Route.UninstallerGlobal>(swipeDismiss = swipeBackDirection) { key ->
                InstallerNavEntry(pageTheme, interceptPredictiveBack, onBack) {
                    host.RouteContent(key, uiState, sharedViewModel)
                }
            }
            entry<Route.Lab>(swipeDismiss = swipeBackDirection) { key ->
                InstallerNavEntry(pageTheme, interceptPredictiveBack, onBack, hostGlassDropdowns = true) {
                    host.RouteContent(key, uiState, sharedViewModel)
                }
            }
            entry<Route.DefaultInstaller>(swipeDismiss = swipeBackDirection) { key ->
                InstallerNavEntry(pageTheme, interceptPredictiveBack, onBack) {
                    host.RouteContent(key, uiState, sharedViewModel)
                }
            }
            entry<Route.Priv>(swipeDismiss = swipeBackDirection) { key ->
                InstallerNavEntry(pageTheme, interceptPredictiveBack, onBack) {
                    host.RouteContent(key, uiState, sharedViewModel)
                }
            }
        }
    }
}

@Composable
private fun InstallerNavEntry(
    pageTheme: SettingsPageTheme,
    interceptPredictiveBack: Boolean,
    onBack: () -> Unit,
    hostGlassDropdowns: Boolean = false,
    content: @Composable () -> Unit,
) {
    val navigationEventState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = interceptPredictiveBack,
        onBackCompleted = onBack,
    )
    // Move the page between the glass host and the plain container so OS4 changes keep its state.
    val currentContent by rememberUpdatedState(content)
    val movableContent = remember { movableContentOf { currentContent() } }
    pageTheme {
        if (hostGlassDropdowns && InstallerTheme.useMiuixOs4) {
            InstallerMiuixComponentHost(
                useOs4 = true,
                useBlur = true,
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
            ) { scope ->
                Box(modifier = Modifier.fillMaxSize().then(scope.pageBackdropModifier)) {
                    movableContent()
                }
            }
        } else {
            movableContent()
        }
    }
}
