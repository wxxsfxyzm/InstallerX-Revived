package com.rosan.installer.ui.page.miuix.settings.config.all

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.rosan.installer.R
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.config.all.AllViewAction
import com.rosan.installer.ui.page.main.settings.config.all.AllViewEvent
import com.rosan.installer.ui.page.main.settings.config.all.AllViewModel
import com.rosan.installer.ui.page.main.settings.config.all.AllViewState
import com.rosan.installer.ui.page.miuix.settings.MiuixSettingsScreen
import kotlinx.coroutines.flow.collectLatest
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.Delete
import top.yukonga.miuix.kmp.icon.icons.useful.Edit
import top.yukonga.miuix.kmp.icon.icons.useful.SelectAll
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
fun MiuixAllPage(
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
    // Use MiuixScrollBehavior for TopAppBar
    val scrollBehavior = MiuixScrollBehavior()

    // Logic to show/hide FAB based on scroll direction
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
                if (showFloatingState.value == isScrollingDown) {
                    showFloatingState.value = !isScrollingDown
                }
            }
    }

    // Event collection for Snackbar
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
                            AllViewAction.RestoreDataConfig(configEntity = event.configEntity)
                        )
                    }
                }
            }
        }
    }

    // Use Miuix Scaffold
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            // Use Miuix TopAppBar
            TopAppBar(
                title = stringResource(id = R.string.config),
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showFloating,
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                // Use Miuix FloatingActionButton
                FloatingActionButton(
                    modifier = Modifier.padding(end = 16.dp),
                    containerColor = MiuixTheme.colorScheme.surface,
                    onClick = { navController.navigate(MiuixSettingsScreen.Builder.MiuixEditConfig(null).route) }
                ) {
                    Icon(
                        imageVector = AppIcons.Add,
                        contentDescription = stringResource(id = R.string.add),
                        tint = MiuixTheme.colorScheme.primary
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier.padding(top = innerPadding.calculateTopPadding())
        ) {
            when {
                viewModel.state.data.progress is AllViewState.Data.Progress.Loading
                        && viewModel.state.data.configs.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            InfiniteProgressIndicator()
                            Text(
                                text = stringResource(id = R.string.loading),
                                style = MiuixTheme.textStyles.main
                            )
                        }
                    }
                }

                viewModel.state.data.progress is AllViewState.Data.Progress.Loaded
                        && viewModel.state.data.configs.isEmpty() -> {
                    LottieWidget(
                        spec = LottieCompositionSpec.RawRes(R.raw.empty_state),
                        text = stringResource(id = R.string.empty_configs)
                    )
                }

                else -> {
                    ShowDataWidget(
                        viewModel = viewModel,
                        listState = listState
                    )
                }
            }
        }
    }
}

@Composable
fun LottieWidget(
    spec: LottieCompositionSpec,
    text: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val composition by rememberLottieComposition(spec)
            val progress by animateLottieCompositionAsState(
                composition = composition,
                iterations = LottieConstants.IterateForever,
            )
            LottieAnimation(
                modifier = Modifier.size(200.dp),
                composition = composition,
                progress = { progress }
            )
            Text(
                text = text,
                style = MiuixTheme.textStyles.main
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShowDataWidget(
    viewModel: AllViewModel,
    listState: LazyStaggeredGridState = rememberLazyStaggeredGridState()
) {
    LazyVerticalStaggeredGrid(
        modifier = Modifier
            .fillMaxSize()
            .overScrollVertical(),
        columns = StaggeredGridCells.Adaptive(350.dp),
        contentPadding = PaddingValues(16.dp),
        verticalItemSpacing = 16.dp,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        overscrollEffect = null,
        state = listState,
    ) {
        items(viewModel.state.data.configs) {
            DataItemWidget(viewModel, it)
        }
    }
}

@Composable
private fun DataItemWidget(
    viewModel: AllViewModel,
    entity: ConfigEntity
) {
    // Use Miuix Card
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                    text = entity.name,
                    style = MiuixTheme.textStyles.headline1
                )
                if (entity.description.isNotEmpty()) {
                    Text(
                        text = entity.description,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.subtitle
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                minHeight = 35.dp,
                minWidth = 35.dp,
                backgroundColor = MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                onClick = { viewModel.dispatch(AllViewAction.MiuixEditDataConfig(entity)) }) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = MiuixIcons.Useful.Edit,
                    tint = MiuixTheme.colorScheme.onSurface.copy(alpha = if (isSystemInDarkTheme()) 0.7f else 0.9f),
                    contentDescription = stringResource(id = R.string.edit)
                )
            }
            IconButton(
                minHeight = 35.dp,
                minWidth = 35.dp,
                backgroundColor = MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                onClick = { viewModel.dispatch(AllViewAction.DeleteDataConfig(entity)) }) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = MiuixIcons.Useful.Delete,
                    tint = MiuixTheme.colorScheme.onSurface.copy(alpha = if (isSystemInDarkTheme()) 0.7f else 0.9f),
                    contentDescription = stringResource(id = R.string.delete)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                minHeight = 35.dp,
                minWidth = 35.dp,
                backgroundColor = MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                onClick = { viewModel.dispatch(AllViewAction.ApplyConfig(entity)) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = MiuixIcons.Useful.SelectAll,
                        tint = MiuixTheme.colorScheme.onSurface.copy(alpha = if (isSystemInDarkTheme()) 0.7f else 0.9f),
                        contentDescription = stringResource(id = R.string.apply)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        modifier = Modifier.padding(end = 3.dp),
                        text = "作用域",
                        color = MiuixTheme.colorScheme.onSurface.copy(alpha = if (isSystemInDarkTheme()) 0.7f else 0.9f),
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}