package com.rosan.installer.ui.page.main.settings.config.all

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.SnackbarResult
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
import com.rosan.installer.ui.theme.none
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewAllPage(
    navController: NavController,
    viewModel: AllViewModel
) {
    LaunchedEffect(Unit) {
        viewModel.navController = navController
    }

    val showFloatingState = remember { mutableStateOf(true) }
    val showFloating by showFloatingState
    val listState = rememberLazyStaggeredGridState()
    val snackBarHostState = remember { SnackbarHostState() }
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

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

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AllViewEvent.DeletedConfig -> {
                    val result = snackBarHostState.showSnackbar(
                        message = viewModel.context.getString(R.string.delete_success),
                        actionLabel = viewModel.context.getString(R.string.restore),
                        withDismissAction = true
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.dispatch(
                            AllViewAction.RestoreDataConfig(
                                configEntity = event.configEntity
                            )
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.none,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                windowInsets = TopAppBarDefaults.windowInsets.add(WindowInsets(left = 12.dp)),
                title = { Text(text = stringResource(id = R.string.config)) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                )
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
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
    ) {
        Box(modifier = Modifier.padding(it)) {
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
                    // TODO Add error handling
                    // Since we don't allow removing default profile,
                    // There is no need to handle an empty state.
                }

                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (!viewModel.state.userReadScopeTips) {
                            ScopeTipCard(viewModel = viewModel)
                            Spacer(modifier = Modifier.size(8.dp))
                        }
                        ShowDataWidget(
                            viewModel = viewModel,
                            listState = listState
                        )
                    }
                }
            }
        }
    }
}