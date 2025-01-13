package com.rosan.installer.ui.page.settings.config.apply

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowBack
import androidx.compose.material.icons.automirrored.twotone.Sort
import androidx.compose.material.icons.twotone.ArrowBack
import androidx.compose.material.icons.twotone.ArrowUpward
import androidx.compose.material.icons.twotone.Close
import androidx.compose.material.icons.twotone.LibraryAddCheck
import androidx.compose.material.icons.twotone.Menu
import androidx.compose.material.icons.twotone.Search
import androidx.compose.material.icons.twotone.Shield
import androidx.compose.material.icons.twotone.Sort
import androidx.compose.material.icons.twotone.Visibility
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.rosan.installer.R
import com.rosan.installer.ui.common.ViewContent
import com.rosan.installer.ui.page.installer.dialog.inner.Chip
import com.rosan.installer.ui.theme.none
import com.rosan.installer.ui.widget.toggle.Toggle
import com.rosan.installer.ui.widget.toggle.ToggleRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.absoluteValue


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun ApplyPage(
    navController: NavController, id: Long, viewModel: ApplyViewModel = koinViewModel() {
        parametersOf(id)
    }
) {
    LaunchedEffect(true) {
        viewModel.dispatch(ApplyViewAction.Init)
    }

    val scope = rememberCoroutineScope()

    val showFloatingState = remember {
        mutableStateOf(false)
    }
    var showFloating by showFloatingState

    var showBottomSheet by remember {
        mutableStateOf(false)
    }

    val lazyListState = rememberLazyListState()

    Scaffold(modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .nestedScroll(
            ShowFloatingActionButtonNestedScrollConnection(
                showFloatingState,
                lazyListState
            )
        ),
        contentWindowInsets = WindowInsets.none,
        topBar = {
            var searchBarActivated by remember {
                mutableStateOf(false)
            }
            TopAppBar(title = {
                @Suppress("AnimatedContentLabel") AnimatedContent(targetState = searchBarActivated) {
                    if (!it) Text(stringResource(R.string.app))
                    else {
                        val focusRequester = remember {
                            FocusRequester()
                        }
                        OutlinedTextField(
                            modifier = Modifier.focusRequester(focusRequester),
                            value = viewModel.state.search,
                            onValueChange = { viewModel.dispatch(ApplyViewAction.Search(it)) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.TwoTone.Search, contentDescription = null
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = {
                                    searchBarActivated = false
                                    viewModel.dispatch(ApplyViewAction.Search(""))
                                }) {
                                    Icon(
                                        imageVector = Icons.TwoTone.Close, contentDescription = null
                                    )
                                }
                            },
                            textStyle = MaterialTheme.typography.titleMedium
                        )
                        SideEffect {
                            focusRequester.requestFocus()
                        }
                    }
                }
            }, navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.TwoTone.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            }, actions = {
                AnimatedVisibility(visible = !searchBarActivated) {
                    IconButton(onClick = { searchBarActivated = !searchBarActivated }) {
                        Icon(imageVector = Icons.TwoTone.Search, contentDescription = null)
                    }
                }
                IconButton(onClick = { showBottomSheet = true }) {
                    Icon(imageVector = Icons.TwoTone.Menu, contentDescription = null)
                }
            })
        }, floatingActionButton = {
            AnimatedVisibility(
                visible = showFloating,
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                FloatingActionButton({
                    scope.launch {
                        showFloating = false
                        lazyListState.animateScrollToItem(0)
                    }
                }) {
                    Icon(imageVector = Icons.TwoTone.ArrowUpward, contentDescription = null)
                }
            }
        }) {
        Box(modifier = Modifier.padding(it)) {
            when {
                viewModel.state.apps.progress is ViewContent.Progress.Loading && viewModel.state.apps.data.isEmpty() -> {
                    LottieWidget(
                        contentPadding = it,
                        spec = LottieCompositionSpec.RawRes(R.raw.loading),
                        text = stringResource(id = R.string.loading)
                    )
                }

                else -> {
                    val refreshing = viewModel.state.apps.progress is ViewContent.Progress.Loading
                    val pullRefreshState = rememberPullRefreshState(refreshing = refreshing,
                        onRefresh = { viewModel.dispatch(ApplyViewAction.LoadApps) })
                    Box(modifier = Modifier.pullRefresh(pullRefreshState)) {
                        ItemsWidget(
                            modifier = Modifier.fillMaxSize(),
                            viewModel = viewModel,
                            lazyListState = lazyListState
                        )
                        PullRefreshIndicator(
                            modifier = Modifier.align(Alignment.TopCenter),
                            refreshing = refreshing,
                            state = pullRefreshState
                        )

                    }

                }
            }
        }
    }

    if (showBottomSheet) ModalBottomSheet(onDismissRequest = { showBottomSheet = false }) {
        BottomSheetContent(viewModel)
    }
}

private class ShowFloatingActionButtonNestedScrollConnection(
    private val showFloatingState: MutableState<Boolean>,
    private val lazyListState: LazyListState
) : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (available.y.absoluteValue > 1)
            showFloatingState.value = if (!lazyListState.isScrollInProgress) false
            else available.y > 1 && lazyListState.firstVisibleItemIndex > 1

        return super.onPreScroll(available, source)
    }
}

