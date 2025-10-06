package com.rosan.installer.ui.page.miuix.settings.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RoomPreferences
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.rosan.installer.R
import com.rosan.installer.ui.page.main.settings.config.all.AllViewAction
import com.rosan.installer.ui.page.main.settings.config.all.AllViewModel
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.page.miuix.settings.config.all.MiuixAllPage
import com.rosan.installer.ui.page.miuix.settings.preferred.MiuixPreferredPage
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationItem

@Composable
fun MiuixMainPage(navController: NavController, preferredViewModel: PreferredViewModel) {
    val allViewModel: AllViewModel = koinViewModel {
        parametersOf(navController)
    }
    LaunchedEffect(Unit) {
        allViewModel.dispatch(AllViewAction.Init)
    }

    val pages = arrayOf(
        NavigationData(
            icon = Icons.Rounded.RoomPreferences,
            label = stringResource(R.string.config)
        ) {
            MiuixAllPage(navController, allViewModel)
        },
        NavigationData(
            icon = Icons.Rounded.Settings,
            label = stringResource(R.string.preferred)
        ) {
            MiuixPreferredPage(navController, preferredViewModel)
        }
    )

    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val currentPage = pagerState.currentPage

    fun onPageChanged(page: Int) {
        scope.launch { pagerState.animateScrollToPage(page) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
        ) { page ->
            pages[page].content.invoke()
        }

        NavigationBar(
            items = pages.map { NavigationItem(label = it.label, icon = it.icon) },
            selected = currentPage,
            showDivider = true,
            onClick = { index -> onPageChanged(index) }
        )
    }
}
