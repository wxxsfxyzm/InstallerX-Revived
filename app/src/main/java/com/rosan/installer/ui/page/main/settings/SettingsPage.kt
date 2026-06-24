// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationItemIconPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarArrangement
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailColors
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.library.FloatingBottomBar
import com.rosan.installer.ui.library.FloatingBottomBarDefaults
import com.rosan.installer.ui.library.FloatingBottomBarItem
import com.rosan.installer.ui.library.FloatingBottomBarMode
import com.rosan.installer.ui.navigation.MainPagerState
import com.rosan.installer.ui.navigation.NavigationTab
import com.rosan.installer.ui.page.main.settings.config.all.AllPage
import com.rosan.installer.ui.page.main.settings.history.HistoryPage
import com.rosan.installer.ui.page.main.settings.home.HomePage
import com.rosan.installer.ui.page.main.settings.preferred.PreferredPage
import com.rosan.installer.ui.theme.installerMaterial3BlurEffect
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop


/**
 * Compact Screen Layout (Portrait/Phone)
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Material3SettingsCompactLayout(
    configCount: Int,
    mainPagerState: MainPagerState,
    tabs: List<NavigationTab>,
    useBlur: Boolean,
    useFloatingBottomBar: Boolean,
    backdrop: LayerBackdrop?,
    isMedium: Boolean
) {
    val navigationWindowInsets = WindowInsets.safeDrawing.only(
        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(),
        bottomBar = {
            if (useFloatingBottomBar) {
                Material3FloatingBottomBar(
                    mainPagerState = mainPagerState,
                    tabs = tabs,
                    configCount = configCount,
                    backdrop = backdrop
                )
            } else {
                RowNavigation(
                    modifier = Modifier.installerMaterial3BlurEffect(backdrop),
                    windowInsets = navigationWindowInsets,
                    tabs = tabs,
                    currentPage = mainPagerState.selectedPage,
                    onPageChanged = { mainPagerState.animateToPage(it) },
                    configCount = configCount,
                    containerColor = if (useBlur) Color.Transparent else BottomAppBarDefaults.containerColor,
                    isMedium = isMedium
                )
            }
        }
    ) { paddingValues ->
        Material3SettingsPagerContent(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(paddingValues)
                .then(backdrop?.let { Modifier.layerBackdrop(backdrop) } ?: Modifier),
            configCount = configCount,
            mainPagerState = mainPagerState,
            tabs = tabs,
            useBlur = useBlur,
            outerPadding = paddingValues
        )
    }
}

/**
 * Wide Screen Layout (Tablet/Landscape)
 */
@Composable
fun Material3SettingsWideScreenLayout(
    configCount: Int,
    mainPagerState: MainPagerState,
    tabs: List<NavigationTab>,
    useBlur: Boolean,
    useFloatingBottomBar: Boolean,
    backdrop: LayerBackdrop?
) {
    val navigationWindowInsets = WindowInsets.safeDrawing.only(
        WindowInsetsSides.Vertical + WindowInsetsSides.Start
    )

    if (useFloatingBottomBar) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(),
            bottomBar = {
                Material3FloatingBottomBar(
                    mainPagerState = mainPagerState,
                    tabs = tabs,
                    configCount = configCount,
                    backdrop = backdrop
                )
            }
        ) { paddingValues ->
            Material3SettingsPagerContent(
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(paddingValues)
                    .then(backdrop?.let { Modifier.layerBackdrop(backdrop) } ?: Modifier),
                configCount = configCount,
                mainPagerState = mainPagerState,
                tabs = tabs,
                useBlur = useBlur,
                outerPadding = paddingValues
            )
        }
    } else {
        Row(modifier = Modifier.fillMaxSize()) {
            ColumnNavigation(
                windowInsets = navigationWindowInsets,
                tabs = tabs,
                currentPage = mainPagerState.selectedPage,
                onPageChanged = { mainPagerState.animateToPage(it) }
            )

            Material3SettingsPagerContent(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .consumeWindowInsets(navigationWindowInsets.only(WindowInsetsSides.Start))
                    .then(backdrop?.let { Modifier.layerBackdrop(backdrop) } ?: Modifier),
                configCount = configCount,
                mainPagerState = mainPagerState,
                tabs = tabs,
                useBlur = useBlur,
                outerPadding = PaddingValues(0.dp) // Rail navigation doesn't overlay bottom content
            )
        }
    }
}

