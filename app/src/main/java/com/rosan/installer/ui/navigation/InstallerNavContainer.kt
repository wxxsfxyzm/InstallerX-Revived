// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SceneInfo
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.NavDisplayTransitionEffects
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.NavigationEventState
import androidx.navigationevent.compose.rememberNavigationEventState
import com.rosan.installer.domain.settings.model.PredictiveBackAnimation
import com.rosan.installer.domain.settings.model.ThemeState
import com.rosan.installer.ui.animation.predictiveback.AOSPCrossActivityAnimation
import com.rosan.installer.ui.animation.predictiveback.KernelSUClassicPredictiveBackAnimation
import com.rosan.installer.ui.animation.predictiveback.MiuixPredictiveBackAnimation
import com.rosan.installer.ui.animation.predictiveback.NoPredictiveBackAnimation
import com.rosan.installer.ui.animation.predictiveback.ScalePredictiveBackAnimation
import com.rosan.installer.ui.page.main.settings.config.apply.ApplyPage
import com.rosan.installer.ui.page.main.settings.config.apply.NewApplyPage
import com.rosan.installer.ui.page.main.settings.config.edit.EditPage
import com.rosan.installer.ui.page.main.settings.config.edit.NewEditPage
import com.rosan.installer.ui.page.main.settings.main.MainPage
import com.rosan.installer.ui.page.main.settings.preferred.about.AboutPage
import com.rosan.installer.ui.page.main.settings.preferred.about.NewAboutPage
import com.rosan.installer.ui.page.main.settings.preferred.about.OpenSourceLicensePage
import com.rosan.installer.ui.page.main.settings.preferred.auxiliaryinstall.LegacyAuxiliaryInstallSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.auxiliaryinstall.NewAuxiliaryInstallSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.installer.LegacyInstallerGlobalSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.installer.NewInstallerGlobalSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.installer.dialog.DialogSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.installer.dialog.NewDialogSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.installer.notification.NewNotificationSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.installer.notification.NotificationSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.lab.LegacyLabPage
import com.rosan.installer.ui.page.main.settings.preferred.lab.NewLabPage
import com.rosan.installer.ui.page.main.settings.preferred.theme.LegacyThemeSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.theme.NewThemeSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.uninstaller.LegacyUninstallerGlobalSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.uninstaller.NewUninstallerGlobalSettingsPage
import com.rosan.installer.ui.page.miuix.settings.config.apply.MiuixApplyPage
import com.rosan.installer.ui.page.miuix.settings.config.edit.MiuixEditPage
import com.rosan.installer.ui.page.miuix.settings.preferred.about.MiuixAboutPage
import com.rosan.installer.ui.page.miuix.settings.preferred.about.ossLicensePage.MiuixOpenSourceLicensePage
import com.rosan.installer.ui.page.miuix.settings.preferred.auxiliaryinstall.MiuixAuxiliaryInstallSettingsPage
import com.rosan.installer.ui.page.miuix.settings.preferred.installer.MiuixInstallerGlobalSettingsPage
import com.rosan.installer.ui.page.miuix.settings.preferred.installer.dialog.MiuixDialogSettingsPage
import com.rosan.installer.ui.page.miuix.settings.preferred.installer.notification.MiuixNotificationSettingsPage
import com.rosan.installer.ui.page.miuix.settings.preferred.lab.MiuixLabPage
import com.rosan.installer.ui.page.miuix.settings.preferred.theme.MiuixThemeSettingsPage
import com.rosan.installer.ui.page.miuix.settings.preferred.uninstaller.MiuixUninstallerGlobalSettingsPage
import kotlinx.coroutines.launch

