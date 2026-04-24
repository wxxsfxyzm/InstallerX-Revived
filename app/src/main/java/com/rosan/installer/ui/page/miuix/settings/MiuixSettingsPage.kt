// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.settings

// -------------------------------------------------------------
// 【关键修改】使用别名导入两套不同的 Backdrop 以避免冲突
// -------------------------------------------------------------
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.navigation.MainPagerState
import com.rosan.installer.ui.navigation.Route
import com.rosan.installer.ui.page.miuix.settings.config.all.MiuixAllPage
import com.rosan.installer.ui.page.miuix.settings.home.MiuixHomePage
import com.rosan.installer.ui.page.miuix.settings.preferred.MiuixPreferredPage
import com.rosan.installer.ui.page.miuix.widgets.FloatingBottomBar
import com.rosan.installer.ui.page.miuix.widgets.FloatingBottomBarItem
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
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.kyant.backdrop.backdrops.LayerBackdrop as KyantLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop as kyantLayerBackdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop as MiuixLayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop as miuixLayerBackdrop

/**
 * Reusable Floating Bottom Bar
 */
@Composable
private fun SettingsFloatingBottomBar(
    mainPagerState: MainPagerState,
    navigationItems: List<NavigationItem>,
    useFloatingBottomBarBlur: Boolean,
    floatingBackdrop: KyantLayerBackdrop
) {
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
            selectedIndex = { mainPagerState.pagerState.targetPage },
            onSelected = { index ->
                mainPagerState.animateToPage(index)
            },
            backdrop = floatingBackdrop,
            tabsCount = navigationItems.size,
            isBlurEnabled = useFloatingBottomBarBlur
        ) {
            navigationItems.forEachIndexed { index, item ->
                FloatingBottomBarItem(
                    onClick = {
                        mainPagerState.animateToPage(index)
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
    configCount: Int,
    mainPagerState: MainPagerState,
    navigationItems: List<NavigationItem>,
    snackbarHostState: SnackbarHostState,
    useFloatingBottomBar: Boolean,
    useFloatingBottomBarBlur: Boolean,
    floatingBackdrop: KyantLayerBackdrop?,
    miuixBackdrop: MiuixLayerBackdrop?
) {
    val navigator = LocalNavigator.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (useFloatingBottomBar && floatingBackdrop != null) {
                SettingsFloatingBottomBar(
                    mainPagerState = mainPagerState,
                    navigationItems = navigationItems,
                    useFloatingBottomBarBlur = useFloatingBottomBarBlur,
                    floatingBackdrop = floatingBackdrop
                )
            } else if (!useFloatingBottomBar) {
                val blurActive = miuixBackdrop != null
                val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
                Box(
                    modifier = Modifier
                        .then(
                            if (blurActive) {
                                Modifier.textureBlur(
                                    backdrop = miuixBackdrop,
                                    shape = RectangleShape,
                                    blurRadius = 25f,
                                    colors = BlurColors(
                                        blendColors = listOf(
                                            BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(0.8f)),
                                        ),
                                    ),
                                )
                            } else {
                                Modifier
                            }
                        )
                        .background(barColor)
                ) {
                    NavigationBar(
                        color = barColor
                    ) {
                        navigationItems.forEachIndexed { index, item ->
                            NavigationBarItem(
                                selected = mainPagerState.pagerState.targetPage == index,
                                onClick = {
                                    mainPagerState.animateToPage(index)
                                },
                                icon = item.icon,
                                label = item.label
                            )
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(state = snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = mainPagerState.pagerState.targetPage == 1,
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
            configCount = configCount,
            mainPagerState = mainPagerState,
            navigationItems = navigationItems,
            snackbarHostState = snackbarHostState,
            modifier = Modifier.fillMaxSize(),
            outerPadding = paddingValues,
            useFloatingBottomBar = useFloatingBottomBar,
            floatingBackdrop = floatingBackdrop,
            miuixBackdrop = miuixBackdrop
        )
    }
}

/**
 * Wide Screen Layout (Tablet/Landscape)
 */
@Composable
fun SettingsWideScreenLayout(
    configCount: Int,
    mainPagerState: MainPagerState,
    navigationItems: List<NavigationItem>,
    snackbarHostState: SnackbarHostState,
    useFloatingBottomBar: Boolean,
    useFloatingBottomBarBlur: Boolean,
    floatingBackdrop: KyantLayerBackdrop?,
    miuixBackdrop: MiuixLayerBackdrop?
) {
    if (useFloatingBottomBar) {
        SettingsWideContent(
            configCount = configCount,
            mainPagerState = mainPagerState,
            navigationItems = navigationItems,
            snackbarHostState = snackbarHostState,
            useFloatingBottomBar = true,
            useFloatingBottomBarBlur = useFloatingBottomBarBlur,
            floatingBackdrop = floatingBackdrop,
            miuixBackdrop = miuixBackdrop
        )
    } else {
        val startInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Start)

        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.surface)
        ) {
            val blurActive = miuixBackdrop != null
            val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .then(
                        if (blurActive) {
                            Modifier.textureBlur(
                                backdrop = miuixBackdrop,
                                shape = RectangleShape,
                                blurRadius = 25f,
                                colors = BlurColors(
                                    blendColors = listOf(
                                        BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(0.8f)),
                                    ),
                                ),
                            )
                        } else {
                            Modifier
                        }
                    )
                    .background(barColor)
            ) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    color = barColor
                ) {
                    navigationItems.forEachIndexed { index, item ->
                        NavigationRailItem(
                            selected = mainPagerState.pagerState.targetPage == index,
                            onClick = {
                                mainPagerState.animateToPage(index)
                            },
                            icon = item.icon,
                            label = item.label
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .consumeWindowInsets(startInsets)
            ) {
                SettingsWideContent(
                    configCount = configCount,
                    mainPagerState = mainPagerState,
                    navigationItems = navigationItems,
                    snackbarHostState = snackbarHostState,
                    useFloatingBottomBar = false,
                    useFloatingBottomBarBlur = false,
                    floatingBackdrop = floatingBackdrop,
                    miuixBackdrop = miuixBackdrop
                )
            }
        }
    }
}

@Composable
private fun SettingsWideContent(
    configCount: Int,
    mainPagerState: MainPagerState,
    navigationItems: List<NavigationItem>,
    snackbarHostState: SnackbarHostState,
    useFloatingBottomBar: Boolean,
    useFloatingBottomBarBlur: Boolean,
    floatingBackdrop: KyantLayerBackdrop?,
    miuixBackdrop: MiuixLayerBackdrop?
) {
    val navigator = LocalNavigator.current
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (useFloatingBottomBar && floatingBackdrop != null) {
                SettingsFloatingBottomBar(
                    mainPagerState = mainPagerState,
                    navigationItems = navigationItems,
                    useFloatingBottomBarBlur = useFloatingBottomBarBlur,
                    floatingBackdrop = floatingBackdrop
                )
            }
        },
        snackbarHost = { SnackbarHost(state = snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = mainPagerState.pagerState.targetPage == 1,
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
            configCount = configCount,
            mainPagerState = mainPagerState,
            navigationItems = navigationItems,
            snackbarHostState = snackbarHostState,
            modifier = Modifier.fillMaxSize(),
            outerPadding = paddingValues,
            useFloatingBottomBar = useFloatingBottomBar,
            floatingBackdrop = floatingBackdrop,
            miuixBackdrop = miuixBackdrop
        )
    }
}

