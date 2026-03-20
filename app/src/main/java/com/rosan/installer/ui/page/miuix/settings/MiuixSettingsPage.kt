// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.settings

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RoomPreferences
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.ThemeState
import com.rosan.installer.domain.settings.provider.ThemeStateProvider
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.SettingsSharedViewModel
import com.rosan.installer.ui.page.miuix.settings.config.all.MiuixAllPage
import com.rosan.installer.ui.page.miuix.settings.config.apply.MiuixApplyPage
import com.rosan.installer.ui.page.miuix.settings.config.edit.MiuixEditPage
import com.rosan.installer.ui.page.miuix.settings.preferred.MiuixPreferredPage
import com.rosan.installer.ui.page.miuix.settings.preferred.subpage.about.MiuixAboutPage
import com.rosan.installer.ui.page.miuix.settings.preferred.subpage.about.ossLicensePage.MiuixOpenSourceLicensePage
import com.rosan.installer.ui.page.miuix.settings.preferred.subpage.installer.MiuixInstallerGlobalSettingsPage
import com.rosan.installer.ui.page.miuix.settings.preferred.subpage.lab.MiuixLabPage
import com.rosan.installer.ui.page.miuix.settings.preferred.subpage.theme.MiuixThemeSettingsPage
import com.rosan.installer.ui.page.miuix.settings.preferred.subpage.uninstaller.MiuixUninstallerGlobalSettingsPage
import com.rosan.installer.ui.theme.getMiuixAppBarColor
import com.rosan.installer.ui.theme.installerHazeEffect
import com.rosan.installer.ui.theme.rememberMiuixHazeStyle
import com.rosan.installer.ui.util.UIConstants
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun MiuixSettingsPage(
    sharedViewModel: SettingsSharedViewModel = koinViewModel(viewModelStoreOwner = LocalActivity.current as ComponentActivity)
) {
    val navController = rememberNavController()
    val themeStateProvider = koinInject<ThemeStateProvider>()
    val uiState by themeStateProvider.themeStateFlow.collectAsStateWithLifecycle(initialValue = ThemeState())
    val sharedState by sharedViewModel.state.collectAsStateWithLifecycle()
    val useBlur = uiState.useBlur

    val currentBackStackEntry by navController.currentBackStackEntryAsState()

    LaunchedEffect(currentBackStackEntry, sharedState.pendingNavigateToTheme) {
        if (sharedState.pendingNavigateToTheme && currentBackStackEntry?.destination?.route == MiuixSettingsScreen.MiuixMain.route) {
            navController.navigate(MiuixSettingsScreen.MiuixTheme.route)
            sharedViewModel.markPendingNavigateToTheme(false)
        }
    }

    NavHost(
        navController = navController,
        startDestination = MiuixSettingsScreen.MiuixMain.route,
        // Animation from KernelSU manager
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 5 },
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 5 },
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        }
    ) {
        composable(route = MiuixSettingsScreen.MiuixMain.route) {
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

            // --- Layout Decision Logic ---
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isDefinitelyWide = maxWidth > UIConstants.WIDE_SCREEN_THRESHOLD
                val isWideByShape =
                    maxWidth > UIConstants.MEDIUM_WIDTH_THRESHOLD && (maxHeight.value / maxWidth.value < UIConstants.PORTRAIT_ASPECT_RATIO_THRESHOLD)
                val isWideScreen = isDefinitelyWide || isWideByShape

                if (isWideScreen)
                    SettingsWideScreenLayout(
                        navController = navController,
                        pagerState = pagerState,
                        navigationItems = navigationItems,
                        snackbarHostState = snackbarHostState,
                        hazeState = hazeState,
                        hazeStyle = hazeStyle
                    )
                else
                    SettingsCompactLayout(
                        navController = navController,
                        pagerState = pagerState,
                        navigationItems = navigationItems,
                        snackbarHostState = snackbarHostState,
                        hazeState = hazeState,
                        hazeStyle = hazeStyle
                    )
            }
        }

        composable(
            route = MiuixSettingsScreen.MiuixEditConfig.route,
            arguments = listOf(
                navArgument("id") {
                    type = NavType.LongType
                }
            ),
        ) {
            val id = it.arguments?.getLong("id")
            MiuixEditPage(
                navController = navController,
                id = if (id != -1L) id else null,
                useBlur = useBlur
            )
        }
        composable(
            route = MiuixSettingsScreen.MiuixApplyConfig.route,
            arguments = listOf(
                navArgument("id") {
                    type = NavType.LongType
                }
            ),
        ) {
            val id = it.arguments?.getLong("id")!!
            MiuixApplyPage(
                navController = navController,
                id = id
            )
        }
        composable(route = MiuixSettingsScreen.MiuixAbout.route) {
            MiuixAboutPage(navController = navController)
        }
        composable(route = MiuixSettingsScreen.MiuixOpenSourceLicense.route) {
            MiuixOpenSourceLicensePage(navController = navController)
        }
        composable(route = MiuixSettingsScreen.MiuixTheme.route) {
            MiuixThemeSettingsPage(navController = navController)
        }
        composable(route = MiuixSettingsScreen.MiuixInstallerGlobal.route) {
            MiuixInstallerGlobalSettingsPage(navController = navController)
        }
        composable(route = MiuixSettingsScreen.MiuixUninstallerGlobal.route) {
            MiuixUninstallerGlobalSettingsPage(navController = navController)
        }
        composable(route = MiuixSettingsScreen.MiuixLab.route) {
            MiuixLabPage(navController = navController)
        }
    }
}

