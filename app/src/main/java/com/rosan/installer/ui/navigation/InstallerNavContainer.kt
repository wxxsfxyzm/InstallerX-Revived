// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.navigation

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.itos.coui.theme.CouiTheme
import com.rosan.installer.domain.settings.model.preferences.PredictiveBackAnimation
import com.rosan.installer.domain.settings.model.preferences.ThemeState
import com.rosan.installer.domain.settings.model.preferences.theme.UiStyle
import com.rosan.installer.ui.animation.predictiveback.installerNavTransition
import com.rosan.installer.ui.page.coui.settings.config.apply.CouiApplyPage
import com.rosan.installer.ui.page.coui.settings.config.edit.CouiEditPage
import com.rosan.installer.ui.page.coui.settings.home.installer.CouiDefaultInstallerPage
import com.rosan.installer.ui.page.coui.settings.home.priv.CouiPrivPage
import com.rosan.installer.ui.page.coui.settings.preferred.about.CouiAboutPage
import com.rosan.installer.ui.page.coui.settings.preferred.about.CouiOpenSourceLicensePage
import com.rosan.installer.ui.page.coui.settings.preferred.installer.CouiInstallerGlobalSettingsPage
import com.rosan.installer.ui.page.coui.settings.preferred.installer.authorizer.CouiAuthorizerCustPage
import com.rosan.installer.ui.page.coui.settings.preferred.installer.dialog.CouiDialogSettingsPage
import com.rosan.installer.ui.page.coui.settings.preferred.installer.notification.CouiNotificationSettingsPage
import com.rosan.installer.ui.page.coui.settings.preferred.lab.CouiLabPage
import com.rosan.installer.ui.page.coui.settings.preferred.network.CouiNetworkPage
import com.rosan.installer.ui.page.coui.settings.preferred.theme.CouiThemeSettingsPage
import com.rosan.installer.ui.page.coui.settings.preferred.uninstaller.CouiUninstallerGlobalSettingsPage
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
import com.rosan.installer.ui.util.rememberDeviceCornerRadius
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.nav.core.NavCornerClipMode
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun InstallerNavContainer(uiState: ThemeState) {
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
        val backdropColor = when (uiState.uiStyle) {
            UiStyle.COUI -> CouiTheme.colorScheme.background
            UiStyle.MIUIX -> MiuixTheme.colorScheme.surface
            UiStyle.MATERIAL3 -> MaterialTheme.colorScheme.surfaceContainer
        }
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
            onBack = onBack,
            transition = transition,
            effects = effects,
        ) {
            entry<Route.Main> {
                InstallerNavEntry(interceptPredictiveBack, onBack) {
                    when (uiState.uiStyle) {
                        UiStyle.COUI -> CouiMainPageWrapper(uiState, sharedViewModel)
                        UiStyle.MIUIX -> MiuixMainPageWrapper(uiState, sharedViewModel)
                        UiStyle.MATERIAL3 -> Material3MainPageWrapper(uiState, sharedViewModel)
                    }
                }
            }
            entry<Route.EditConfig>(swipeDismiss = NavSwipeDirection.None) { key ->
                InstallerNavEntry(interceptPredictiveBack, onBack) {
                    val id = if (key.id != -1L) key.id else null
                    when (uiState.uiStyle) {
                        UiStyle.COUI -> CouiEditPage(id = id, useBlur = useBlur)
                        UiStyle.MIUIX -> MiuixEditPage(id = id, useBlur = useBlur)
                        UiStyle.MATERIAL3 -> EditPage(id = id, useBlur = useBlur)
                    }
                }
            }
            entry<Route.ApplyConfig>(swipeDismiss = swipeBackDirection) { key ->
                InstallerNavEntry(interceptPredictiveBack, onBack) {
                    val id = key.id
                    when (uiState.uiStyle) {
                        UiStyle.COUI -> CouiApplyPage(id, useBlur)
                        UiStyle.MIUIX -> MiuixApplyPage(id, useBlur)
                        UiStyle.MATERIAL3 -> ApplyPage(id, useBlur)
                    }
                }
            }
            entry<Route.About>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(interceptPredictiveBack, onBack) {
                    when (uiState.uiStyle) {
                        UiStyle.COUI -> CouiAboutPage(useBlur)
                        UiStyle.MIUIX -> MiuixAboutPage(useBlur)
                        UiStyle.MATERIAL3 -> AboutPage(useBlur)
                    }
                }
            }
            entry<Route.OpenSourceLicense>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(interceptPredictiveBack, onBack) {
                    when (uiState.uiStyle) {
                        UiStyle.COUI -> CouiOpenSourceLicensePage(useBlur)
                        UiStyle.MIUIX -> MiuixOpenSourceLicensePage(useBlur)
                        UiStyle.MATERIAL3 -> OpenSourceLicensePage(useBlur)
                    }
                }
            }
            entry<Route.Theme>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(interceptPredictiveBack, onBack) {
                    when (uiState.uiStyle) {
                        UiStyle.COUI -> CouiThemeSettingsPage()
                        UiStyle.MIUIX -> MiuixThemeSettingsPage()
                        UiStyle.MATERIAL3 -> ThemeSettingsPage()
                    }
                }
            }
            entry<Route.Network>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(interceptPredictiveBack, onBack) {
                    when (uiState.uiStyle) {
                        UiStyle.COUI -> CouiNetworkPage(useBlur)
                        UiStyle.MIUIX -> MiuixNetworkPage(useBlur)
                        UiStyle.MATERIAL3 -> NetworkPage(useBlur)
                    }
                }
            }
            entry<Route.InstallerGlobal>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(interceptPredictiveBack, onBack) {
                    when (uiState.uiStyle) {
                        UiStyle.COUI -> CouiInstallerGlobalSettingsPage(useBlur)
                        UiStyle.MIUIX -> MiuixInstallerGlobalSettingsPage(useBlur)
                        UiStyle.MATERIAL3 -> InstallerGlobalSettingsPage(useBlur)
                    }
                }
            }
            entry<Route.AuthorizerCust>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(interceptPredictiveBack, onBack) {
                    when (uiState.uiStyle) {
                        UiStyle.COUI -> CouiAuthorizerCustPage(useBlur)
                        UiStyle.MIUIX -> MiuixAuthorizerCustPage(useBlur)
                        UiStyle.MATERIAL3 -> AuthorizerCustPage(useBlur)
                    }
                }
            }
            entry<Route.DialogSettings>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(interceptPredictiveBack, onBack) {
                    when (uiState.uiStyle) {
                        UiStyle.COUI -> CouiDialogSettingsPage(useBlur)
                        UiStyle.MIUIX -> MiuixDialogSettingsPage(useBlur)
                        UiStyle.MATERIAL3 -> DialogSettingsPage(useBlur)
                    }
                }
            }
            entry<Route.NotificationSettings>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(interceptPredictiveBack, onBack) {
                    when (uiState.uiStyle) {
                        UiStyle.COUI -> CouiNotificationSettingsPage(useBlur)
                        UiStyle.MIUIX -> MiuixNotificationSettingsPage(useBlur)
                        UiStyle.MATERIAL3 -> NotificationSettingsPage(useBlur)
                    }
                }
            }
            entry<Route.UninstallerGlobal>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(interceptPredictiveBack, onBack) {
                    when (uiState.uiStyle) {
                        UiStyle.COUI -> CouiUninstallerGlobalSettingsPage(useBlur)
                        UiStyle.MIUIX -> MiuixUninstallerGlobalSettingsPage(useBlur)
                        UiStyle.MATERIAL3 -> UninstallerGlobalSettingsPage(useBlur)
                    }
                }
            }
            entry<Route.Lab>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(interceptPredictiveBack, onBack) {
                    when (uiState.uiStyle) {
                        UiStyle.COUI -> CouiLabPage(useBlur)
                        UiStyle.MIUIX -> MiuixLabPage(useBlur)
                        UiStyle.MATERIAL3 -> LabPage(useBlur)
                    }
                }
            }
            entry<Route.DefaultInstaller>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(interceptPredictiveBack, onBack) {
                    when (uiState.uiStyle) {
                        UiStyle.COUI -> CouiDefaultInstallerPage(useBlur)
                        UiStyle.MIUIX -> MiuixDefaultInstallerPage(useBlur)
                        UiStyle.MATERIAL3 -> DefaultInstallerPage(useBlur)
                    }
                }
            }
            entry<Route.Priv>(swipeDismiss = swipeBackDirection) {
                InstallerNavEntry(interceptPredictiveBack, onBack) {
                    when (uiState.uiStyle) {
                        UiStyle.COUI -> CouiPrivPage(useBlur)
                        UiStyle.MIUIX -> MiuixPrivPage(useBlur)
                        UiStyle.MATERIAL3 -> PrivPage(useBlur)
                    }
                }
            }
        }
    }
}

@Composable
private fun InstallerNavEntry(interceptPredictiveBack: Boolean, onBack: () -> Unit, content: @Composable () -> Unit) {
    val navigationEventState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = interceptPredictiveBack,
        onBackCompleted = onBack,
    )
    content()
}
