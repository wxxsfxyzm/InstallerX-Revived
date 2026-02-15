package com.rosan.installer.ui.page.miuix.settings

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.config.all.AllViewAction
import com.rosan.installer.ui.page.main.settings.config.all.AllViewEvent
import com.rosan.installer.ui.page.main.settings.config.all.AllViewModel
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewEvent
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.page.miuix.settings.config.all.MiuixAllPage
import com.rosan.installer.ui.page.miuix.settings.config.apply.MiuixApplyPage
import com.rosan.installer.ui.page.miuix.settings.config.edit.MiuixEditPage
import com.rosan.installer.ui.page.miuix.settings.preferred.MiuixPreferredPage
import com.rosan.installer.ui.page.miuix.settings.preferred.subpage.home.MiuixHomePage
import com.rosan.installer.ui.page.miuix.settings.preferred.subpage.home.ossLicensePage.MiuixOpenSourceLicensePage
import com.rosan.installer.ui.page.miuix.settings.preferred.subpage.installer.MiuixInstallerGlobalSettingsPage
import com.rosan.installer.ui.page.miuix.settings.preferred.subpage.installer.MiuixUninstallerGlobalSettingsPage
import com.rosan.installer.ui.page.miuix.settings.preferred.subpage.lab.MiuixLabPage
import com.rosan.installer.ui.page.miuix.settings.preferred.subpage.theme.MiuixThemeSettingsPage
import com.rosan.installer.ui.page.miuix.widgets.ErrorDisplaySheet
import com.rosan.installer.ui.theme.getMiuixAppBarColor
import com.rosan.installer.ui.theme.installerHazeEffect
import com.rosan.installer.ui.theme.rememberMiuixHazeStyle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarDuration
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.SnackbarResult
import top.yukonga.miuix.kmp.theme.MiuixTheme

