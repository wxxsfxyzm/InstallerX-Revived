package com.rosan.installer.ui.page.miuix.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RoomPreferences
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.rosan.installer.ui.page.miuix.settings.preferred.subpage.installer.MiuixInstallerGlobalSettingsPage
import com.rosan.installer.ui.page.miuix.settings.preferred.subpage.lab.MiuixLabPage
import com.rosan.installer.ui.page.miuix.settings.preferred.subpage.theme.MiuixThemeSettingsPage
import com.rosan.installer.ui.page.miuix.widgets.ErrorDisplaySheet
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun MiuixSettingsPage(preferredViewModel: PreferredViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()

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
            val coroutineScope = rememberCoroutineScope()
            val snackBarHostState = remember { SnackbarHostState() }
            val hazeState = remember { HazeState() }
            val hazeStyle = HazeStyle(
                backgroundColor = MiuixTheme.colorScheme.surface,
                tint = HazeTint(MiuixTheme.colorScheme.surface.copy(0.8f))
            )

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
            LaunchedEffect(Unit) {
                preferredViewModel.uiEvents.collect { event ->
                    snackBarHostState.currentSnackbarData?.dismiss()
                    when (event) {
                        is PreferredViewEvent.ShowDefaultInstallerResult -> {
                            snackBarHostState.showSnackbar(event.message)
                        }

                        is PreferredViewEvent.ShowDefaultInstallerErrorDetail -> {
                            val snackbarResult = snackBarHostState.showSnackbar(
                                message = event.title,
                                actionLabel = context.getString(R.string.details),
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

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier.hazeEffect(hazeState) {
                            style = hazeStyle
                            blurRadius = 30.dp
                            noiseFactor = 0f
                        },
                        color = Color.Transparent,
                        items = navigationItems,
                        selected = pagerState.currentPage,
                        showDivider = true,
                        onClick = { index ->
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    )
                },
                snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
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

            errorDialogInfo?.let { dialogInfo ->
                ErrorDisplaySheet(
                    showState = showErrorSheetState,
                    exception = dialogInfo.exception,
                    onDismissRequest = { showErrorSheetState.value = false }, // Hide sheet on dismiss
                    onRetry = errorDialogInfo?.retryAction?.let { retryAction ->
                        {
                            showErrorSheetState.value = false // Hide sheet
                            preferredViewModel.dispatch(retryAction) // Then execute retry action
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
                id = if (id != -1L) id
                else null
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
        composable(route = MiuixSettingsScreen.MiuixTheme.route) {
            MiuixThemeSettingsPage(navController = navController, viewModel = preferredViewModel)

        }
        composable(route = MiuixSettingsScreen.MiuixInstallerGlobal.route) {
            MiuixInstallerGlobalSettingsPage(navController = navController, viewModel = preferredViewModel)
        }
        composable(route = MiuixSettingsScreen.MiuixLab.route) {
            MiuixLabPage(navController = navController, viewModel = preferredViewModel)
        }
    }
}

@Composable
private fun InstallerPagerContent(
    hazeState: HazeState,
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