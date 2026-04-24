// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.navigation

import android.os.Build
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RoomPreferences
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.ThemeState
import com.rosan.installer.domain.settings.repository.ConfigRepository
import com.rosan.installer.ui.page.main.settings.SettingsSharedViewModel
import com.rosan.installer.ui.page.miuix.settings.SettingsCompactLayout
import com.rosan.installer.ui.page.miuix.settings.SettingsWideScreenLayout
import com.rosan.installer.ui.theme.LocalWindowLayoutInfo
import com.rosan.installer.ui.theme.WindowLayoutType
import com.rosan.installer.ui.theme.rememberMiuixBlurBackdrop
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.SnackbarHostState

// Centralized complex layout logic to keep provider clean and readable
@Composable
fun MiuixMainPageWrapper(
    uiState: ThemeState,
    sharedViewModel: SettingsSharedViewModel
) {
    val layoutInfo = LocalWindowLayoutInfo.current
    val sharedState by sharedViewModel.state.collectAsStateWithLifecycle()
    val useBlur = uiState.useBlur
    val useFloatingBottomBar = uiState.useAppleFloatingBar
    val useFloatingBottomBarBlur =
        useBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    val configRepo = koinInject<ConfigRepository>()
    val configCountFlow = remember { configRepo.flowAll().map { it.size } }
    val configCount by configCountFlow.collectAsStateWithLifecycle(initialValue = 0)
    val homeLabel = stringResource(R.string.home)
    val homeIcon = ImageVector.vectorResource(R.drawable.ic_tile_icon)
    val configLabel = stringResource(R.string.config)
    val preferredLabel = stringResource(R.string.preferred)

    val navigationItems = remember(homeLabel, configLabel, preferredLabel) {
        listOf(
            NavigationItem(
                label = homeLabel,
                icon = homeIcon
            ),
            NavigationItem(
                label = configLabel,
                icon = Icons.Rounded.RoomPreferences
            ),
            NavigationItem(
                label = preferredLabel,
                icon = Icons.Rounded.Settings
            )
        )
    }

    val pagerState = rememberPagerState(
        initialPage = sharedState.lastMainPageIndex,
        pageCount = { navigationItems.size }
    )
    val mainPagerState = rememberMainPagerState(pagerState)
    val settledPage = mainPagerState.pagerState.settledPage
    LaunchedEffect(settledPage) {
        mainPagerState.syncPage()
        if (sharedState.lastMainPageIndex != settledPage) {
            sharedViewModel.updateLastMainPageIndex(settledPage)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // Create separated backdrops for different blurred components
    val floatingBackdrop = com.kyant.backdrop.backdrops.rememberLayerBackdrop()
    val miuixBackdrop = rememberMiuixBlurBackdrop(useBlur)

    // Branch statically without layout delay traps
    if (layoutInfo.type == WindowLayoutType.EXPANDED || layoutInfo.showNavigationRail) {
        SettingsWideScreenLayout(
            configCount = configCount,
            mainPagerState = mainPagerState,
            navigationItems = navigationItems,
            snackbarHostState = snackbarHostState,
            useFloatingBottomBar = useFloatingBottomBar,
            useFloatingBottomBarBlur = useFloatingBottomBarBlur,
            floatingBackdrop = floatingBackdrop,
            miuixBackdrop = miuixBackdrop
        )
    } else {
        SettingsCompactLayout(
            configCount = configCount,
            mainPagerState = mainPagerState,
            navigationItems = navigationItems,
            snackbarHostState = snackbarHostState,
            useFloatingBottomBar = useFloatingBottomBar,
            useFloatingBottomBarBlur = useFloatingBottomBarBlur,
            floatingBackdrop = floatingBackdrop,
            miuixBackdrop = miuixBackdrop
        )
    }
}