@Composable
fun LottieWidget(
    contentPadding: PaddingValues, spec: LottieCompositionSpec, text: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
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
            LottieAnimation(modifier = Modifier.size(200.dp),
                composition = composition,
                progress = { progress })
            Text(
                text = text, style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemsWidget(
    modifier: Modifier,
    viewModel: ApplyViewModel,
    lazyListState: LazyListState,
) {
    LazyColumn(
        modifier = modifier,
        state = lazyListState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
//        item {
//            ItemWidget(
//                viewModel = viewModel,
//                app = null
//            )
//        }
        items(viewModel.state.checkedApps, key = { it.packageName }) {
            var alpha by remember {
                mutableFloatStateOf(0f)
            }
            ItemWidget(
                modifier = Modifier.animateItem(
                    fadeInSpec = null, fadeOutSpec = null, placementSpec = spring(
                        stiffness = Spring.StiffnessMediumLow,
                        visibilityThreshold = IntOffset.VisibilityThreshold
                    )
                )
                    .graphicsLayer(
                        alpha = animateFloatAsState(
                            targetValue = alpha,
                            animationSpec = spring(stiffness = 100f), label = ""
                        ).value
                    ),
                viewModel = viewModel,
                app = it
            )
            SideEffect {
                alpha = 1f
            }
        }
    }
}

@Composable
fun ItemWidget(
    modifier: Modifier = Modifier,
    viewModel: ApplyViewModel,
    app: ApplyViewApp,
) {
    val applied =
        viewModel.state.appEntities.data.find { it.packageName == app.packageName } != null
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = {
                        viewModel.dispatch(
                            ApplyViewAction.ApplyPackageName(
                                app.packageName, !applied
                            )
                        )
                    },
                    interactionSource = remember {
                        MutableInteractionSource()
                    },
                    indication = ripple(
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val packageManager = LocalContext.current.packageManager
            val scope = rememberCoroutineScope()
            var icon by remember {
                mutableStateOf(viewModel.defaultIcon)
            }
            SideEffect {
                scope.launch(Dispatchers.IO) {
                    icon = packageManager.getApplicationIcon(app.packageName)
                }
            }
            Image(
                painter = rememberDrawablePainter(icon),
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.CenterVertically),
                contentDescription = null
            )
            Column(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(1f)
            ) {
                Text(
                    text = app.label ?: app.packageName,
                    style = MaterialTheme.typography.titleMedium
                )
                AnimatedVisibility(viewModel.state.showPackageName) {
                    Text(
                        app.packageName, style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Switch(modifier = Modifier.align(Alignment.CenterVertically),
                checked = applied,
                onCheckedChange = {
                    viewModel.dispatch(
                        ApplyViewAction.ApplyPackageName(
                            app.packageName, it
                        )
                    )
                })
        }
    }
}

@Composable
private fun BottomSheetContent(viewModel: ApplyViewModel) {
    Box(modifier = Modifier.fillMaxWidth()) {
        CompositionLocalProvider(LocalContentColor provides AlertDialogDefaults.titleContentColor) {
            ProvideTextStyle(MaterialTheme.typography.headlineSmall) {
                Text(stringResource(R.string.options), modifier = Modifier.align(Alignment.Center))
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OrderWidget(viewModel)
        ChipsWidget(viewModel)
    }
}

@Composable
private fun LabelWidget(text: String) {
    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.titleMedium) {
        Text(text)
    }
}

@Composable
private fun OrderWidget(viewModel: ApplyViewModel) {
    LabelWidget(stringResource(R.string.sort))

    data class OrderData(val labelResId: Int, val type: ApplyViewState.OrderType)

    val map = listOf(
        OrderData(R.string.sort_by_label, ApplyViewState.OrderType.Label),
        OrderData(R.string.sort_by_package_name, ApplyViewState.OrderType.PackageName),
        OrderData(R.string.sort_by_install_time, ApplyViewState.OrderType.FirstInstallTime)
    )

    val selectedIndex = map.map { it.type }.indexOf(viewModel.state.orderType)
    ToggleRow(selectedIndex = selectedIndex) {
        val a = mutableListOf<String>()
        map.forEachIndexed { index, value ->
            Toggle(selected = selectedIndex == index, onSelected = {
                viewModel.dispatch(ApplyViewAction.Order(value.type))
            }) {
                Text(stringResource(value.labelResId))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipsWidget(viewModel: ApplyViewModel) {
    LabelWidget(stringResource(R.string.more))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val orderInReverse = viewModel.state.orderInReverse
        val selectedFirst = viewModel.state.selectedFirst
        val showSystemApp = viewModel.state.showSystemApp
        val showPackageName = viewModel.state.showPackageName
        Chip(
            selected = orderInReverse,
            label = stringResource(R.string.sort_by_reverse_order),
            icon = Icons.AutoMirrored.TwoTone.Sort,
            onClick = { viewModel.dispatch(ApplyViewAction.OrderInReverse(!orderInReverse)) }
        )
        Chip(
            selected = selectedFirst,
            label = stringResource(R.string.sort_by_selected_first),
            icon = Icons.TwoTone.LibraryAddCheck,
            onClick = { viewModel.dispatch(ApplyViewAction.SelectedFirst(!selectedFirst)) }
        )
        Chip(
            selected = showSystemApp,
            label = stringResource(R.string.sort_by_show_system_app),
            icon = Icons.TwoTone.Shield,
            onClick = { viewModel.dispatch(ApplyViewAction.ShowSystemApp(!showSystemApp)) }
        )
        Chip(
            selected = showPackageName,
            label = stringResource(R.string.sort_by_show_package_name),
            icon = Icons.TwoTone.Visibility,
            onClick = { viewModel.dispatch(ApplyViewAction.ShowPackageName(!showPackageName)) }
        )
    }
}
