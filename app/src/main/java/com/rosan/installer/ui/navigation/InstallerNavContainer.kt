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
import androidx.compose.runtime.remember
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
import com.rosan.installer.ui.page.main.settings.SettingsSharedViewModel
import com.rosan.installer.ui.page.main.settings.config.apply.ApplyPage
import com.rosan.installer.ui.page.main.settings.config.edit.EditPage
import com.rosan.installer.ui.page.main.settings.home.installer.DefaultInstallerPage
import com.rosan.installer.ui.page.main.settings.home.priv.PrivPage
import com.rosan.installer.ui.page.main.settings.preferred.about.AboutPage
import com.rosan.installer.ui.page.main.settings.preferred.about.OpenSourceLicensePage
import com.rosan.installer.ui.page.main.settings.preferred.installer.InstallerGlobalSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.installer.authorizer.AuthorizerCustPage
import com.rosan.installer.ui.page.main.settings.preferred.installer.dialog.DialogSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.installer.notification.NotificationSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.lab.LabPage
import com.rosan.installer.ui.page.main.settings.preferred.network.NetworkPage
import com.rosan.installer.ui.page.main.settings.preferred.theme.ThemeSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.uninstaller.UninstallerGlobalSettingsPage
import com.rosan.installer.ui.page.miuix.settings.config.apply.MiuixApplyPage
import com.rosan.installer.ui.page.miuix.settings.config.edit.MiuixEditPage
import com.rosan.installer.ui.page.miuix.settings.home.installer.MiuixDefaultInstallerPage
import com.rosan.installer.ui.page.miuix.settings.home.priv.MiuixPrivPage
import com.rosan.installer.ui.page.miuix.settings.preferred.about.MiuixAboutPage
import com.rosan.installer.ui.page.miuix.settings.preferred.about.ossLicensePage.MiuixOpenSourceLicensePage
import com.rosan.installer.ui.page.miuix.settings.preferred.installer.MiuixInstallerGlobalSettingsPage
import com.rosan.installer.ui.page.miuix.settings.preferred.installer.authorizer.MiuixAuthorizerCustPage
import com.rosan.installer.ui.page.miuix.settings.preferred.installer.dialog.MiuixDialogSettingsPage
import com.rosan.installer.ui.page.miuix.settings.preferred.installer.notification.MiuixNotificationSettingsPage
import com.rosan.installer.ui.page.miuix.settings.preferred.lab.MiuixLabPage
import com.rosan.installer.ui.page.miuix.settings.preferred.network.MiuixNetworkPage
import com.rosan.installer.ui.page.miuix.settings.preferred.theme.MiuixThemeSettingsPage
import com.rosan.installer.ui.page.miuix.settings.preferred.uninstaller.MiuixUninstallerGlobalSettingsPage
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
    val useBlur = uiState.useBlur

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
            entry<Route.Main> {
                InstallerNavEntry(pageTheme, interceptPredictiveBack, onBack) {
                    if (uiState.useMiuix) {
                        MiuixMainPageWrapper(uiState, sharedViewModel)
                    } else {
                        Material3MainPageWrapper(uiState, sharedViewModel)
                    }
                }
            }
            entry<Route.EditConfig>(swipeDismiss = NavSwipeDirection.None) { key ->
                InstallerNavEntry(pageTheme, interceptPredictiveBack, onBack, hostGlassDropdowns = true) {
                    val id = key.id
                    if (uiState.useMiuix) {
                        MiuixEditPage(
                            id = if (id != -1L) id else null,
                            useBlur = useBlur,
                        )
                    } else {
                        EditPage(
                            id = if (id != -1L) id else null,
                            useBlur = useBlur,
                        )
                    }
                }
            }
            entry<Route.ApplyConfig>(swipeDismiss = swipeBackDirection) { key ->
                InstallerNavEntry(pageTheme, interceptPredictiveBack, onBack) {
                    val id = key.id
                    if (uiState.useMiuix) {
                        MiuixApplyPage(id, useBlur)
                    } else {
                        ApplyPage(id, useBlur)
                    }
                }
            }
            entry<Route.About>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(pageTheme, interceptPredictiveBack, onBack) {
                    if (uiState.useMiuix) {
                        MiuixAboutPage(useBlur)
                    } else {
                        AboutPage(useBlur)
                    }
                }
            }
            entry<Route.OpenSourceLicense>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(pageTheme, interceptPredictiveBack, onBack) {
                    if (uiState.useMiuix) {
                        MiuixOpenSourceLicensePage(useBlur)
                    } else {
                        OpenSourceLicensePage(useBlur)
                    }
                }
            }
            entry<Route.Theme>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(pageTheme, interceptPredictiveBack, onBack) {
                    if (uiState.useMiuix) {
                        MiuixThemeSettingsPage()
                    } else {
                        ThemeSettingsPage()
                    }
                }
            }
            entry<Route.Network>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(pageTheme, interceptPredictiveBack, onBack, hostGlassDropdowns = true) {
                    if (uiState.useMiuix) {
                        MiuixNetworkPage(useBlur)
                    } else {
                        NetworkPage(useBlur)
                    }
                }
            }
            entry<Route.InstallerGlobal>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(pageTheme, interceptPredictiveBack, onBack, hostGlassDropdowns = true) {
                    if (uiState.useMiuix) {
                        MiuixInstallerGlobalSettingsPage(useBlur)
                    } else {
                        InstallerGlobalSettingsPage(useBlur)
                    }
                }
            }
            entry<Route.AuthorizerCust>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(pageTheme, interceptPredictiveBack, onBack) {
                    if (uiState.useMiuix) {
                        MiuixAuthorizerCustPage(useBlur)
                    } else {
                        AuthorizerCustPage(useBlur)
                    }
                }
            }
            entry<Route.DialogSettings>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(pageTheme, interceptPredictiveBack, onBack, hostGlassDropdowns = true) {
                    if (uiState.useMiuix) {
                        MiuixDialogSettingsPage(useBlur)
                    } else {
                        DialogSettingsPage(useBlur)
                    }
                }
            }
            entry<Route.NotificationSettings>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(pageTheme, interceptPredictiveBack, onBack, hostGlassDropdowns = true) {
                    if (uiState.useMiuix) {
                        MiuixNotificationSettingsPage(useBlur)
                    } else {
                        NotificationSettingsPage(useBlur)
                    }
                }
            }
            entry<Route.UninstallerGlobal>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(pageTheme, interceptPredictiveBack, onBack) {
                    if (uiState.useMiuix) {
                        MiuixUninstallerGlobalSettingsPage(useBlur)
                    } else {
                        UninstallerGlobalSettingsPage(useBlur)
                    }
                }
            }
            entry<Route.Lab>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(pageTheme, interceptPredictiveBack, onBack, hostGlassDropdowns = true) {
                    if (uiState.useMiuix) {
                        MiuixLabPage(useBlur)
                    } else {
                        LabPage(useBlur)
                    }
                }
            }
            entry<Route.DefaultInstaller>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(pageTheme, interceptPredictiveBack, onBack) {
                    if (uiState.useMiuix) {
                        MiuixDefaultInstallerPage(useBlur)
                    } else {
                        DefaultInstallerPage(useBlur)
                    }
                }
            }
            entry<Route.Priv>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(pageTheme, interceptPredictiveBack, onBack) {
                    if (uiState.useMiuix) {
                        MiuixPrivPage(useBlur)
                    } else {
                        PrivPage(useBlur)
                    }
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
    pageTheme {
        if (hostGlassDropdowns && InstallerTheme.useMiuixOs4) {
            InstallerMiuixComponentHost(
                useOs4 = true,
                useBlur = true,
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
            ) { scope ->
                Box(modifier = Modifier.fillMaxSize().then(scope.pageBackdropModifier)) {
                    content()
                }
            }
        } else {
            content()
        }
    }
}