@Composable
private fun Material3FloatingBottomBar(
    mainPagerState: MainPagerState,
    tabs: List<NavigationTab>,
    configCount: Int,
    backdrop: LayerBackdrop?
) {
    val fallbackBackdrop = rememberLayerBackdrop()
    val floatingBackdrop = backdrop ?: fallbackBackdrop

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
                .padding(bottom = 12.dp)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
            selectedIndex = { mainPagerState.selectedPage },
            onSelected = { index ->
                mainPagerState.animateToPage(index)
            },
            backdrop = floatingBackdrop,
            tabsCount = tabs.size,
            mode = if (backdrop != null) FloatingBottomBarMode.Blur else FloatingBottomBarMode.None,
            colors = FloatingBottomBarDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                indicatorColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            tabs.forEachIndexed { index, tab ->
                FloatingBottomBarItem(
                    onClick = {
                        mainPagerState.animateToPage(index)
                    },
                    modifier = Modifier.defaultMinSize(minWidth = 76.dp)
                ) {
                    val showBadge = index == 1 && configCount > 1

                    BadgedBox(
                        badge = {
                            // Badge keeps its own colors defined in ConfigBadge
                            ConfigBadge(showBadge = showBadge, configCount = configCount)
                        }
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                        )
                    }
                    Text(
                        text = tab.label,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
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
 * Reusable Pager Content
 */
@Composable
private fun Material3SettingsPagerContent(
    modifier: Modifier = Modifier,
    configCount: Int,
    mainPagerState: MainPagerState,
    tabs: List<NavigationTab>,
    useBlur: Boolean,
    outerPadding: PaddingValues
) {
    HorizontalPager(
        state = mainPagerState.pagerState,
        userScrollEnabled = true,
        modifier = modifier
    ) { page ->
        // Delegate page content rendering based on the current page index
        when (page) {
            0 -> HomePage(
                useBlur = useBlur,
                title = tabs[page].label,
                outerPadding = outerPadding,
                configCount = configCount,
                onNavigateToProfiles = { mainPagerState.animateToPage(1) }
            )

            1 -> AllPage(
                useBlur = useBlur,
                title = tabs[page].label,
                outerPadding = outerPadding
            )

            2 -> HistoryPage(
                useBlur = useBlur,
                title = tabs[page].label,
                outerPadding = outerPadding
            )

            3 -> PreferredPage(
                useBlur = useBlur,
                title = tabs[page].label,
                outerPadding = outerPadding
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RowNavigation(
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets,
    tabs: List<NavigationTab>,
    currentPage: Int,
    onPageChanged: (Int) -> Unit,
    configCount: Int,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    isMedium: Boolean = false
) {
    ShortNavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentSize(),
        windowInsets = windowInsets,
        containerColor = containerColor,
        arrangement = if (isMedium) ShortNavigationBarArrangement.Centered else ShortNavigationBarArrangement.EqualWeight
    ) {
        tabs.forEachIndexed { index, navigationData ->
            ShortNavigationBarItem(
                selected = currentPage == index,
                onClick = { onPageChanged(index) },
                iconPosition = if (isMedium) NavigationItemIconPosition.Start else NavigationItemIconPosition.Top,
                icon = {
                    val showBadge = index == 1 && configCount > 1

                    BadgedBox(
                        badge = {
                            ConfigBadge(showBadge = showBadge, configCount = configCount)
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
                }
            )
        }
    }
}

@Composable
private fun ColumnNavigation(
    windowInsets: WindowInsets,
    tabs: List<NavigationTab>,
    currentPage: Int,
    onPageChanged: (Int) -> Unit
) {
    val state = rememberWideNavigationRailState()
    val scope = rememberCoroutineScope()

    WideNavigationRail(
        state = state,
        windowInsets = windowInsets,
        colors = WideNavigationRailColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modalContainerColor = WideNavigationRailDefaults.colors().modalContainerColor,
            modalScrimColor = WideNavigationRailDefaults.colors().modalScrimColor,
            modalContentColor = WideNavigationRailDefaults.colors().modalContentColor
        ),
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
        tabs.forEachIndexed { index, navigationTab ->
            WideNavigationRailItem(
                railExpanded = state.targetValue == WideNavigationRailValue.Expanded,
                selected = currentPage == index,
                onClick = { onPageChanged(index) },
                icon = {
                    Icon(
                        imageVector = navigationTab.icon,
                        contentDescription = navigationTab.label
                    )
                },
                label = {
                    Text(text = navigationTab.label)
                }
            )
        }
    }
}

@Composable
private fun ConfigBadge(showBadge: Boolean, configCount: Int) {
    AnimatedVisibility(
        visible = showBadge,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
        label = "badge"
    ) {
        Badge(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        ) {
            Text(configCount.toString())
        }
    }
}