private object UIConstants {
    val WIDE_SCREEN_THRESHOLD = 840.dp
    val MEDIUM_WIDTH_THRESHOLD = 600.dp
    const val PORTRAIT_ASPECT_RATIO_THRESHOLD = 1.2f
}

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun MiuixSettingsPage(preferredViewModel: PreferredViewModel) {
    val navController = rememberNavController()
    val useBlur = preferredViewModel.state.useBlur
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
            val allViewModel: AllViewModel = koinViewModel { parametersOf(navController) }
            LaunchedEffect(Unit) {
                allViewModel.dispatch(AllViewAction.Init)
            }

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

            val pagerState = rememberPagerState(pageCount = { navigationItems.size })
            val snackBarHostState = remember { SnackbarHostState() }
            val hazeState = if (useBlur) remember { HazeState() } else null

            // LaunchedEffect to handle snackbar events from AllViewModel
            LaunchedEffect(Unit) {
                allViewModel.eventFlow.collectLatest { event ->
                    when (event) {
                        is AllViewEvent.DeletedConfig -> {
                            val result = snackBarHostState.showSnackbar(
                                message = allViewModel.context.getString(R.string.delete_success),
                                actionLabel = allViewModel.context.getString(R.string.restore),
                                withDismissAction = true
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                allViewModel.dispatch(
                                    AllViewAction.RestoreDataConfig(configEntity = event.configEntity)
                                )
                            }
                        }
                    }
                }
            }

            var errorDialogInfo by remember { mutableStateOf<PreferredViewEvent.ShowDefaultInstallerErrorDetail?>(null) }
            val showErrorSheetState = remember { mutableStateOf(false) }

            val defaultInstallerErrorDetailActionLabel = stringResource(R.string.details)
            LaunchedEffect(Unit) {
                preferredViewModel.uiEvents.collect { event ->
                    //snackBarHostState.newestSnackbarData()?.dismiss()
                    when (event) {
                        is PreferredViewEvent.ShowDefaultInstallerResult -> {
                            snackBarHostState.showSnackbar(event.message)
                        }

                        is PreferredViewEvent.ShowDefaultInstallerErrorDetail -> {
                            val snackbarResult = snackBarHostState.showSnackbar(
                                message = event.title,
                                actionLabel = defaultInstallerErrorDetailActionLabel,
                                duration = SnackbarDuration.Short
                            )
                            if (snackbarResult == SnackbarResult.ActionPerformed) {
                                errorDialogInfo = event
                                showErrorSheetState.value = true
                            }
                        }

                        else -> {}
                    }
                }
            }

            // --- Layout Decision Logic ---
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isDefinitelyWide = maxWidth > UIConstants.WIDE_SCREEN_THRESHOLD
                val isWideByShape =
                    maxWidth > UIConstants.MEDIUM_WIDTH_THRESHOLD && (maxHeight.value / maxWidth.value < UIConstants.PORTRAIT_ASPECT_RATIO_THRESHOLD)
                val isWideScreen = isDefinitelyWide || isWideByShape

                if (isWideScreen) {
                    SettingsWideScreenLayout(
                        navController = navController,
                        pagerState = pagerState,
                        navigationItems = navigationItems,
                        allViewModel = allViewModel,
                        preferredViewModel = preferredViewModel,
                        snackBarHostState = snackBarHostState,
                        hazeState = hazeState
                    )
                } else {
                    SettingsCompactLayout(
                        navController = navController,
                        pagerState = pagerState,
                        navigationItems = navigationItems,
                        allViewModel = allViewModel,
                        preferredViewModel = preferredViewModel,
                        snackBarHostState = snackBarHostState,
                        hazeState = hazeState
                    )
                }
            }

            errorDialogInfo?.let { dialogInfo ->
                ErrorDisplaySheet(
                    showState = showErrorSheetState,
                    exception = dialogInfo.exception,
                    onDismissRequest = { showErrorSheetState.value = false },
                    onRetry = errorDialogInfo?.retryAction?.let { retryAction ->
                        {
                            showErrorSheetState.value = false
                            preferredViewModel.dispatch(retryAction)
                        }
                    },
                    title = dialogInfo.title
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
            MiuixHomePage(navController = navController, viewModel = preferredViewModel)
        }
        composable(route = MiuixSettingsScreen.MiuixOpenSourceLicense.route) {
            MiuixOpenSourceLicensePage(navController = navController, viewModel = preferredViewModel)
        }
        composable(route = MiuixSettingsScreen.MiuixTheme.route) {
            MiuixThemeSettingsPage(navController = navController, viewModel = preferredViewModel)
        }
        composable(route = MiuixSettingsScreen.MiuixInstallerGlobal.route) {
            MiuixInstallerGlobalSettingsPage(navController = navController, viewModel = preferredViewModel)
        }
        composable(route = MiuixSettingsScreen.MiuixUninstallerGlobal.route) {
            MiuixUninstallerGlobalSettingsPage(navController = navController, viewModel = preferredViewModel)
        }
        composable(route = MiuixSettingsScreen.MiuixLab.route) {
            MiuixLabPage(navController = navController, viewModel = preferredViewModel)
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
    allViewModel: AllViewModel,
    preferredViewModel: PreferredViewModel,
    snackBarHostState: SnackbarHostState,
    hazeState: HazeState?
) {
    val coroutineScope = rememberCoroutineScope()
    val hazeStyle = rememberMiuixHazeStyle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.installerHazeEffect(hazeState, hazeStyle),
                // Keep the color configuration from the original installer code
                // to maintain the haze blur effect.
                color = hazeState.getMiuixAppBarColor()
            ) {
                // Iterate through items and use NavigationBarItem to align with the Miuix library example
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
        snackbarHost = { SnackbarHost(state = snackBarHostState) },
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
            allViewModel = allViewModel,
            preferredViewModel = preferredViewModel,
            navigationItems = navigationItems,
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
    allViewModel: AllViewModel,
    preferredViewModel: PreferredViewModel,
    snackBarHostState: SnackbarHostState,
    hazeState: HazeState?
) {
    val coroutineScope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface)
    ) {
        // Left Panel: Navigation Rail
        NavigationRail(
            modifier = Modifier.fillMaxHeight()
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
                allViewModel = allViewModel,
                preferredViewModel = preferredViewModel,
                snackBarHostState = snackBarHostState,
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
    allViewModel: AllViewModel,
    preferredViewModel: PreferredViewModel,
    snackBarHostState: SnackbarHostState,
    hazeState: HazeState?
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.systemBars.union(
            WindowInsets.displayCutout.exclude(
                WindowInsets.displayCutout.only(WindowInsetsSides.Start)
            )
        ),
        snackbarHost = { SnackbarHost(state = snackBarHostState) },
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
            allViewModel = allViewModel,
            preferredViewModel = preferredViewModel,
            navigationItems = navigationItems,
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
    allViewModel: AllViewModel,
    preferredViewModel: PreferredViewModel,
    navigationItems: List<NavigationItem>,
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
                viewModel = allViewModel,
                hazeState = hazeState,
                title = navigationItems[page].label,
                outerPadding = outerPadding
            )

            1 -> MiuixPreferredPage(
                navController = navController,
                viewModel = preferredViewModel,
                hazeState = hazeState,
                title = navigationItems[page].label,
                outerPadding = outerPadding
            )
        }
    }
}