/**
 * Compact Screen Layout (Portrait/Phone)
 */
@Composable
private fun SettingsCompactLayout(
    navController: NavController,
    pagerState: PagerState,
    navigationItems: List<NavigationItem>,
    snackbarHostState: SnackbarHostState,
    hazeState: HazeState?,
    hazeStyle: HazeStyle
) {
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.installerHazeEffect(hazeState, hazeStyle),
                color = hazeState.getMiuixAppBarColor()
            ) {
                navigationItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        icon = item.icon,
                        label = item.label
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(state = snackbarHostState) },
        floatingActionButton = {
            // FAB logic specifically tied to the first page (Config)
            AnimatedVisibility(
                visible = pagerState.currentPage == 0,
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                FloatingActionButton(
                    modifier = Modifier.padding(end = 16.dp),
                    containerColor = if (MiuixTheme.isDynamicColor) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.background,
                    shadowElevation = 2.dp,
                    onClick = { navController.navigate(MiuixSettingsScreen.Builder.MiuixEditConfig(null).route) }
                ) {
                    Icon(
                        imageVector = AppIcons.Add,
                        modifier = Modifier.size(40.dp),
                        contentDescription = stringResource(id = R.string.add),
                        tint = if (MiuixTheme.isDynamicColor) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.primary
                    )
                }
            }
        }
    ) { paddingValues ->
        InstallerPagerContent(
            hazeState = hazeState,
            pagerState = pagerState,
            navController = navController,
            navigationItems = navigationItems,
            snackbarHostState = snackbarHostState,
            modifier = Modifier.fillMaxSize(),
            outerPadding = paddingValues
        )
    }
}

/**
 * Wide Screen Layout (Tablet/Landscape)
 * Uses a Row to split the View into a Side Panel and Main Content.
 */
@Composable
private fun SettingsWideScreenLayout(
    navController: NavController,
    pagerState: PagerState,
    navigationItems: List<NavigationItem>,
    snackbarHostState: SnackbarHostState,
    hazeState: HazeState?,
    hazeStyle: HazeStyle
) {
    val coroutineScope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface)
    ) {
        // Left Panel: Navigation Rail
        NavigationRail(
            modifier = Modifier
                .fillMaxHeight()
                .installerHazeEffect(hazeState, hazeStyle),
            color = hazeState.getMiuixAppBarColor()
        ) {
            navigationItems.forEachIndexed { index, item ->
                NavigationRailItem(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    icon = item.icon,
                    label = item.label
                )
            }
        }

        // Right Panel: Content + FAB + Snackbar
        Box(modifier = Modifier.weight(1f)) {
            SettingsWideContent(
                navController = navController,
                pagerState = pagerState,
                navigationItems = navigationItems,
                snackbarHostState = snackbarHostState,
                hazeState = hazeState
            )
        }
    }
}

@Composable
private fun SettingsWideContent(
    navController: NavController,
    pagerState: PagerState,
    navigationItems: List<NavigationItem>,
    snackbarHostState: SnackbarHostState,
    hazeState: HazeState?
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.systemBars.union(
            WindowInsets.displayCutout.exclude(
                WindowInsets.displayCutout.only(WindowInsetsSides.Start)
            )
        ),
        snackbarHost = { SnackbarHost(state = snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = pagerState.currentPage == 0,
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                FloatingActionButton(
                    modifier = Modifier.padding(end = 16.dp),
                    containerColor = if (MiuixTheme.isDynamicColor) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.background,
                    shadowElevation = 2.dp,
                    onClick = { navController.navigate(MiuixSettingsScreen.Builder.MiuixEditConfig(null).route) }
                ) {
                    Icon(
                        imageVector = AppIcons.Add,
                        modifier = Modifier.size(40.dp),
                        contentDescription = stringResource(id = R.string.add),
                        tint = if (MiuixTheme.isDynamicColor) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.primary
                    )
                }
            }
        }
    ) { paddingValues ->
        InstallerPagerContent(
            hazeState = hazeState,
            pagerState = pagerState,
            navController = navController,
            navigationItems = navigationItems,
            snackbarHostState = snackbarHostState,
            modifier = Modifier.fillMaxSize(),
            outerPadding = paddingValues
        )
    }
}

@Composable
private fun InstallerPagerContent(
    hazeState: HazeState?,
    pagerState: PagerState,
    navController: NavController,
    navigationItems: List<NavigationItem>,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    outerPadding: PaddingValues
) {
    HorizontalPager(
        state = pagerState,
        userScrollEnabled = true,
        overscrollEffect = null,
        modifier = modifier
    ) { page ->
        when (page) {
            0 -> MiuixAllPage(
                navController = navController,
                hazeState = hazeState,
                title = navigationItems[page].label,
                outerPadding = outerPadding,
                snackbarHostState = snackbarHostState
            )

            1 -> MiuixPreferredPage(
                navController = navController,
                hazeState = hazeState,
                title = navigationItems[page].label,
                outerPadding = outerPadding,
                snackbarHostState = snackbarHostState
            )
        }
    }
}