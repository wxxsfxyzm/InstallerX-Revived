// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.activity

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
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
import com.rosan.installer.domain.settings.model.ThemeState
import com.rosan.installer.domain.settings.provider.ThemeStateProvider
import com.rosan.installer.ui.animation.predictiveback.PredictiveBackAnimationHandler
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
import com.rosan.installer.ui.page.main.settings.preferred.subpage.about.AboutPage
import com.rosan.installer.ui.page.main.settings.preferred.subpage.about.NewAboutPage
import com.rosan.installer.ui.page.main.settings.preferred.subpage.about.OpenSourceLicensePage
import com.rosan.installer.ui.page.main.settings.preferred.subpage.installer.LegacyInstallerGlobalSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.subpage.installer.NewInstallerGlobalSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.subpage.lab.LegacyLabPage
import com.rosan.installer.ui.page.main.settings.preferred.subpage.lab.NewLabPage
import com.rosan.installer.ui.page.main.settings.preferred.subpage.theme.LegacyThemeSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.subpage.theme.NewThemeSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.subpage.uninstaller.LegacyUninstallerGlobalSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.subpage.uninstaller.NewUninstallerGlobalSettingsPage
import com.rosan.installer.ui.page.miuix.settings.SettingsCompactLayout
import com.rosan.installer.ui.page.miuix.settings.SettingsWideScreenLayout
import com.rosan.installer.ui.page.miuix.settings.config.apply.MiuixApplyPage
import com.rosan.installer.ui.page.miuix.settings.config.edit.MiuixEditPage
import com.rosan.installer.ui.page.miuix.settings.preferred.subpage.about.MiuixAboutPage
import com.rosan.installer.ui.page.miuix.settings.preferred.subpage.about.MiuixBlendAboutPage
import com.rosan.installer.ui.page.miuix.settings.preferred.subpage.about.ossLicensePage.MiuixOpenSourceLicensePage
import com.rosan.installer.ui.page.miuix.settings.preferred.subpage.installer.MiuixInstallerGlobalSettingsPage
import com.rosan.installer.ui.page.miuix.settings.preferred.subpage.lab.MiuixLabPage
import com.rosan.installer.ui.page.miuix.settings.preferred.subpage.theme.MiuixThemeSettingsPage
import com.rosan.installer.ui.page.miuix.settings.preferred.subpage.uninstaller.MiuixUninstallerGlobalSettingsPage
import com.rosan.installer.ui.theme.InstallerTheme
import com.rosan.installer.ui.theme.rememberMiuixHazeStyle
import com.rosan.installer.ui.util.UIConstants
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.material3.Surface as Material3Surface
import top.yukonga.miuix.kmp.basic.Surface as MiuixSurface

// Define screen layout types to prevent dynamic Subcompose delays
enum class WindowLayoutType {
    COMPACT,
    EXPANDED
}

// Statically calculate layout based on accurate window info
@Composable
fun calculateWindowLayoutType(): WindowLayoutType {
    val containerSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    val screenWidthDp = with(density) { containerSize.width.toDp() }
    val screenHeightDp = with(density) { containerSize.height.toDp() }

    val isDefinitelyWide = screenWidthDp > UIConstants.WIDE_SCREEN_THRESHOLD
    val aspectRatio = screenHeightDp.value / screenWidthDp.value
    val isWideByShape = screenWidthDp > UIConstants.MEDIUM_WIDTH_THRESHOLD &&
            aspectRatio < UIConstants.PORTRAIT_ASPECT_RATIO_THRESHOLD

    return if (isDefinitelyWide || isWideByShape) WindowLayoutType.EXPANDED else WindowLayoutType.COMPACT
}

class SettingsActivity : ComponentActivity(), KoinComponent {
    private val themeStateProvider by inject<ThemeStateProvider>()

