package com.rosan.installer.ui.page.main.settings.config.all

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.SettingsScreen
import com.rosan.installer.ui.page.main.widget.card.ScopeTipCard
import com.rosan.installer.ui.page.main.widget.card.ShowDataWidget
import com.rosan.installer.ui.page.main.widget.setting.DeleteEventCollector
import com.rosan.installer.ui.theme.getM3TopBarColor
import com.rosan.installer.ui.theme.installerHazeEffect
import com.rosan.installer.ui.theme.none
import com.rosan.installer.ui.theme.rememberMaterial3HazeStyle
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewAllPage(
    navController: NavController,
    viewModel: AllViewModel,
    outerPadding: PaddingValues = PaddingValues(0.dp),
    hazeState: HazeState? = null
) {
    LaunchedEffect(Unit) {
        viewModel.navController = navController
    }

    val listState = rememberLazyStaggeredGridState()
    val hazeStyle = rememberMaterial3HazeStyle()
    val snackBarHostState = remember { SnackbarHostState() }
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val showFloatingState = remember { mutableStateOf(true) }
    val showFloating by showFloatingState

    LaunchedEffect(listState) {
        var previousIndex = listState.firstVisibleItemIndex
        var previousOffset = listState.firstVisibleItemScrollOffset

        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collectLatest { (index, offset) ->
                val isScrollingDown = when {
                    index > previousIndex -> true
                    index < previousIndex -> false
                    else -> offset > previousOffset
                }

                previousIndex = index
                previousOffset = offset

                val newShowFloating = !isScrollingDown
                if (showFloatingState.value != newShowFloating) {
                    showFloatingState.value = newShowFloating
                }
            }
    }

    DeleteEventCollector(viewModel, snackBarHostState)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.none,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                modifier = Modifier.installerHazeEffect(hazeState, hazeStyle),
                windowInsets = TopAppBarDefaults.windowInsets.add(WindowInsets(left = 12.dp)),
                title = { Text(text = stringResource(id = R.string.config)) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = hazeState.getM3TopBarColor(),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    scrolledContainerColor = hazeState.getM3TopBarColor()
                )
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                modifier = Modifier.padding(bottom = outerPadding.calculateBottomPadding()),
                visible = showFloating,
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                SmallExtendedFloatingActionButton(
                    icon = {
                        Icon(
                            imageVector = AppIcons.Add,
                            contentDescription = stringResource(id = R.string.add)
                        )
                    },
                    text = {
                        Text(text = stringResource(id = R.string.add))
                    },
                    onClick = {
                        navController.navigate(SettingsScreen.Builder.EditConfig(null).route)
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (viewModel.state.data.progress) {
                is AllViewState.Data.Progress.Loading if viewModel.state.data.configs.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ContainedLoadingIndicator(
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                            Text(
                                text = stringResource(id = R.string.loading),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }

                is AllViewState.Data.Progress.Loaded if viewModel.state.data.configs.isEmpty() -> {
                    // Since we don't allow removing default profile,
                    // There is no need to handle an empty state.
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = innerPadding.calculateTopPadding())
                    ) {
                        if (!viewModel.state.userReadScopeTips) {
                            ScopeTipCard(viewModel = viewModel)
                            Spacer(modifier = Modifier.size(8.dp))
                        }
                        ShowDataWidget(
                            viewModel = viewModel,
                            listState = listState,
                            hazeState = hazeState,
                            contentPadding = PaddingValues(
                                top = 16.dp,
                                bottom = outerPadding.calculateBottomPadding() + 16.dp,
                                start = 16.dp,
                                end = 16.dp
                            )
                        )
                    }
                }
            }
        }
    }
}