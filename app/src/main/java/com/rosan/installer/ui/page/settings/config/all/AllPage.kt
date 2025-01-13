package com.rosan.installer.ui.page.settings.config.all

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.Rule
import androidx.compose.material.icons.twotone.Add
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.material.icons.twotone.Rule
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.rosan.installer.R
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.ui.page.settings.SettingsScreen
import com.rosan.installer.ui.theme.none
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun AllPage(
    navController: NavController,
    windowInsets: WindowInsets,
    viewModel: AllViewModel = koinViewModel() {
        parametersOf(navController)
    }
) {
    LaunchedEffect(true) {
        viewModel.dispatch(AllViewAction.Init)
        viewModel.navController = navController
    }

    val showFloatingState = remember {
        mutableStateOf(true)
    }
    val showFloating by showFloatingState

    val snackBarHostState = remember {
        SnackbarHostState()
    }

    LaunchedEffect(true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AllViewEvent.DeletedConfig -> {
                    val result = snackBarHostState.showSnackbar(
                        message = viewModel.context.getString(R.string.delete_success),
                        actionLabel = viewModel.context.getString(R.string.restore),
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
            .windowInsetsPadding(windowInsets)
            .nestedScroll(ShowFloatingActionButtonNestedScrollConnection(showFloatingState)),
        contentWindowInsets = WindowInsets.none,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.config))
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showFloating,
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                ExtendedFloatingActionButton(
                    icon = {
                        Icon(
                            imageVector = Icons.TwoTone.Add,
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
            when {
                viewModel.state.data.progress is AllViewState.Data.Progress.Loading
                        && viewModel.state.data.configs.isEmpty() -> {
                    LottieWidget(
                        spec = LottieCompositionSpec.RawRes(R.raw.loading),
                        text = stringResource(id = R.string.loading)
                    )
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
                modifier = Modifier
                    .size(200.dp),
                composition = composition,
                progress = { progress }
            )
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShowDataWidget(
    viewModel: AllViewModel,
) {
    LazyVerticalStaggeredGrid(
        modifier = Modifier.fillMaxSize(),
        columns = StaggeredGridCells.Adaptive(350.dp),
        contentPadding = PaddingValues(16.dp),
        verticalItemSpacing = 16.dp,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(viewModel.state.data.configs) {
            DataItemWidget(viewModel, it)
        }
    }
}

@Composable
fun DataItemWidget(
    viewModel: AllViewModel,
    entity: ConfigEntity
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                    text = entity.name,
                    style = MaterialTheme.typography.titleMedium
                )
                if (entity.description.isNotEmpty()) {
                    Text(
                        text = entity.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.dispatch(AllViewAction.EditDataConfig(entity)) }) {
                    Icon(
                        imageVector = Icons.TwoTone.Edit,
                        contentDescription = stringResource(id = R.string.edit)
                    )
                }
                IconButton(onClick = { viewModel.dispatch(AllViewAction.DeleteDataConfig(entity)) }) {
                    Icon(
                        imageVector = Icons.TwoTone.Delete,
                        contentDescription = stringResource(id = R.string.delete)
                    )
                }
                IconButton(onClick = {
                    viewModel.dispatch(AllViewAction.ApplyConfig(entity))
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.TwoTone.Rule,
                        contentDescription = stringResource(id = R.string.apply)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                var enabled by remember {
                    mutableStateOf(false)
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { enabled = !enabled }
                )
            }
        }
    }
}

class ShowFloatingActionButtonNestedScrollConnection(
    private val showFloatingState: MutableState<Boolean>
) : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (available.y.absoluteValue > 1) showFloatingState.value = available.y >= 0
        return super.onPreScroll(available, source)
    }
}
