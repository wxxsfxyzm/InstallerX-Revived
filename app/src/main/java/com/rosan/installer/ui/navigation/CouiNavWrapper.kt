// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.navigation

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RoomPreferences
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.itos.coui.icons.CouiIcons
import com.itos.coui.navigation.CouiNavigationItem
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.preferences.ThemeState
import com.rosan.installer.domain.settings.repository.ConfigRepository
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.coui.settings.CouiSettingsCompactLayout
import com.rosan.installer.ui.page.coui.settings.CouiSettingsWideScreenLayout
import com.rosan.installer.ui.page.main.settings.SettingsSharedViewModel
import com.rosan.installer.ui.theme.LocalWindowLayoutInfo
import com.rosan.installer.ui.theme.WindowLayoutType
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject

@Composable
fun CouiMainPageWrapper(uiState: ThemeState, sharedViewModel: SettingsSharedViewModel) {
    val layoutInfo = LocalWindowLayoutInfo.current
    val sharedState by sharedViewModel.state.collectAsStateWithLifecycle()

    val configRepo = koinInject<ConfigRepository>()
    val configCountFlow = remember { configRepo.flowAll().map { it.size } }
    val configCount by configCountFlow.collectAsStateWithLifecycle(initialValue = 0)

    val homeLabel = stringResource(R.string.home)
    val homeIcon = ImageVector.vectorResource(R.drawable.ic_tile_icon)
    val configLabel = stringResource(R.string.config)
    val historyLabel = stringResource(R.string.history)
    val preferredLabel = stringResource(R.string.preferred)
    val settingOutlined = CouiIcons.Outlined.Setting
    val settingFilled = CouiIcons.Filled.Setting

    val navigationItems = remember(
        homeLabel,
        homeIcon,
        configLabel,
        historyLabel,
        preferredLabel,
        settingOutlined,
        settingFilled,
    ) {
        listOf(
            CouiNavigationItem(
                title = homeLabel,
                icon = homeIcon,
                selectedIcon = homeIcon,
            ),
            CouiNavigationItem(
                title = configLabel,
                icon = Icons.Rounded.RoomPreferences,
                selectedIcon = Icons.Rounded.RoomPreferences,
            ),
            CouiNavigationItem(
                title = historyLabel,
                icon = AppIcons.History,
                selectedIcon = AppIcons.History,
            ),
            CouiNavigationItem(
                title = preferredLabel,
                icon = settingOutlined,
                selectedIcon = settingFilled,
            ),
        )
    }

    val pagerState = rememberPagerState(
        initialPage = sharedState.lastMainPageIndex,
        pageCount = { navigationItems.size },
    )
    val mainPagerState = rememberMainPagerState(pagerState)
    val currentPage = mainPagerState.pagerState.currentPage
    val settledPage = mainPagerState.pagerState.settledPage
    LaunchedEffect(currentPage) {
        mainPagerState.syncPage()
    }
    LaunchedEffect(settledPage) {
        if (sharedState.lastMainPageIndex != settledPage) {
            sharedViewModel.updateLastMainPageIndex(settledPage)
        }
    }
    MainScreenBackHandler(
        mainPagerState = mainPagerState,
        navController = LocalNavigator.current,
    )

    val snackbarHostState = remember { SnackbarHostState() }

    if (layoutInfo.type == WindowLayoutType.EXPANDED || layoutInfo.showNavigationRail) {
        CouiSettingsWideScreenLayout(
            configCount = configCount,
            mainPagerState = mainPagerState,
            navigationItems = navigationItems,
            snackbarHostState = snackbarHostState,
            useBlur = uiState.useBlur,
        )
    } else {
        CouiSettingsCompactLayout(
            configCount = configCount,
            mainPagerState = mainPagerState,
            navigationItems = navigationItems,
            snackbarHostState = snackbarHostState,
            useBlur = uiState.useBlur,
        )
    }
}
