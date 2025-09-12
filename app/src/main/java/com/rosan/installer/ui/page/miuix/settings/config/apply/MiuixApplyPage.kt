package com.rosan.installer.ui.page.miuix.settings.config.apply

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.Sort
import androidx.compose.material.icons.twotone.LibraryAddCheck
import androidx.compose.material.icons.twotone.Shield
import androidx.compose.material.icons.twotone.Visibility
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.rosan.installer.R
import com.rosan.installer.ui.common.ViewContent
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.config.apply.ApplyViewAction
import com.rosan.installer.ui.page.main.settings.config.apply.ApplyViewApp
import com.rosan.installer.ui.page.main.settings.config.apply.ApplyViewModel
import com.rosan.installer.ui.page.main.settings.config.apply.ApplyViewState
import com.rosan.installer.ui.page.main.settings.config.apply.ItemWidget
import com.rosan.installer.ui.page.main.settings.config.apply.ItemsWidget
import com.rosan.installer.ui.page.main.widget.chip.Chip
import com.rosan.installer.ui.page.main.widget.setting.AppBackButton
import com.rosan.installer.ui.page.main.widget.setting.LabelWidget
import com.rosan.installer.ui.theme.InstallerMaterialExpressiveTheme
import com.rosan.installer.ui.theme.none
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MiuixApplyPage(
    navController: NavController,
    id: Long,
    viewModel: ApplyViewModel = koinViewModel {
        parametersOf(id)
    }
) {
    LaunchedEffect(Unit) { viewModel.dispatch(ApplyViewAction.Init) }

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showBottomSheet by remember { mutableStateOf(false) }
    val showFloating by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0
        }
    }
    InstallerMaterialExpressiveTheme {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentWindowInsets = WindowInsets.none,
            topBar = {
                var searchBarActivated by remember { mutableStateOf(false) }
                TopAppBar(
                    scrollBehavior = scrollBehavior,
                    title = {
                        AnimatedContent(targetState = searchBarActivated) {
                            if (!it) Text(stringResource(R.string.app))
                            else {
                                val focusRequester = remember { FocusRequester() }
                                OutlinedTextField(
                                    modifier = Modifier.focusRequester(focusRequester),
                                    value = viewModel.state.search,
                                    onValueChange = { viewModel.dispatch(ApplyViewAction.Search(it)) },
                                    singleLine = true,
                                    leadingIcon = {
                                        Icon(
                                            imageVector = AppIcons.Search,
                                            contentDescription = stringResource(R.string.search)
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(
                                            shapes = IconButtonShapes(
                                                shape = IconButtonDefaults.smallRoundShape,
                                                pressedShape = IconButtonDefaults.smallPressedShape
                                            ),
                                            onClick = {
                                                searchBarActivated = false
                                                viewModel.dispatch(ApplyViewAction.Search(""))
                                            }) {
                                            Icon(
                                                imageVector = AppIcons.Close,
                                                contentDescription = stringResource(R.string.close)
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
                    },
                    navigationIcon = {
                        AppBackButton(onClick = { navController.navigateUp() })
                    },
                    actions = {
                        AnimatedVisibility(visible = !searchBarActivated) {
                            IconButton(
                                onClick = { searchBarActivated = !searchBarActivated }) {
                                Icon(
                                    imageVector = AppIcons.Search,
                                    contentDescription = stringResource(R.string.search)
                                )
                            }
                        }
                        IconButton(onClick = { showBottomSheet = true }) {
                            Icon(
                                imageVector = AppIcons.Menu,
                                contentDescription = stringResource(R.string.menu)
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = showFloating,
                    enter = scaleIn(),
                    exit = scaleOut(),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    FloatingActionButton({
                        coroutineScope.launch {
                            lazyListState.animateScrollToItem(0)
                        }
                    }) {
                        Icon(imageVector = AppIcons.ArrowUp, contentDescription = null)
                    }
                }
            }) {
            Box(modifier = Modifier.padding(it)) {
                when {
                    viewModel.state.apps.progress is ViewContent.Progress.Loading && viewModel.state.apps.data.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                ContainedLoadingIndicator()
                                Text(
                                    text = stringResource(id = R.string.loading),
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                    }

                    else -> {
                        val refreshing = viewModel.state.apps.progress is ViewContent.Progress.Loading
                        val pullToRefreshState = rememberPullToRefreshState()
                        // 使用 PullToRefreshBox 作为根容器
                        PullToRefreshBox(
                            state = pullToRefreshState,
                            isRefreshing = refreshing,
                            onRefresh = { viewModel.dispatch(ApplyViewAction.LoadApps) },
                            modifier = Modifier.fillMaxSize(), // 将修饰符应用在这里
                            indicator = {
                                //  将 Indicator 替换为 LoadingIndicator
                                PullToRefreshDefaults.LoadingIndicator(
                                    modifier = Modifier.align(Alignment.TopCenter),
                                    state = pullToRefreshState,
                                    isRefreshing = refreshing,
                                    color = MaterialTheme.colorScheme.primary,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                )
                            }
                        ) {
                            ItemsWidget(
                                modifier = Modifier.fillMaxSize(),
                                viewModel = viewModel,
                                lazyListState = lazyListState
                            )
                            // PullToRefreshBox 默认已经包含了一个居中对齐的指示器 (Indicator)。
                            // 不需要再手动添加 PullRefreshIndicator。
                            // 如果需要自定义指示器，可以使用 indicator 参数。
                        }

                    }

                }
            }
        }

        if (showBottomSheet) ModalBottomSheet(onDismissRequest = { showBottomSheet = false }) {
            BottomSheetContent(viewModel)
        }
    }
}

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
        items(viewModel.state.checkedApps, key = { it.packageName }) {
            var alpha by remember {
                mutableFloatStateOf(0f)
            }
            ItemWidget(
                modifier = Modifier
                    .animateItem(
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
            Switch(
                modifier = Modifier.align(Alignment.CenterVertically),
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OrderWidget(viewModel: ApplyViewModel) {
    val haptic = LocalHapticFeedback.current

    LabelWidget(stringResource(R.string.sort), 0)

    data class OrderData(val labelResId: Int, val type: ApplyViewState.OrderType)

    val map = listOf(
        OrderData(R.string.sort_by_label, ApplyViewState.OrderType.Label),
        OrderData(R.string.sort_by_package_name, ApplyViewState.OrderType.PackageName),
        OrderData(R.string.sort_by_install_time, ApplyViewState.OrderType.FirstInstallTime)
    )

    val selectedIndex = map.map { it.type }.indexOf(viewModel.state.orderType)

    Row(
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        val modifiers = List(map.size) { Modifier.weight(1f) } // 根据需要调整权重

        map.forEachIndexed { index, value ->
            ToggleButton(
                checked = selectedIndex == index,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    viewModel.dispatch(ApplyViewAction.Order(value.type))
                },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    map.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = ToggleButtonDefaults.toggleButtonColors(
                    checkedContainerColor = MaterialTheme.colorScheme.primary,
                    checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = modifiers[index]
                    .semantics { role = Role.RadioButton }
            ) {
                Text(stringResource(value.labelResId))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipsWidget(viewModel: ApplyViewModel) {
    LabelWidget(stringResource(R.string.more), 0)
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
