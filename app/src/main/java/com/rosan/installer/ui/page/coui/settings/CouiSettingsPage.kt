// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar as M3NavigationBar
import androidx.compose.material3.NavigationBarItem as M3NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itos.coui.effect.CouiFloatingRenderer
import com.itos.coui.floatingactionbutton.Fab
import com.itos.coui.icons.CouiIcons
import com.itos.coui.layout.Scaffold
import com.itos.coui.navigation.CouiNavigationBarAction
import com.itos.coui.navigation.CouiNavigationBarDefaults
import com.itos.coui.navigation.CouiNavigationItem
import com.itos.coui.navigation.NavigationBar
import com.itos.coui.theme.CouiTheme
import com.rosan.installer.R
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.navigation.MainPagerState
import com.rosan.installer.ui.navigation.Route
import com.rosan.installer.ui.page.coui.settings.config.all.CouiAllPage
import com.rosan.installer.ui.page.coui.settings.history.CouiHistoryPage
import com.rosan.installer.ui.page.coui.settings.home.CouiHomePage
import com.rosan.installer.ui.page.coui.settings.preferred.CouiPreferredPage

@Composable
fun CouiSettingsCompactLayout(
    configCount: Int,
    mainPagerState: MainPagerState,
    navigationItems: List<CouiNavigationItem>,
    snackbarHostState: SnackbarHostState,
    useBlur: Boolean,
) {
    val navigator = LocalNavigator.current
    val addLabel = stringResource(R.string.add)
    val floatingRenderer = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            runCatching { CouiFloatingRenderer() }.getOrNull()
        } else {
            null
        }
    }
    val useCouiFloatingNavBar =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && floatingRenderer != null

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CouiTheme.colorScheme.background,
        bottomBar = {
            if (useCouiFloatingNavBar && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val addIcon = CouiIcons.Filled.Add
                val rightActionStyle = CouiNavigationBarDefaults.coloredActionStyle()
                NavigationBar(
                    renderer = floatingRenderer,
                    items = navigationItems,
                    selectedIndex = mainPagerState.selectedPage,
                    rightAction = CouiNavigationBarAction(
                        icon = addIcon,
                        contentDescription = addLabel,
                        onClick = { navigator.push(Route.EditConfig(-1)) },
                        visible = mainPagerState.selectedPage == 1,
                        style = rightActionStyle,
                    ),
                    onSelection = { mainPagerState.animateToPage(it) },
                )
            } else {
                M3NavigationBar(
                    containerColor = CouiTheme.colorScheme.card,
                ) {
                    navigationItems.forEachIndexed { index, item ->
                        val selected = mainPagerState.selectedPage == index
                        M3NavigationBarItem(
                            selected = selected,
                            onClick = { mainPagerState.animateToPage(index) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.icon,
                                    contentDescription = item.title,
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                    }
                }
            }
        },
        floatingActionButton = if (!useCouiFloatingNavBar) {
            {
                AnimatedVisibility(
                    visible = mainPagerState.selectedPage == 1,
                    enter = scaleIn(),
                    exit = scaleOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp),
                ) {
                    FloatingActionButton(
                        containerColor = CouiTheme.colorScheme.controlAccent,
                        contentColor = Color.White,
                        onClick = { navigator.push(Route.EditConfig(-1)) },
                    ) {
                        Icon(
                            imageVector = CouiIcons.Filled.Add,
                            contentDescription = addLabel,
                        )
                    }
                }
            }
        } else {
            null
        },
    ) { outerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            CouiSettingsPagerContent(
                configCount = configCount,
                mainPagerState = mainPagerState,
                navigationItems = navigationItems,
                snackbarHostState = snackbarHostState,
                outerPadding = outerPadding,
                useBlur = useBlur,
                modifier = Modifier.fillMaxSize(),
            )
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = outerPadding.calculateBottomPadding() + 8.dp),
            )
        }
    }
}

