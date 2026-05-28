// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.ui.navigation

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.preferences.ThemeState
import com.rosan.installer.domain.settings.repository.ConfigRepository
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.Material3SettingsCompactLayout
import com.rosan.installer.ui.page.main.settings.Material3SettingsWideScreenLayout
import com.rosan.installer.ui.page.main.settings.SettingsSharedViewModel
import com.rosan.installer.ui.theme.LocalWindowLayoutInfo
import com.rosan.installer.ui.theme.rememberMaterial3BlurBackdrop
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject

@Immutable
data class NavigationTab(
    val icon: ImageVector,
    val label: String
)

@Composable
fun Material3MainPageWrapper(
    uiState: ThemeState,
    sharedViewModel: SettingsSharedViewModel
) {
    val sharedState by sharedViewModel.state.collectAsStateWithLifecycle()
    val useBlur = uiState.useBlur
    val useFloatingBottomBar = uiState.useAppleFloatingBar
    val backdrop = rememberMaterial3BlurBackdrop(useBlur)

    val configRepo = koinInject<ConfigRepository>()
    val configCountFlow = remember { configRepo.flowAll().map { it.size } }
    val configCount by configCountFlow.collectAsStateWithLifecycle(initialValue = 0)
    val homeLabel = stringResource(id = R.string.home)
    val homeIcon = ImageVector.vectorResource(R.drawable.ic_tile_icon)
    val configLabel = stringResource(R.string.config)
    val historyLabel = stringResource(R.string.history)
    val preferredLabel = stringResource(R.string.preferred)

    val tabs = remember(homeLabel, configLabel, historyLabel, preferredLabel) {
        listOf(
            NavigationTab(
                icon = homeIcon,
                label = homeLabel
            ),
            NavigationTab(
                icon = AppIcons.RoomPreferences,
                label = configLabel
            ),
            NavigationTab(
                icon = AppIcons.History,
                label = historyLabel
            ),
            NavigationTab(
                icon = AppIcons.SettingsSuggest,
                label = preferredLabel
            )
        )
    }

    val pagerState = rememberPagerState(
        initialPage = sharedState.lastMainPageIndex,
        pageCount = { tabs.size }
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

    val layoutInfo = LocalWindowLayoutInfo.current
    val showRail = layoutInfo.showNavigationRail
    val isMedium = layoutInfo.isMediumPortrait

    // Branch statically without layout delay traps
    if (showRail) {
        Material3SettingsWideScreenLayout(
            configCount = configCount,
            mainPagerState = mainPagerState,
            tabs = tabs,
            useBlur = useBlur,
            useFloatingBottomBar = useFloatingBottomBar,
            backdrop = backdrop
        )
    } else {
        Material3SettingsCompactLayout(
            configCount = configCount,
            mainPagerState = mainPagerState,
            tabs = tabs,
            useBlur = useBlur,
            useFloatingBottomBar = useFloatingBottomBar,
            backdrop = backdrop,
            isMedium = isMedium
        )
    }
}
