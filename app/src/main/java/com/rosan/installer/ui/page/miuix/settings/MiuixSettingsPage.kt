// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.navigation.Route
import com.rosan.installer.ui.page.miuix.settings.config.all.MiuixAllPage
import com.rosan.installer.ui.page.miuix.settings.preferred.MiuixPreferredPage
import com.rosan.installer.ui.page.miuix.widgets.FloatingBottomBar
import com.rosan.installer.ui.page.miuix.widgets.FloatingBottomBarItem
import com.rosan.installer.ui.theme.getMiuixAppBarColor
import com.rosan.installer.ui.theme.installerHazeEffect
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import kotlinx.coroutines.launch
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
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Reusable Floating Bottom Bar
 */
@Composable
private fun SettingsFloatingBottomBar(
    pagerState: PagerState,
    navigationItems: List<NavigationItem>,
    useFloatingBottomBarBlur: Boolean,
    backdrop: LayerBackdrop
) {
    val coroutineScope = rememberCoroutineScope()
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        FloatingBottomBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(
                    bottom = 12.dp + WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding()
                ),
            selectedIndex = { pagerState.currentPage },
            onSelected = { index ->
                coroutineScope.launch {
                    pagerState.animateScrollToPage(index)
                }
            },
            backdrop = backdrop,
            tabsCount = navigationItems.size,
            isBlurEnabled = useFloatingBottomBarBlur
        ) {
            navigationItems.forEachIndexed { index, item ->
                FloatingBottomBarItem(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    modifier = Modifier.defaultMinSize(minWidth = 76.dp)
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = MiuixTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        color = MiuixTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible
                    )
                }
            }
        }
    }
}

/**
 * Compact Screen Layout (Portrait/Phone)
 */
@Composable
fun SettingsCompactLayout(
    pagerState: PagerState,
    navigationItems: List<NavigationItem>,
    snackbarHostState: SnackbarHostState,
    useFloatingBottomBar: Boolean,
    useFloatingBottomBarBlur: Boolean,
    hazeState: HazeState?,
    hazeStyle: HazeStyle,
    backdrop: LayerBackdrop
) {
    val navigator = LocalNavigator.current
    val coroutineScope = rememberCoroutineScope()

    // Removed explicit contentWindowInsets override.
    // Let the Scaffold pass default insets (which won't consume status bar cutouts),
    // allowing inner pages to handle their own statusBarsPadding().
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (useFloatingBottomBar) {
                SettingsFloatingBottomBar(
                    pagerState = pagerState,
                    navigationItems = navigationItems,
                    useFloatingBottomBarBlur = useFloatingBottomBarBlur,
                    backdrop = backdrop
                )
            } else {
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
            }
        },
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
                    onClick = { navigator.push(Route.EditConfig(-1)) }
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
        SettingsPagerContent(
            hazeState = hazeState,
            pagerState = pagerState,
            navigationItems = navigationItems,
            snackbarHostState = snackbarHostState,
            modifier = Modifier.fillMaxSize(),
            outerPadding = paddingValues,
            useFloatingBottomBar = useFloatingBottomBar,
            backdrop = backdrop
        )
    }
}

/**
 * Wide Screen Layout (Tablet/Landscape)
 */
@Composable
fun SettingsWideScreenLayout(
    pagerState: PagerState,
    navigationItems: List<NavigationItem>,
    snackbarHostState: SnackbarHostState,
    useFloatingBottomBar: Boolean,
    useFloatingBottomBarBlur: Boolean,
    hazeState: HazeState?,
    hazeStyle: HazeStyle,
    backdrop: LayerBackdrop
) {
    val coroutineScope = rememberCoroutineScope()

    if (useFloatingBottomBar) {
        SettingsWideContent(
            pagerState = pagerState,
            navigationItems = navigationItems,
            snackbarHostState = snackbarHostState,
            useFloatingBottomBar = true,
            useFloatingBottomBarBlur = useFloatingBottomBarBlur,
            backdrop = backdrop,
            hazeState = hazeState
        )
    } else {
        // Calculate the insets consumed by the NavigationRail on the start side
        val startInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Start)

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
            Box(
                modifier = Modifier
                    .weight(1f)
                    // Only consume start insets so the side bar area isn't duplicated.
                    // Top and bottom insets are left for the inner pages to handle.
                    .consumeWindowInsets(startInsets)
            ) {
                SettingsWideContent(
                    pagerState = pagerState,
                    navigationItems = navigationItems,
                    snackbarHostState = snackbarHostState,
                    useFloatingBottomBar = false,
                    useFloatingBottomBarBlur = false,
                    backdrop = null,
                    hazeState = hazeState
                )
            }
        }
    }
}

@Composable
private fun SettingsWideContent(
    pagerState: PagerState,
    navigationItems: List<NavigationItem>,
    snackbarHostState: SnackbarHostState,
    useFloatingBottomBar: Boolean,
    useFloatingBottomBarBlur: Boolean,
    backdrop: LayerBackdrop?,
    hazeState: HazeState?
) {
    val navigator = LocalNavigator.current
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (useFloatingBottomBar && backdrop != null) {
                SettingsFloatingBottomBar(
                    pagerState = pagerState,
                    navigationItems = navigationItems,
                    useFloatingBottomBarBlur = useFloatingBottomBarBlur,
                    backdrop = backdrop
                )
            }
        },
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
                    onClick = { navigator.push(Route.EditConfig(-1)) }
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
        SettingsPagerContent(
            hazeState = hazeState,
            pagerState = pagerState,
            navigationItems = navigationItems,
            snackbarHostState = snackbarHostState,
            modifier = Modifier.fillMaxSize(),
            outerPadding = paddingValues,
            useFloatingBottomBar = useFloatingBottomBar,
            backdrop = backdrop
        )
    }
}

@Composable
private fun SettingsPagerContent(
    hazeState: HazeState?,
    pagerState: PagerState,
    navigationItems: List<NavigationItem>,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    outerPadding: PaddingValues,
    useFloatingBottomBar: Boolean,
    backdrop: LayerBackdrop?
) {
    HorizontalPager(
        state = pagerState,
        userScrollEnabled = true,
        overscrollEffect = null,
        beyondViewportPageCount = 1,
        modifier = modifier
            .then(if (backdrop != null && useFloatingBottomBar) Modifier.layerBackdrop(backdrop) else Modifier)
    ) { page ->
        when (page) {
            0 -> MiuixAllPage(
                hazeState = hazeState,
                title = navigationItems[page].label,
                outerPadding = outerPadding,
                snackbarHostState = snackbarHostState
            )

            1 -> MiuixPreferredPage(
                hazeState = hazeState,
                title = navigationItems[page].label,
                outerPadding = outerPadding,
                snackbarHostState = snackbarHostState
            )
        }
    }
}
