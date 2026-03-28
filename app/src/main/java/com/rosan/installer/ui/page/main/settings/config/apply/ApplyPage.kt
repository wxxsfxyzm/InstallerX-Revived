// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.config.apply

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.Sort
import androidx.compose.material.icons.twotone.LibraryAddCheck
import androidx.compose.material.icons.twotone.Shield
import androidx.compose.material.icons.twotone.Visibility
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.widget.chip.Chip
import com.rosan.installer.ui.page.main.widget.setting.AppBackButton
import com.rosan.installer.ui.page.main.widget.setting.ApplyItemWidget
import com.rosan.installer.ui.page.main.widget.setting.LabelWidget
import com.rosan.installer.ui.theme.none
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ApplyPage(
    navController: NavController,
    id: Long,
    viewModel: ApplyViewModel = koinViewModel {
        parametersOf(id)
    }
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showBottomSheet by remember { mutableStateOf(false) }
    val showFloating by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0
        }
    }

    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.none,
        topBar = {
            var searchBarActivated by remember { mutableStateOf(false) }
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    AnimatedContent(targetState = searchBarActivated) {
                        if (!it) Text(stringResource(R.string.config_scope))
                        else {
                            val focusRequester = remember { FocusRequester() }
                            OutlinedTextField(
                                modifier = Modifier.focusRequester(focusRequester),
                                value = uiState.search,
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
                modifier = Modifier.padding(
                    end = horizontalSafeInsets.calculateEndPadding(layoutDirection),
                    bottom = 16.dp
                )
            ) {
                FloatingActionButton({
                    coroutineScope.launch {
                        lazyListState.animateScrollToItem(0)
                    }
                }) {
                    Icon(imageVector = AppIcons.ArrowUp, contentDescription = null)
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.apps.progress is ViewContent.Progress.Loading && uiState.apps.data.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = stringResource(id = R.string.loading),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }

                else -> {
                    val refreshing = uiState.apps.progress is ViewContent.Progress.Loading
                    val pullToRefreshState = rememberPullToRefreshState()
                    PullToRefreshBox(
                        state = pullToRefreshState,
                        isRefreshing = refreshing,
                        onRefresh = { viewModel.dispatch(ApplyViewAction.LoadApps) },
                        modifier = Modifier.fillMaxSize(),
                        indicator = {
                            PullToRefreshDefaults.Indicator(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = paddingValues.calculateTopPadding()),
                                state = pullToRefreshState,
                                isRefreshing = refreshing,
                                color = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        }
                    ) {
                        ItemsWidget(
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(scrollBehavior.nestedScrollConnection),
                            uiState = uiState,
                            viewModel = viewModel,
                            lazyListState = lazyListState,
                            topPadding = paddingValues.calculateTopPadding(),
                            bottomPadding = paddingValues.calculateBottomPadding(),
                            startPadding = horizontalSafeInsets.calculateStartPadding(layoutDirection),
                            endPadding = horizontalSafeInsets.calculateEndPadding(layoutDirection)
                        )
                        Spacer(modifier = Modifier.navigationBarsPadding())
                    }
                }
            }
        }
    }

    if (showBottomSheet) ModalBottomSheet(onDismissRequest = { showBottomSheet = false }) {
        BottomSheetContent(uiState = uiState, viewModel = viewModel)
    }
}

@Composable
private fun ItemsWidget(
    modifier: Modifier,
    uiState: ApplyViewState,
    viewModel: ApplyViewModel,
    lazyListState: LazyListState,
    topPadding: Dp = 0.dp,
    bottomPadding: Dp = 0.dp,
    startPadding: Dp = 0.dp,
    endPadding: Dp = 0.dp
) {
    // Optimize lookup performance by converting the list to a Set.
    // Use derivedStateOf to ensure it only recalculates when the data actually changes.
    val appliedPackageSet by remember(uiState.appEntities.data) {
        derivedStateOf {
            uiState.appEntities.data.map { it.packageName }.toHashSet()
        }
    }

    LazyColumn(
        modifier = modifier,
        state = lazyListState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(
            start = startPadding + 8.dp,
            top = topPadding + 8.dp,
            end = endPadding + 8.dp,
            bottom = bottomPadding + 88.dp
        )
    ) {
        items(
            items = uiState.checkedApps,
            key = { it.packageName },
            contentType = { "app_item" } // Help Compose recycle items more efficiently
        ) { app ->
            val isApplied = appliedPackageSet.contains(app.packageName)

            // Dispatch action to load the icon when the item becomes visible
            LaunchedEffect(app.packageName) {
                viewModel.dispatch(ApplyViewAction.LoadIcon(app.packageName))
            }

            // Retrieve the dynamically loaded icon from the state
            val iconBitmap = uiState.displayIcons[app.packageName]

            ApplyItemWidget(
                modifier = Modifier.animateItem(
                    // Handle reordering animations with a spring effect
                    placementSpec = spring(
                        stiffness = Spring.StiffnessMediumLow,
                        visibilityThreshold = IntOffset.VisibilityThreshold
                    )
                ),
                app = app,
                icon = iconBitmap, // Pass the managed state
                isApplied = isApplied,
                isM3e = false,
                showPackageName = uiState.showPackageName,
                onToggle = { isChecked ->
                    viewModel.dispatch(ApplyViewAction.ApplyPackageName(app.packageName, isChecked))
                },
                onClick = {
                    viewModel.dispatch(ApplyViewAction.ApplyPackageName(app.packageName, !isApplied))
                }
            )
        }
    }
}

@Composable
private fun BottomSheetContent(uiState: ApplyViewState, viewModel: ApplyViewModel) {
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
        OrderWidget(uiState = uiState, viewModel = viewModel)
        ChipsWidget(uiState = uiState, viewModel = viewModel)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OrderWidget(
    uiState: ApplyViewState,
    viewModel: ApplyViewModel
) {
    val haptic = LocalHapticFeedback.current

    LabelWidget(stringResource(R.string.sort), 0.dp)

    data class OrderData(val labelResId: Int, val type: ApplyViewState.OrderType)

    val map = listOf(
        OrderData(R.string.sort_by_label, ApplyViewState.OrderType.Label),
        OrderData(R.string.sort_by_package_name, ApplyViewState.OrderType.PackageName),
        OrderData(R.string.sort_by_install_time, ApplyViewState.OrderType.FirstInstallTime)
    )

    val selectedIndex = map.map { it.type }.indexOf(uiState.orderType)

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
private fun ChipsWidget(
    uiState: ApplyViewState,
    viewModel: ApplyViewModel
) {
    LabelWidget(stringResource(R.string.more), 0.dp)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val orderInReverse = uiState.orderInReverse
        val selectedFirst = uiState.selectedFirst
        val showSystemApp = uiState.showSystemApp
        val showPackageName = uiState.showPackageName
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
