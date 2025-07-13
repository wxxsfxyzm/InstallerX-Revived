package com.rosan.installer.ui.page.settings.config.apply

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
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
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
import com.rosan.installer.ui.theme.none
import com.rosan.installer.ui.widget.setting.LabelWidget
import com.rosan.installer.ui.widget.toggle.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.absoluteValue


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ApplyPage(
    navController: NavController, id: Long, viewModel: ApplyViewModel = koinViewModel {
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

    Scaffold(
        modifier = Modifier
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
            }, navigationIcon = {
                IconButton(
                    onClick = { navController.navigateUp() },
                    shapes = IconButtonShapes(
                        shape = IconButtonDefaults.smallRoundShape,
                        pressedShape = IconButtonDefaults.smallPressedShape
                    ),
                    colors = IconButtonDefaults.iconButtonColors(
                        // 指定“启用”状态下的内容（图标）颜色
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        // （可选）指定“启用”状态下的容器（背景）颜色
                        containerColor = MaterialTheme.colorScheme.primaryContainer, // 标准 IconButton 背景是透明的
                    )
                ) {
                    Icon(
                        imageVector = AppIcons.ArrowBack,
                        contentDescription = stringResource(id = R.string.back)
                    )
                }
            }, actions = {
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
                    Icon(imageVector = AppIcons.ArrowUp, contentDescription = null)
                }
            }
        }) {
        Box(modifier = Modifier.padding(it)) {
            when {
                viewModel.state.apps.progress is ViewContent.Progress.Loading && viewModel.state.apps.data.isEmpty() -> {
                    // 使用 Box 将加载指示器和文本居中
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // 使用 Column 将指示器和文本垂直排列
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            //  M3E 风格的加载指示器
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

/*@Composable
private fun LabelWidget(text: String) {
    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.titleMedium) {
        Text(text)
    }
}*/

/*@Composable
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
}*/
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

    // 使用 Row 来替代 ToggleRow，并添加 selectableGroup 以提高无障碍性
    Row(
        modifier = Modifier
            .selectableGroup()
            // 使用 clip 和 border 在 Row 外部创建统一的圆角和边框
            .clip(RoundedCornerShape(8.dp)),
        // 使用 ButtonGroupDefaults.ConnectedSpaceBetween (即 0.dp) 来使按钮紧密相连
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        map.forEachIndexed { index, value ->
            // 使用 Material 3 Expressive 的 ToggleButton
            ToggleButton(
                checked = selectedIndex == index,
                onCheckedChange = {
                    // 触发震动反馈
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    // onCheckedChange 会在按钮被点击时触发
                    // 我们直接分发 Action 来更新状态
                    viewModel.dispatch(ApplyViewAction.Order(value.type))
                },
                // 根据按钮位置应用不同的形状，实现 "连接" 的视觉效果
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    map.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                // 自定义颜色
                colors = ToggleButtonDefaults.toggleButtonColors(
                    // 选中时，背景为主题色
                    checkedContainerColor = MaterialTheme.colorScheme.primary,
                    // 选中时，内容（文字）为高对比度颜色
                    checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                    // 未选中时，背景为透明或表面色
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    // 未选中时，内容（文字）颜色
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.semantics { role = Role.RadioButton }
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
