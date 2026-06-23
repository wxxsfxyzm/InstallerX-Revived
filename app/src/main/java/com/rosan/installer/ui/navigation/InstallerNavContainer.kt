// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.navigation

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
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
import com.rosan.installer.ui.page.miuix.settings.preferred.theme.MiuixThemeSettingsPage
import com.rosan.installer.ui.page.miuix.settings.preferred.uninstaller.MiuixUninstallerGlobalSettingsPage
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.nav.core.NavCornerClipMode
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection

@Composable
fun InstallerNavContainer(uiState: ThemeState) {
    val sharedViewModel: SettingsSharedViewModel = koinViewModel(
        viewModelStoreOwner = LocalActivity.current as ComponentActivity
    )

    val backStack = rememberNavBackStack<Route>(Route.Main)
    val navigator = remember(backStack) { Navigator(backStack) }
    val useBlur = uiState.useBlur

    CompositionLocalProvider(
        LocalNavigator provides navigator,
    ) {
        val navCornerRadius = rememberNavSystemCornerRadius()
        val effects = remember(navCornerRadius) {
            NavDisplayEffects(
                enableCornerClip = true,
                cornerClipRadius = navCornerRadius,
                cornerClipMode = NavCornerClipMode.Leading,
                dimAmount = 0.5f,
                blockInputDuringTransition = true,
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

        NavDisplay(
            backStack = backStack,
            onBack = { navigator.pop() },
            transition = transition,
            effects = effects,
        ) {
            entry<Route.Main> {
                if (uiState.useMiuix) {
                    MiuixMainPageWrapper(uiState, sharedViewModel)
                } else {
                    Material3MainPageWrapper(uiState, sharedViewModel)
                }
            }
            entry<Route.EditConfig>(swipeDismiss = swipeBackDirection) { key ->
                val id = key.id
                if (uiState.useMiuix) {
                    MiuixEditPage(
                        id = if (id != -1L) id else null,
                        useBlur = useBlur
                    )
                } else {
                    EditPage(
                        id = if (id != -1L) id else null,
                        useBlur = useBlur
                    )
                }
            }
            entry<Route.ApplyConfig>(swipeDismiss = swipeBackDirection) { key ->
                val id = key.id
                if (uiState.useMiuix) {
                    MiuixApplyPage(id, useBlur)
                } else {
                    ApplyPage(id, useBlur)
                }
            }
            entry<Route.About>(swipeDismiss = swipeBackDirection) {
                if (uiState.useMiuix) {
                    MiuixAboutPage(useBlur)
                } else {
                    AboutPage(useBlur)
                }
            }
            entry<Route.OpenSourceLicense>(swipeDismiss = swipeBackDirection) {
                if (uiState.useMiuix) {
                    MiuixOpenSourceLicensePage(useBlur)
                } else {
                    OpenSourceLicensePage(useBlur)
                }
            }
            entry<Route.Theme>(swipeDismiss = swipeBackDirection) {
                if (uiState.useMiuix) {
                    MiuixThemeSettingsPage()
                } else {
                    ThemeSettingsPage()
                }
            }
            entry<Route.InstallerGlobal>(swipeDismiss = swipeBackDirection) {
                if (uiState.useMiuix) {
                    MiuixInstallerGlobalSettingsPage(useBlur)
                } else {
                    InstallerGlobalSettingsPage(useBlur)
                }
            }
            entry<Route.AuthorizerCust>(swipeDismiss = swipeBackDirection) {
                if (uiState.useMiuix) {
                    MiuixAuthorizerCustPage(useBlur)
                } else {
                    AuthorizerCustPage(useBlur)
                }
            }
            entry<Route.DialogSettings>(swipeDismiss = swipeBackDirection) {
                if (uiState.useMiuix) {
                    MiuixDialogSettingsPage(useBlur)
                } else {
                    DialogSettingsPage(useBlur)
                }
            }
            entry<Route.NotificationSettings>(swipeDismiss = swipeBackDirection) {
                if (uiState.useMiuix) {
                    MiuixNotificationSettingsPage(useBlur)
                } else {
                    NotificationSettingsPage(useBlur)
                }
            }
            entry<Route.UninstallerGlobal>(swipeDismiss = swipeBackDirection) {
                if (uiState.useMiuix) {
                    MiuixUninstallerGlobalSettingsPage(useBlur)
                } else {
                    UninstallerGlobalSettingsPage(useBlur)
                }
            }
            entry<Route.Lab>(swipeDismiss = swipeBackDirection) {
                if (uiState.useMiuix) {
                    MiuixLabPage(useBlur)
                } else {
                    LabPage(useBlur)
                }
            }
            entry<Route.DefaultInstaller>(swipeDismiss = swipeBackDirection) {
                if (uiState.useMiuix) {
                    MiuixDefaultInstallerPage(useBlur)
                } else {
                    DefaultInstallerPage(useBlur)
                }
            }
            entry<Route.Priv>(swipeDismiss = swipeBackDirection) {
                if (uiState.useMiuix) {
                    MiuixPrivPage(useBlur)
                } else {
                    PrivPage(useBlur)
                }
            }
        }
    }
}