@Composable
private fun SettingsPagerContent(
    configCount: Int,
    modifier: Modifier = Modifier,
    mainPagerState: MainPagerState,
    navigationItems: List<NavigationItem>,
    snackbarHostState: SnackbarHostState,
    outerPadding: PaddingValues,
    useFloatingBottomBar: Boolean,
    floatingBackdrop: KyantLayerBackdrop?,
    miuixBackdrop: MiuixLayerBackdrop?
) {
    HorizontalPager(
        state = mainPagerState.pagerState,
        userScrollEnabled = true,
        overscrollEffect = null,
        beyondViewportPageCount = 1,
        modifier = modifier
            .then(if (useFloatingBottomBar && floatingBackdrop != null) Modifier.kyantLayerBackdrop(floatingBackdrop) else Modifier)
            .then(if (!useFloatingBottomBar && miuixBackdrop != null) Modifier.miuixLayerBackdrop(miuixBackdrop) else Modifier)
    ) { page ->
        val useBlur = floatingBackdrop != null && miuixBackdrop != null
        when (page) {
            0 -> MiuixHomePage(
                enableBlur = useBlur,
                title = navigationItems[page].label,
                configCount = configCount,
                outerPadding = outerPadding,
                snackbarHostState = snackbarHostState,
                onNavigateToProfiles = { mainPagerState.animateToPage(1) }
            )

            1 -> MiuixAllPage(
                enableBlur = useBlur,
                title = navigationItems[page].label,
                outerPadding = outerPadding,
                snackbarHostState = snackbarHostState
            )

            2 -> MiuixPreferredPage(
                enableBlur = useBlur,
                title = navigationItems[page].label,
                outerPadding = outerPadding,
                snackbarHostState = snackbarHostState
            )
        }
    }
}
