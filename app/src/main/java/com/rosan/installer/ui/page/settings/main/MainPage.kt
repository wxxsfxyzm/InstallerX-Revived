package com.rosan.installer.ui.page.settings.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Home
import androidx.compose.material.icons.twotone.RoomPreferences
import androidx.compose.material.icons.twotone.SettingsSuggest
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.rosan.installer.R
import com.rosan.installer.ui.page.settings.config.all.AllPage
import com.rosan.installer.ui.page.settings.home.HomePage
import com.rosan.installer.ui.page.settings.preferred.PreferredPage
import com.rosan.installer.ui.theme.exclude
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun MainPage(navController: NavController) {
    val data = arrayOf(
        NavigationData(
            icon = Icons.TwoTone.Home,
            label = stringResource(R.string.home)
        ) {
            HomePage(navController, it)
        },
        NavigationData(
            icon = Icons.TwoTone.RoomPreferences,
            label = stringResource(R.string.config)
        ) {
            AllPage(navController, it)
        },
        NavigationData(
            icon = Icons.TwoTone.SettingsSuggest,
            label = stringResource(R.string.preferred)
        ) {
            PreferredPage(navController, it)
        }
    )

    val pagerState = rememberPagerState(pageCount = { data.size })
    val currentPage = pagerState.currentPage
    fun onPageChanged(page: Int) {
        GlobalScope.launch(Dispatchers.Main) {
            pagerState.scrollToPage(page = page)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val isLandscapeScreen = maxHeight / maxWidth > 1.4

        val navigationSide =
            if (isLandscapeScreen) WindowInsetsSides.Bottom
            else WindowInsetsSides.Left

        val navigationWindowInsets = WindowInsets.safeDrawing.only(
            (if (isLandscapeScreen) WindowInsetsSides.Horizontal
            else WindowInsetsSides.Vertical) + navigationSide
        )
        val pageWindowInsets = WindowInsets.safeDrawing.exclude(navigationSide)

        Row(
            modifier = Modifier
                .fillMaxSize()
        ) {
            if (!isLandscapeScreen) {
                ColumnNavigation(
                    windowInsets = navigationWindowInsets,
                    data = data,
                    currentPage = currentPage,
                    onPageChanged = { onPageChanged(it) }
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = false,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                ) { page ->
                    data[page].content.invoke(pageWindowInsets)
                }
                if (isLandscapeScreen) {
                    RowNavigation(
                        windowInsets = navigationWindowInsets,
                        data = data,
                        currentPage = currentPage,
                        onPageChanged = { onPageChanged(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun RowNavigation(
    windowInsets: WindowInsets,
    data: Array<NavigationData>,
    currentPage: Int,
    onPageChanged: (Int) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(),
        windowInsets = windowInsets
    ) {
        data.forEachIndexed { index, navigationData ->
            NavigationBarItem(
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
                },
                alwaysShowLabel = false
            )
        }
    }
}

@Composable
fun ColumnNavigation(
    windowInsets: WindowInsets,
    data: Array<NavigationData>,
    currentPage: Int,
    onPageChanged: (Int) -> Unit
) {
    NavigationRail(
        modifier = Modifier
            .fillMaxHeight()
            .wrapContentSize(),
        windowInsets = windowInsets
    ) {
        Spacer(
            modifier = Modifier
                .weight(1f)
        )
        data.forEachIndexed { index, navigationData ->
            NavigationRailItem(
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
                },
                alwaysShowLabel = false
            )
        }
    }
}
