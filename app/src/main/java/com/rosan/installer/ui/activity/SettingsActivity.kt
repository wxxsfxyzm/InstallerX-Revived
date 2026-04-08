// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.activity

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RoomPreferences
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SceneInfo
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.NavigationEventState
import androidx.navigationevent.compose.rememberNavigationEventState
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.PredictiveBackAnimation
import com.rosan.installer.domain.settings.model.ThemeState
import com.rosan.installer.domain.settings.provider.ThemeStateProvider
import com.rosan.installer.ui.animation.predictiveback.AOSPCrossActivityAnimation
import com.rosan.installer.ui.animation.predictiveback.KernelSUClassicPredictiveBackAnimation
import com.rosan.installer.ui.animation.predictiveback.KernelSUOfficialPredictiveBackAnimation
import com.rosan.installer.ui.animation.predictiveback.NoPredictiveBackAnimation
import com.rosan.installer.ui.animation.predictiveback.ScalePredictiveBackAnimation
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.navigation.Route
import com.rosan.installer.ui.navigation.rememberNavigator
import com.rosan.installer.ui.page.main.settings.SettingsSharedViewModel
import com.rosan.installer.ui.page.main.settings.config.apply.ApplyPage
import com.rosan.installer.ui.page.main.settings.config.apply.NewApplyPage
import com.rosan.installer.ui.page.main.settings.config.edit.EditPage
import com.rosan.installer.ui.page.main.settings.config.edit.NewEditPage
import com.rosan.installer.ui.page.main.settings.main.MainPage
import com.rosan.installer.ui.page.main.settings.preferred.about.AboutPage
import com.rosan.installer.ui.page.main.settings.preferred.about.NewAboutPage
import com.rosan.installer.ui.page.main.settings.preferred.about.OpenSourceLicensePage
import com.rosan.installer.ui.page.main.settings.preferred.installer.LegacyInstallerGlobalSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.installer.NewInstallerGlobalSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.installer.notification.NewNotificationSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.installer.notification.NotificationSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.lab.LegacyLabPage
import com.rosan.installer.ui.page.main.settings.preferred.lab.NewLabPage
import com.rosan.installer.ui.page.main.settings.preferred.theme.LegacyThemeSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.theme.NewThemeSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.uninstaller.LegacyUninstallerGlobalSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.uninstaller.NewUninstallerGlobalSettingsPage
import com.rosan.installer.ui.page.miuix.settings.SettingsCompactLayout
import com.rosan.installer.ui.page.miuix.settings.SettingsWideScreenLayout
import com.rosan.installer.ui.page.miuix.settings.config.apply.MiuixApplyPage
import com.rosan.installer.ui.page.miuix.settings.config.edit.MiuixEditPage
import com.rosan.installer.ui.page.miuix.settings.preferred.about.MiuixAboutPage
import com.rosan.installer.ui.page.miuix.settings.preferred.about.MiuixBlendAboutPage
import com.rosan.installer.ui.page.miuix.settings.preferred.about.ossLicensePage.MiuixOpenSourceLicensePage
import com.rosan.installer.ui.page.miuix.settings.preferred.installer.MiuixInstallerGlobalSettingsPage
import com.rosan.installer.ui.page.miuix.settings.preferred.installer.notification.MiuixNotificationSettingsPage
import com.rosan.installer.ui.page.miuix.settings.preferred.lab.MiuixLabPage
import com.rosan.installer.ui.page.miuix.settings.preferred.theme.MiuixThemeSettingsPage
import com.rosan.installer.ui.page.miuix.settings.preferred.uninstaller.MiuixUninstallerGlobalSettingsPage
import com.rosan.installer.ui.theme.InstallerTheme
import com.rosan.installer.ui.theme.rememberMiuixHazeStyle
import com.rosan.installer.ui.util.WindowLayoutType
import com.rosan.installer.ui.util.calculateWindowLayoutType
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.theme.MiuixTheme

class SettingsActivity : ComponentActivity(), KoinComponent {
    private val themeStateProvider by inject<ThemeStateProvider>()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        // Enable edge-to-edge mode for immersive experience
        enableEdgeToEdge()
        // Compat Navigation Bar color for Xiaomi Devices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            window.isNavigationBarContrastEnforced = false

        var isThemeLoaded = false
        // Keep splash screen visible until data is safely loaded
        splashScreen.setKeepOnScreenCondition { !isThemeLoaded }

        super.onCreate(savedInstanceState)
        setContent {
            val uiState by themeStateProvider.themeStateFlow.collectAsStateWithLifecycle(initialValue = ThemeState())
            isThemeLoaded = uiState.isLoaded

            // Prevent heavy navigation setup until state is ready
            if (!isThemeLoaded) return@setContent

            val layoutType = calculateWindowLayoutType()

            InstallerTheme(
                isExpressive = uiState.isExpressive,
                useMiuix = uiState.useMiuix,
                themeMode = uiState.themeMode,
                paletteStyle = uiState.paletteStyle,
                colorSpec = uiState.colorSpec,
                useDynamicColor = uiState.useDynamicColor,
                useMiuixMonet = uiState.useMiuixMonet,
                seedColor = uiState.seedColor
            ) {
                val backgroundColor =
                    if (uiState.useMiuix)
                        MiuixTheme.colorScheme.surface
                    else if (uiState.isExpressive)
                        MaterialTheme.colorScheme.surfaceContainer
                    else
                        MaterialTheme.colorScheme.surface
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundColor)
                ) {
                    InstallerNavContainer(uiState, layoutType)
                }
            }
        }
    }
}