    val predictiveBackAnimationHandler: PredictiveBackAnimationHandler =
        ScalePredictiveBackAnimation()

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
                if (uiState.useMiuix) {
                    MiuixSurface(modifier = Modifier.fillMaxSize()) {
                        InstallerNavContainer(uiState, layoutType, predictiveBackAnimationHandler)
                    }
                } else {
                    Material3Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = if (uiState.isExpressive) MaterialTheme.colorScheme.surfaceContainer
                        else MaterialTheme.colorScheme.surface
                    ) {
                        InstallerNavContainer(uiState, layoutType, predictiveBackAnimationHandler)
                    }
                }
            }
        }
    }
}

@Composable
fun InstallerNavContainer(
    uiState: ThemeState,
    layoutType: WindowLayoutType,
    predictiveBackAnimationHandler: PredictiveBackAnimationHandler
) {
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
                        predictiveBackAnimationHandler.PredictiveBackAnimationDecorator(
                            gestureState?.transitionState,
                            content.contentKey,
                            navigator.current()
                        ) {
                            content.Content()
                        }
                    }
                ),
                entryProvider = if (uiState.useMiuix) miuixEntryProvider(uiState, layoutType)
                else materialEntryProvider(isExpressive, useBlur),
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

fun miuixEntryProvider(
    uiState: ThemeState,
    layoutType: WindowLayoutType
): (key: NavKey) -> NavEntry<NavKey> = entryProvider {
    entry<Route.Main> { MiuixMainPageWrapper(uiState, layoutType) }

    entry<Route.EditConfig> { key ->
        val useBlur = uiState.useBlur
        val id = key.id
        MiuixEditPage(
            id = if (id != -1L) id else null,
            useBlur = useBlur
        )
    }
    entry<Route.ApplyConfig> { key ->
        val id = key.id
        MiuixApplyPage(id)
    }
    entry<Route.About> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA)
            MiuixBlendAboutPage()
        else MiuixAboutPage()
    }
    entry<Route.OpenSourceLicense> {
        MiuixOpenSourceLicensePage()
    }
    entry<Route.Theme> {
        MiuixThemeSettingsPage()
    }
    entry<Route.InstallerGlobal> {
        MiuixInstallerGlobalSettingsPage()
    }
    entry<Route.UninstallerGlobal> {
        MiuixUninstallerGlobalSettingsPage()
    }
    entry<Route.Lab> {
        MiuixLabPage()
    }
}

fun materialEntryProvider(
    isExpressive: Boolean,
    useBlur: Boolean
): (key: NavKey) -> NavEntry<NavKey> = entryProvider {
    entry<Route.Main> { MainPage() }

    entry<Route.EditConfig> { key ->
        val id = key.id
        if (isExpressive)
            NewEditPage(
                id = if (id != -1L) id else null,
                useBlur = useBlur
            ) else
            EditPage(
                id = if (id != -1L) id
                else null
            )
    }
    entry<Route.ApplyConfig> { key ->
        val id = key.id
        if (isExpressive)
            NewApplyPage(id)
        else
            ApplyPage(id)
    }
    entry<Route.About> {
        if (isExpressive)
            NewAboutPage()
        else
            AboutPage()
    }
    entry<Route.OpenSourceLicense> {
        OpenSourceLicensePage(isExpressive, useBlur)
    }
    entry<Route.Theme> {
        if (isExpressive) {
            NewThemeSettingsPage()
        } else {
            LegacyThemeSettingsPage()
        }
    }
    entry<Route.InstallerGlobal> {
        if (isExpressive) {
            NewInstallerGlobalSettingsPage()
        } else {
            LegacyInstallerGlobalSettingsPage()
        }
    }
    entry<Route.UninstallerGlobal> {
        if (isExpressive) {
            NewUninstallerGlobalSettingsPage()
        } else {
            LegacyUninstallerGlobalSettingsPage()
        }
    }
    entry<Route.Lab> {
        if (isExpressive) {
            NewLabPage()
        } else {
            LegacyLabPage()
        }
    }
}
