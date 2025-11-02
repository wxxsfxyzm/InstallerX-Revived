package com.rosan.installer.ui.page.miuix.settings.config.apply

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
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
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import com.rosan.installer.ui.page.miuix.widgets.MiuixDropdown
import com.rosan.installer.ui.theme.none
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.ListPopup
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.extra.DropdownImpl
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.Cancel
import top.yukonga.miuix.kmp.icon.icons.useful.ImmersionMore
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

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
    val scrollBehavior = MiuixScrollBehavior()
    val showFloating by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0
        }
    }
    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        contentWindowInsets = WindowInsets.none,
        topBar = {
            Column {
                TopAppBar(
                    scrollBehavior = scrollBehavior,
                    title = stringResource(R.string.config_scope),
                    navigationIcon = {
                        MiuixBackButton(
                            modifier = Modifier.padding(start = 16.dp),
                            icon = MiuixIcons.Useful.Cancel,
                            onClick = { navController.navigateUp() })
                    },
                    actions = { TopAppBarActions(viewModel = viewModel) }
                )
                InputField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    query = viewModel.state.search,
                    onQueryChange = { viewModel.dispatch(ApplyViewAction.Search(it)) },
                    label = stringResource(R.string.search),
                    expanded = false,
                    onExpandedChange = {},
                    onSearch = {}
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showFloating,
                enter = scaleIn(),
                exit = scaleOut(),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                FloatingActionButton(
                    modifier = Modifier.padding(end = 16.dp),
                    containerColor = MiuixTheme.colorScheme.surface,
                    onClick = {
                        coroutineScope.launch {
                            lazyListState.animateScrollToItem(0)
                        }
                    }) {
                    Icon(
                        imageVector = AppIcons.ArrowUp,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.primary
                    )
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
                            InfiniteProgressIndicator()
                            Text(
                                text = stringResource(id = R.string.loading),
                                style = MiuixTheme.textStyles.main
                            )
                        }
                    }
                }

                else -> {
                    val refreshing = viewModel.state.apps.progress is ViewContent.Progress.Loading
                    PullToRefresh(
                        isRefreshing = refreshing,
                        onRefresh = { viewModel.dispatch(ApplyViewAction.LoadApps) },
                        modifier = Modifier.fillMaxSize(),
                        topAppBarScrollBehavior = scrollBehavior,
                        refreshTexts = listOf(
                            stringResource(R.string.pull_to_refresh_hint1),
                            stringResource(R.string.pull_to_refresh_hint2),
                            stringResource(R.string.pull_to_refresh_hint3),
                            stringResource(R.string.pull_to_refresh_hint4)
                        ),
                    ) {
                        data class OrderData(val labelResId: Int, val type: ApplyViewState.OrderType)

                        val orderOptions = remember {
                            listOf(
                                OrderData(R.string.sort_by_label, ApplyViewState.OrderType.Label),
                                OrderData(R.string.sort_by_package_name, ApplyViewState.OrderType.PackageName),
                                OrderData(R.string.sort_by_install_time, ApplyViewState.OrderType.FirstInstallTime)
                            )
                        }

                        // Prepare the list of strings and the current selection index for the dropdown.
                        val dropdownItems = orderOptions.map { stringResource(it.labelResId) }
                        val selectedIndex = orderOptions.indexOfFirst { it.type == viewModel.state.orderType }.coerceAtLeast(0)

                        // Use the StandaloneDropdown instead of SmallTitle.
                        MiuixDropdown(
                            items = dropdownItems,
                            selectedIndex = selectedIndex,
                            onSelectedIndexChange = { newIndex ->
                                // Dispatch the action when a new item is selected.
                                val newOrderType = orderOptions[newIndex].type
                                viewModel.dispatch(ApplyViewAction.Order(newOrderType))
                            }
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Card(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .overScrollVertical()
                        ) {
                            MiuixItemsWidget(
                                modifier = Modifier.fillMaxSize(),
                                viewModel = viewModel,
                                lazyListState = lazyListState
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiuixItemsWidget(
    modifier: Modifier,
    viewModel: ApplyViewModel,
    lazyListState: LazyListState,
) {
    LazyColumn(
        modifier = modifier
            .scrollEndHaptic(),
        state = lazyListState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(8.dp),
        overscrollEffect = null
    ) {
        items(viewModel.state.checkedApps, key = { it.packageName }) {
            var alpha by remember {
                mutableFloatStateOf(0f)
            }
            MiuixItemWidget(
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
private fun MiuixItemWidget(
    modifier: Modifier = Modifier,
    viewModel: ApplyViewModel,
    app: ApplyViewApp,
) {
    val applied =
        viewModel.state.appEntities.data.find { it.packageName == app.packageName } != null
    Box(
        modifier = modifier.fillMaxWidth()
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
                        color = MiuixTheme.colorScheme.primary
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
                    style = MiuixTheme.textStyles.title4
                )
                AnimatedVisibility(viewModel.state.showPackageName) {
                    Text(
                        text = app.packageName,
                        style = MiuixTheme.textStyles.subtitle,
                        color = MiuixTheme.colorScheme.onSurfaceVariantActions
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
                }
            )
        }
    }
}

@Composable
private fun TopAppBarActions(viewModel: ApplyViewModel) {
    val showMenu = remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current

    // Define the options based on the state from the ViewModel.
    // This list will be used to build the menu items.
    val menuOptions = remember(
        viewModel.state.orderInReverse,
        viewModel.state.selectedFirst,
        viewModel.state.showSystemApp,
        viewModel.state.showPackageName
    ) {
        listOf(
            // Pair of (Label String Resource ID, Is Selected Boolean)
            R.string.sort_by_reverse_order to viewModel.state.orderInReverse,
            R.string.sort_by_selected_first to viewModel.state.selectedFirst,
            R.string.sort_by_show_system_app to viewModel.state.showSystemApp,
            R.string.sort_by_show_package_name to viewModel.state.showPackageName
        )
    }

    // Popup menu that appears when the button is clicked.
    ListPopup(
        show = showMenu,
        popupPositionProvider = ListPopupDefaults.ContextMenuPositionProvider,
        alignment = PopupPositionProvider.Align.TopRight,
        onDismissRequest = {
            showMenu.value = false
        },
        enableWindowDim = false
    ) {
        ListPopupColumn {
            menuOptions.forEachIndexed { index, (labelResId, isSelected) ->
                // Reuse DropdownImpl for each menu item.
                // 'isSelected' controls whether the checkmark is shown.
                DropdownImpl(
                    text = stringResource(labelResId),
                    optionSize = menuOptions.size,
                    isSelected = isSelected,
                    onSelectedIndexChange = { selectedIndex ->
                        // When an item is clicked, dispatch the corresponding action to toggle the state.
                        when (selectedIndex) {
                            0 -> viewModel.dispatch(ApplyViewAction.OrderInReverse(!viewModel.state.orderInReverse))
                            1 -> viewModel.dispatch(ApplyViewAction.SelectedFirst(!viewModel.state.selectedFirst))
                            2 -> viewModel.dispatch(ApplyViewAction.ShowSystemApp(!viewModel.state.showSystemApp))
                            3 -> viewModel.dispatch(ApplyViewAction.ShowPackageName(!viewModel.state.showPackageName))
                        }
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                        // Unlike single-choice menus, we don't close the menu here,
                        // allowing the user to toggle multiple options at once.
                    },
                    index = index
                )
            }
        }
    }

    // The "More" button that triggers the popup menu.
    IconButton(
        modifier = Modifier.padding(end = 12.dp),
        onClick = {
            showMenu.value = true
        },
        holdDownState = showMenu.value
    ) {
        Icon(
            imageVector = MiuixIcons.Useful.ImmersionMore,
            tint = MiuixTheme.colorScheme.onBackground,
            contentDescription = "More Options"
        )
    }
}