@Composable
fun InstallerNavContainer(
    uiState: ThemeState,
    layoutType: WindowLayoutType
) {
    val predictiveBackAnimationHandler = remember(uiState.predictiveBackAnimation, uiState.predictiveBackExitDirection) {
        when (uiState.predictiveBackAnimation) {
            PredictiveBackAnimation.None -> NoPredictiveBackAnimation()
            PredictiveBackAnimation.AOSP -> AOSPCrossActivityAnimation(uiState.predictiveBackExitDirection)
            PredictiveBackAnimation.Scale -> ScalePredictiveBackAnimation(uiState.predictiveBackExitDirection)
            PredictiveBackAnimation.KernelSUClassic -> KernelSUClassicPredictiveBackAnimation()
            PredictiveBackAnimation.KernelSUOfficial -> KernelSUOfficialPredictiveBackAnimation()
        }
    }
    val navigator = rememberNavigator(Route.Main)
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
                    NavEntryDecorator { content ->
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
                            MiuixMainPageWrapper(uiState, layoutType)
                        } else {
                            MainPage()
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
                            MiuixApplyPage(id)
                        } else {
                            val id = key.id
                            if (isExpressive)
                                NewApplyPage(id)
                            else
                                ApplyPage(id)
                        }
                    }
                    entry<Route.About> {
                        if (uiState.useMiuix) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA)
                                MiuixBlendAboutPage()
                            else MiuixAboutPage()
                        } else {
                            if (isExpressive)
                                NewAboutPage()
                            else
                                AboutPage()
                        }
                    }
                    entry<Route.OpenSourceLicense> {
                        if (uiState.useMiuix) {
                            MiuixOpenSourceLicensePage()
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
                            MiuixInstallerGlobalSettingsPage()
                        } else {
                            if (isExpressive) {
                                NewInstallerGlobalSettingsPage()
                            } else {
                                LegacyInstallerGlobalSettingsPage()
                            }
                        }
                    }
                    entry<Route.NotificationSettings> {
                        if (uiState.useMiuix) {
                            MiuixNotificationSettingsPage()
                        } else {
                            if (isExpressive) {
                                NewNotificationSettingsPage()
                            } else {
                                NotificationSettingsPage()
                            }
                        }
                    }
                    entry<Route.UninstallerGlobal> {
                        if (uiState.useMiuix) {
                            MiuixUninstallerGlobalSettingsPage()
                        } else {
                            if (isExpressive) {
                                NewUninstallerGlobalSettingsPage()
                            } else {
                                LegacyUninstallerGlobalSettingsPage()
                            }
                        }
                    }
                    entry<Route.Lab> {
                        if (uiState.useMiuix) {
                            MiuixLabPage()
                        } else {
                            if (isExpressive) {
                                NewLabPage()
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
            // Animations elegantly delegated to the handler
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

// Centralized complex layout logic to keep provider clean and readable
@Composable
fun MiuixMainPageWrapper(uiState: ThemeState, layoutType: WindowLayoutType) {
    val sharedViewModel: SettingsSharedViewModel =
        koinViewModel(viewModelStoreOwner = LocalActivity.current as ComponentActivity)
    val sharedState by sharedViewModel.state.collectAsStateWithLifecycle()
    val useBlur = uiState.useBlur
    val useFloatingBottomBar = uiState.useAppleFloatingBar
    val useFloatingBottomBarBlur =
        useBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    val navigationItems = listOf(
        NavigationItem(
            label = stringResource(R.string.config),
            icon = Icons.Rounded.RoomPreferences
        ),
        NavigationItem(
            label = stringResource(R.string.preferred),
            icon = Icons.Rounded.Settings
        )
    )

    val pagerState = rememberPagerState(
        initialPage = sharedState.lastMainPageIndex,
        pageCount = { navigationItems.size }
    )

    LaunchedEffect(pagerState.currentPage) {
        if (sharedState.lastMainPageIndex != pagerState.currentPage) {
            sharedViewModel.updateLastMainPageIndex(pagerState.currentPage)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val hazeState = if (useBlur) remember { HazeState() } else null
    val hazeStyle = rememberMiuixHazeStyle()
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    // Branch statically without layout delay traps
    if (layoutType == WindowLayoutType.EXPANDED) {
        SettingsWideScreenLayout(
            pagerState = pagerState,
            navigationItems = navigationItems,
            snackbarHostState = snackbarHostState,
            useFloatingBottomBar = useFloatingBottomBar,
            useFloatingBottomBarBlur = useFloatingBottomBarBlur,
            hazeState = hazeState,
            hazeStyle = hazeStyle,
            backdrop = backdrop
        )
    } else {
        SettingsCompactLayout(
            pagerState = pagerState,
            navigationItems = navigationItems,
            snackbarHostState = snackbarHostState,
            useFloatingBottomBar = useFloatingBottomBar,
            useFloatingBottomBarBlur = useFloatingBottomBarBlur,
            hazeState = hazeState,
            hazeStyle = hazeStyle,
            backdrop = backdrop,
        )
    }
}