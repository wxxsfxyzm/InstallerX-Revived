package com.rosan.installer.ui.page.main.settings.main

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FlexibleBottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailColors
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.config.all.AllPage
import com.rosan.installer.ui.page.main.settings.config.all.AllViewAction
import com.rosan.installer.ui.page.main.settings.config.all.AllViewModel
import com.rosan.installer.ui.page.main.settings.config.all.NewAllPage
import com.rosan.installer.ui.page.main.settings.preferred.NewPreferredPage
import com.rosan.installer.ui.page.main.settings.preferred.PreferredPage
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.theme.installerHazeEffect
import com.rosan.installer.ui.theme.rememberMaterial3HazeStyle
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(DelicateCoroutinesApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainPage(navController: NavController, preferredViewModel: PreferredViewModel) {
    val allViewModel: AllViewModel = koinViewModel {
        parametersOf(navController)
    }
    LaunchedEffect(Unit) {
        allViewModel.dispatch(AllViewAction.Init)
    }
    val showExpressiveUI = preferredViewModel.state.showExpressiveUI
    val useBlur = showExpressiveUI && preferredViewModel.state.useBlur
    val hazeState = if (useBlur) remember { HazeState() } else null

    val configCount = allViewModel.state.data.configs.size
    val configLabel = stringResource(R.string.config)
    val preferredLabel = stringResource(R.string.preferred)

    val data = remember(showExpressiveUI, configLabel, preferredLabel, navController, allViewModel, preferredViewModel) {
        arrayOf(
            NavigationData(
                icon = AppIcons.RoomPreferences,
                label = configLabel
            ) { outerPadding, hazeState ->
                if (showExpressiveUI) NewAllPage(navController, allViewModel, outerPadding = outerPadding, hazeState = hazeState)
                else AllPage(navController, allViewModel, outerPadding)
            },
            NavigationData(
                icon = AppIcons.SettingsSuggest,
                label = preferredLabel
            ) { outerPadding, hazeState ->
                if (showExpressiveUI) NewPreferredPage(navController, preferredViewModel, outerPadding = outerPadding, hazeState = hazeState)
                else PreferredPage(navController, preferredViewModel, outerPadding)
            }
        )
    }

    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { data.size })
    val currentPage = pagerState.currentPage
    fun onPageChanged(page: Int) {
        scope.launch {
            pagerState.animateScrollToPage(page = page)
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val isPortrait = maxWidth < maxHeight || (maxHeight / maxWidth > 1.4f)

        val navigationSide =
            if (isPortrait) WindowInsetsSides.Bottom
            else WindowInsetsSides.Start

        val navigationWindowInsets = WindowInsets.safeDrawing.only(
            (if (isPortrait) WindowInsetsSides.Horizontal
            else WindowInsetsSides.Vertical) + navigationSide
        )

        val hazeStyle = rememberMaterial3HazeStyle()

        if (isPortrait) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    RowNavigation(
                        modifier = Modifier.installerHazeEffect(hazeState, hazeStyle),
                        isM3e = showExpressiveUI,
                        windowInsets = navigationWindowInsets,
                        data = data,
                        currentPage = currentPage,
                        onPageChanged = { onPageChanged(it) },
                        configCount = configCount,
                        containerColor = if (useBlur) Color.Transparent else BottomAppBarDefaults.containerColor
                    )
                }
            ) { paddingValues ->
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = true,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    data[page].content(paddingValues, if (showExpressiveUI) hazeState else null)
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                ColumnNavigation(
                    isM3e = showExpressiveUI,
                    windowInsets = navigationWindowInsets,
                    data = data,
                    currentPage = currentPage,
                    onPageChanged = { onPageChanged(it) }
                )

                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = true,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                ) { page ->
                    data[page].content(PaddingValues(0.dp), if (showExpressiveUI) hazeState else null)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RowNavigation(
    modifier: Modifier = Modifier,
    isM3e: Boolean,
    windowInsets: WindowInsets,
    data: Array<NavigationData>,
    currentPage: Int,
    onPageChanged: (Int) -> Unit,
    configCount: Int,
    containerColor: Color = if (isM3e) MaterialTheme.colorScheme.surfaceContainer else BottomAppBarDefaults.containerColor
) {
    FlexibleBottomAppBar(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentSize(),
        windowInsets = windowInsets,
        expandedHeight = if (isM3e) BottomAppBarDefaults.FlexibleBottomAppBarHeight else 72.dp,
        containerColor = containerColor,
        horizontalArrangement = BottomAppBarDefaults.FlexibleHorizontalArrangement,
        content = {
            data.forEachIndexed { index, navigationData ->
                NavigationBarItem(
                    selected = currentPage == index,
                    onClick = { onPageChanged(index) },
                    icon = {
                        val showBadge = index == 0 && configCount > 1

                        BadgedBox(
                            badge = {
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = showBadge,
                                    enter = scaleIn() + fadeIn(),
                                    exit = scaleOut() + fadeOut(),
                                    label = "badge"
                                ) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = MaterialTheme.colorScheme.onSecondary
                                    ) { Text(configCount.toString()) }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = navigationData.icon,
                                contentDescription = navigationData.label
                            )
                        }
                    },
                    label = {
                        Text(text = navigationData.label)
                    },
                    alwaysShowLabel = isM3e
                )
            }
        }
    )
}

@Composable
fun ColumnNavigation(
    isM3e: Boolean,
    windowInsets: WindowInsets,
    data: Array<NavigationData>,
    currentPage: Int,
    onPageChanged: (Int) -> Unit
) {
    val state = rememberWideNavigationRailState()
    val scope = rememberCoroutineScope()

    WideNavigationRail(
        state = state,
        windowInsets = windowInsets,
        colors = if (isM3e) WideNavigationRailColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modalContainerColor = WideNavigationRailDefaults.colors().modalContainerColor,
            modalScrimColor = WideNavigationRailDefaults.colors().modalScrimColor,
            modalContentColor = WideNavigationRailDefaults.colors().modalContentColor
        ) else WideNavigationRailDefaults.colors(),
        header = {
            IconButton(
                modifier =
                    Modifier
                        .padding(start = 24.dp)
                        .semantics {
                            stateDescription =
                                if (state.currentValue == WideNavigationRailValue.Expanded) {
                                    "Expanded"
                                } else {
                                    "Collapsed"
                                }
                        },
                onClick = {
                    scope.launch {
                        if (state.targetValue == WideNavigationRailValue.Expanded)
                            state.collapse()
                        else state.expand()
                    }
                },
            ) {
                if (state.targetValue == WideNavigationRailValue.Expanded) {
                    Icon(AppIcons.MenuOpen, "Collapse rail")
                } else {
                    Icon(AppIcons.Menu, "Expand rail")
                }
            }
        }
    ) {
        data.forEachIndexed { index, navigationData ->
            WideNavigationRailItem(
                railExpanded = state.targetValue == WideNavigationRailValue.Expanded,
                selected = currentPage == index,
                onClick = { onPageChanged(index) },
                icon = {
                    Icon(
                        imageVector = navigationData.icon,
                        contentDescription = navigationData.label
                    )
                },
                label = {
                    Text(text = navigationData.label)
                }
            )
        }
    }
}