@Composable
fun CouiSettingsWideScreenLayout(
    configCount: Int,
    mainPagerState: MainPagerState,
    navigationItems: List<CouiNavigationItem>,
    snackbarHostState: SnackbarHostState,
    useBlur: Boolean,
) {
    val navigator = LocalNavigator.current
    val addLabel = stringResource(R.string.add)
    val contentWindowInsets = WindowInsets.systemBars.union(
        WindowInsets.displayCutout.exclude(
            WindowInsets.displayCutout.only(WindowInsetsSides.Start),
        ),
    )
    val startInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
        .only(WindowInsetsSides.Start)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(CouiTheme.colorScheme.background),
    ) {
        NavigationRail(
            containerColor = CouiTheme.colorScheme.background,
        ) {
            navigationItems.forEachIndexed { index, item ->
                val selected = mainPagerState.selectedPage == index
                NavigationRailItem(
                    selected = selected,
                    onClick = { mainPagerState.animateToPage(index) },
                    icon = {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.icon,
                            contentDescription = item.title,
                        )
                    },
                    label = {
                        Text(
                            text = item.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .consumeWindowInsets(startInsets),
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = CouiTheme.colorScheme.background,
                windowInsets = contentWindowInsets,
                floatingActionButton = {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = mainPagerState.selectedPage == 1,
                        enter = scaleIn(),
                        exit = scaleOut(),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 20.dp, bottom = 20.dp),
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            Fab(onClick = { navigator.push(Route.EditConfig(-1)) }) {
                                Icon(
                                    imageVector = CouiIcons.Filled.Add,
                                    contentDescription = addLabel,
                                )
                            }
                        } else {
                            FloatingActionButton(
                                containerColor = CouiTheme.colorScheme.controlAccent,
                                contentColor = Color.White,
                                onClick = { navigator.push(Route.EditConfig(-1)) },
                            ) {
                                Icon(
                                    imageVector = CouiIcons.Filled.Add,
                                    contentDescription = addLabel,
                                )
                            }
                        }
                    }
                },
            ) { outerPadding ->
                Box(modifier = Modifier.fillMaxSize()) {
                    CouiSettingsPagerContent(
                        configCount = configCount,
                        mainPagerState = mainPagerState,
                        navigationItems = navigationItems,
                        snackbarHostState = snackbarHostState,
                        outerPadding = outerPadding,
                        useBlur = useBlur,
                        modifier = Modifier.fillMaxSize(),
                    )
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = outerPadding.calculateBottomPadding() + 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CouiSettingsPagerContent(
    configCount: Int,
    mainPagerState: MainPagerState,
    navigationItems: List<CouiNavigationItem>,
    snackbarHostState: SnackbarHostState,
    outerPadding: PaddingValues,
    useBlur: Boolean,
    modifier: Modifier = Modifier,
) {
    HorizontalPager(
        state = mainPagerState.pagerState,
        userScrollEnabled = true,
        overscrollEffect = null,
        beyondViewportPageCount = 1,
        modifier = modifier,
    ) { page ->
        when (page) {
            0 -> CouiHomePage(
                enableBlur = useBlur,
                title = navigationItems[page].title,
                configCount = configCount,
                outerPadding = outerPadding,
                snackbarHostState = snackbarHostState,
                onNavigateToProfiles = { mainPagerState.animateToPage(1) },
            )

            1 -> CouiAllPage(
                enableBlur = useBlur,
                title = navigationItems[page].title,
                outerPadding = outerPadding,
                snackbarHostState = snackbarHostState,
            )

            2 -> CouiHistoryPage(
                enableBlur = useBlur,
                title = navigationItems[page].title,
                outerPadding = outerPadding,
            )

            3 -> CouiPreferredPage(
                enableBlur = useBlur,
                title = navigationItems[page].title,
                outerPadding = outerPadding,
                snackbarHostState = snackbarHostState,
            )
        }
    }
}