@Composable
fun InstallerNavContainer(
    uiState: ThemeState
) {
    val predictiveBackAnimationHandler = remember(uiState.predictiveBackAnimation, uiState.predictiveBackExitDirection) {
        when (uiState.predictiveBackAnimation) {
            PredictiveBackAnimation.None -> NoPredictiveBackAnimation()
            PredictiveBackAnimation.AOSP -> AOSPCrossActivityAnimation(uiState.predictiveBackExitDirection)
            PredictiveBackAnimation.Scale -> ScalePredictiveBackAnimation(uiState.predictiveBackExitDirection)
            PredictiveBackAnimation.KernelSUClassic -> KernelSUClassicPredictiveBackAnimation()
            PredictiveBackAnimation.MIUIX -> MiuixPredictiveBackAnimation()
        }
    }

    // Initialize the back stack with rememberNavBackStack
    val backStack = rememberNavBackStack(Route.Main)

    // Pass the managed back stack to the Navigator
    val navigator = remember(backStack) { Navigator(backStack) }
    val useBlur = uiState.useBlur
    val isExpressive = uiState.isExpressive

    CompositionLocalProvider(
        LocalNavigator provides navigator,
    ) {
        var gestureState: NavigationEventState<SceneInfo<NavKey>>? = null
        val navigationScope = rememberCoroutineScope()
        val onBack: (() -> Unit) -> Unit = { callBack ->
            navigationScope.launch {
                predictiveBackAnimationHandler.onBackPressed(
                    gestureState?.transitionState,
                    navigator.current()
                )
                callBack() // update transitionState
                navigator.pop()
            }
        }

        val entries =
            rememberDecoratedNavEntries(
                backStack = navigator.backStack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                    NavEntryDecorator(
                        onPop = { key ->
                            predictiveBackAnimationHandler.onPagePop(
                                contentPageKey = key,
                                animationScope = navigationScope
                            )
                        }
                    ) { content ->
                        with(predictiveBackAnimationHandler) {
                            Box(
                                modifier = Modifier.predictiveBackAnimationDecorator(
                                    gestureState?.transitionState,
                                    content.contentKey,
                                    navigator.current()
                                )
                            ) {
                                content.Content()
                            }
                        }
                    }
                ),
                entryProvider = entryProvider {
                    entry<Route.Main> {
                        if (uiState.useMiuix) {
                            MiuixMainPageWrapper(uiState)
                        } else {
                            MainPage(uiState)
                        }
                    }
                    entry<Route.EditConfig> { key ->
                        val id = key.id
                        if (uiState.useMiuix) {
                            val useBlur = uiState.useBlur
                            MiuixEditPage(
                                id = if (id != -1L) id else null,
                                useBlur = useBlur
                            )
                        } else if (isExpressive) {
                            NewEditPage(
                                id = if (id != -1L) id else null,
                                useBlur = useBlur
                            )
                        } else {
                            EditPage(
                                id = if (id != -1L) id
                                else null
                            )
                        }
                    }
                    entry<Route.ApplyConfig> { key ->
                        if (uiState.useMiuix) {
                            val id = key.id
                            MiuixApplyPage(id, useBlur)
                        } else {
                            val id = key.id
                            if (isExpressive)
                                NewApplyPage(id, useBlur)
                            else
                                ApplyPage(id)
                        }
                    }
                    entry<Route.About> {
                        if (uiState.useMiuix) {
                            MiuixAboutPage(useBlur)
                        } else {
                            if (isExpressive)
                                NewAboutPage(useBlur)
                            else
                                AboutPage()
                        }
                    }
                    entry<Route.OpenSourceLicense> {
                        if (uiState.useMiuix) {
                            MiuixOpenSourceLicensePage(useBlur)
                        } else {
                            OpenSourceLicensePage(isExpressive, useBlur)
                        }
                    }
                    entry<Route.Theme> {
                        if (uiState.useMiuix) {
                            MiuixThemeSettingsPage()
                        } else {
                            if (isExpressive) {
                                NewThemeSettingsPage()
                            } else {
                                LegacyThemeSettingsPage()
                            }
                        }
                    }
                    entry<Route.InstallerGlobal> {
                        if (uiState.useMiuix) {
                            MiuixInstallerGlobalSettingsPage(useBlur)
                        } else {
                            if (isExpressive) {
                                NewInstallerGlobalSettingsPage(useBlur)
                            } else {
                                LegacyInstallerGlobalSettingsPage()
                            }
                        }
                    }
                    entry<Route.DialogSettings> {
                        if (uiState.useMiuix) {
                            MiuixDialogSettingsPage(useBlur)
                        } else {
                            if (isExpressive) {
                                NewDialogSettingsPage(useBlur)
                            } else {
                                DialogSettingsPage()

                            }
                        }
                    }
                    entry<Route.NotificationSettings> {
                        if (uiState.useMiuix) {
                            MiuixNotificationSettingsPage(useBlur)
                        } else {
                            if (isExpressive) {
                                NewNotificationSettingsPage(useBlur)
                            } else {
                                NotificationSettingsPage()
                            }
                        }
                    }
                    entry<Route.UninstallerGlobal> {
                        if (uiState.useMiuix) {
                            MiuixUninstallerGlobalSettingsPage(useBlur)
                        } else {
                            if (isExpressive) {
                                NewUninstallerGlobalSettingsPage(useBlur)
                            } else {
                                LegacyUninstallerGlobalSettingsPage()
                            }
                        }
                    }
                    entry<Route.AuxiliaryInstall> {
                        if (uiState.useMiuix) {
                            MiuixAuxiliaryInstallSettingsPage(useBlur)
                        } else {
                            if (isExpressive) {
                                NewAuxiliaryInstallSettingsPage(useBlur)
                            } else {
                                LegacyAuxiliaryInstallSettingsPage()
                            }
                        }
                    }
                    entry<Route.Lab> {
                        if (uiState.useMiuix) {
                            MiuixLabPage(useBlur)
                        } else {
                            if (isExpressive) {
                                NewLabPage(useBlur)
                            } else {
                                LegacyLabPage()
                            }
                        }
                    }
                },
            )

        val sceneState =
            rememberSceneState(
                entries = entries,
                sceneStrategies = listOf(SinglePaneSceneStrategy()),
                sceneDecoratorStrategies = emptyList(),
                sharedTransitionScope = null,
                onBack = { onBack {} },
            )
        val scene = sceneState.currentScene

        // Predictive Back Handling
        val currentInfo = SceneInfo(scene)
        val previousSceneInfos = sceneState.previousScenes.map { SceneInfo(it) }
        gestureState = rememberNavigationEventState(
            currentInfo = currentInfo,
            backInfo = previousSceneInfos
        )

        NavigationBackHandler(
            state = gestureState,
            isBackEnabled = scene.previousEntries.isNotEmpty(),
            onBackCompleted = { callBack -> onBack(callBack) },
            onBackCancelled = { callBack -> callBack() }
        )

        NavDisplay(
            sceneState = sceneState,
            navigationEventState = gestureState,
            contentAlignment = Alignment.TopStart,
            sizeTransform = null,
            transitionEffects = NavDisplayTransitionEffects(
                // Disables touch interception during transitions
                blockInputDuringTransition = true
            ),
            predictivePopTransitionSpec = { swipeEdge ->
                with(predictiveBackAnimationHandler) {
                    onPredictivePopTransitionSpec(swipeEdge = swipeEdge)
                }
            },
            popTransitionSpec = {
                with(predictiveBackAnimationHandler) {
                    onPopTransitionSpec()
                }
            },
            transitionSpec = {
                with(predictiveBackAnimationHandler) {
                    onTransitionSpec()
                }
            }
        )
    }
}
