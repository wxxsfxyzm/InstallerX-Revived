// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CheckableDropdownMenuItem
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.main.widget.menu.GroupedDropdownMenuPopup
import com.rosan.installer.ui.page.main.widget.setting.ExpressiveBackButton
import com.rosan.installer.ui.page.main.widget.setting.LabelWidget
import com.rosan.installer.ui.theme.bottomShape
import com.rosan.installer.ui.theme.getMaterial3AppBarColor
import com.rosan.installer.ui.theme.installerMaterial3BlurEffect
import com.rosan.installer.ui.theme.middleShape
import com.rosan.installer.ui.theme.rememberMaterial3BlurBackdrop
import com.rosan.installer.ui.theme.singleShape
import com.rosan.installer.ui.theme.topShape
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import top.yukonga.miuix.kmp.blur.layerBackdrop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyPage(id: Long, useBlur: Boolean, viewModel: ApplyViewModel = koinViewModel { parametersOf(id) }) {
    val navigator = LocalNavigator.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showOptionsMenu by remember { mutableStateOf(false) }
    val showFloating by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0
        }
    }

    val layoutDirection = LocalLayoutDirection.current

    val backdrop = rememberMaterial3BlurBackdrop(useBlur)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            var searchBarActivated by remember { mutableStateOf(false) }
            TopAppBar(
                modifier = Modifier.installerMaterial3BlurEffect(backdrop),
                windowInsets = TopAppBarDefaults.windowInsets.add(WindowInsets(left = 12.dp)),
                scrollBehavior = scrollBehavior,
                title = {
                    AnimatedContent(targetState = searchBarActivated) {
                        if (!it) {
                            Text(stringResource(R.string.config_scope))
                        } else {
                            val focusRequester = remember { FocusRequester() }
                            OutlinedTextField(
                                modifier = Modifier.focusRequester(focusRequester),
                                value = uiState.search,
                                onValueChange = { viewModel.dispatch(ApplyViewAction.Search(it)) },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = AppIcons.Search,
                                        contentDescription = stringResource(R.string.search),
                                    )
                                },
                                trailingIcon = {
                                    IconButton(
                                        shapes = IconButtonShapes(
                                            shape = IconButtonDefaults.smallRoundShape,
                                            pressedShape = IconButtonDefaults.smallPressedShape,
                                        ),
                                        onClick = {
                                            searchBarActivated = false
                                            viewModel.dispatch(ApplyViewAction.Search(""))
                                        },
                                    ) {
                                        Icon(
                                            imageVector = AppIcons.Close,
                                            contentDescription = stringResource(R.string.close),
                                        )
                                    }
                                },
                                textStyle = MaterialTheme.typography.titleMedium,
                            )
                            SideEffect {
                                focusRequester.requestFocus()
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backdrop.getMaterial3AppBarColor(),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    scrolledContainerColor = backdrop.getMaterial3AppBarColor(),
                ),
                navigationIcon = {
                    Row {
                        ExpressiveBackButton { navigator.pop() }
                        Spacer(modifier = Modifier.size(16.dp))
                    }
                },
                actions = {
                    AnimatedVisibility(visible = !searchBarActivated) {
                        IconButton(onClick = { searchBarActivated = !searchBarActivated }) {
                            Icon(
                                imageVector = AppIcons.Search,
                                contentDescription = stringResource(R.string.search),
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(
                                imageVector = AppIcons.Menu,
                                contentDescription = stringResource(R.string.menu),
                            )
                        }
                        ApplyOptionsDropdown(
                            expanded = showOptionsMenu,
                            uiState = uiState,
                            onDismissRequest = { showOptionsMenu = false },
                            onAction = viewModel::dispatch,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showFloating,
                enter = scaleIn(),
                exit = scaleOut(),
                modifier = Modifier.padding(
                    bottom = 16.dp,
                ),
            ) {
                FloatingActionButton({
                    coroutineScope.launch {
                        lazyListState.animateScrollToItem(0)
                    }
                }) {
                    Icon(imageVector = AppIcons.ArrowUp, contentDescription = null)
                }
            }
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.apps.progress is ViewContent.Progress.Loading && uiState.apps.data.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            ContainedLoadingIndicator()
                            Text(
                                text = stringResource(id = R.string.loading),
                                style = MaterialTheme.typography.titleLarge,
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
                            PullToRefreshDefaults.LoadingIndicator(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = paddingValues.calculateTopPadding()),
                                state = pullToRefreshState,
                                isRefreshing = refreshing,
                                color = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            )
                        },
                    ) {
                        ItemsWidget(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
                            uiState = uiState,
                            viewModel = viewModel,
                            lazyListState = lazyListState,
                            topPadding = paddingValues.calculateTopPadding(),
                            bottomPadding = paddingValues.calculateBottomPadding(),
                            startPadding = paddingValues.calculateStartPadding(layoutDirection),
                            endPadding = paddingValues.calculateEndPadding(layoutDirection),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemsWidget(
    modifier: Modifier = Modifier,
    uiState: ApplyViewState,
    viewModel: ApplyViewModel,
    lazyListState: LazyListState,
    topPadding: Dp = 0.dp,
    bottomPadding: Dp = 0.dp,
    startPadding: Dp = 0.dp,
    endPadding: Dp = 0.dp,
) {
    val unknownLabel = stringResource(R.string.config_scope_unknown)
    val unknownGroupLabel = stringResource(R.string.config_scope_unknown_group)
    val appsGroupLabel = stringResource(R.string.config_scope_apps)
    val appliedPackageSet by remember(uiState.appEntities.data) {
        derivedStateOf {
            uiState.appEntities.data.map { it.packageName }.toHashSet()
        }
    }

    LazyColumn(
        modifier = modifier,
        state = lazyListState,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        contentPadding = PaddingValues(
            start = startPadding + 16.dp,
            top = topPadding + 8.dp,
            end = endPadding + 16.dp,
            bottom = bottomPadding + 96.dp,
        ),
    ) {
        val apps = uiState.checkedApps
        val showApps = apps.isNotEmpty()
        val showUnknown = uiState.showUnknownScope &&
            (uiState.search.isBlank() || unknownLabel.contains(uiState.search, ignoreCase = true))

        if (showUnknown) {
            item(
                key = "unknown_group_label",
                contentType = "scope_item",
            ) {
                LabelWidget(
                    unknownGroupLabel,
                    horizontalPadding = 16.dp,
                    topPadding = 0.dp,
                )
            }
            item(
                key = "unknown_scope",
                contentType = "scope_item",
            ) {
                val isApplied = appliedPackageSet.contains(null)
                UnknownScopeItemWidget(
                    modifier = Modifier.animateItem(
                        placementSpec = spring(
                            stiffness = Spring.StiffnessMediumLow,
                            visibilityThreshold = IntOffset.VisibilityThreshold,
                        ),
                    ),
                    title = unknownLabel,
                    shape = singleShape,
                    isApplied = isApplied,
                    onToggle = { isChecked ->
                        viewModel.dispatch(ApplyViewAction.ApplyPackageName(null, isChecked))
                    },
                    onClick = {
                        viewModel.dispatch(ApplyViewAction.ApplyPackageName(null, !isApplied))
                    },
                )
            }
        }

        if (showApps) {
            item(
                key = "apps_group_label",
                contentType = "scope_item",
            ) {
                LabelWidget(
                    appsGroupLabel,
                    horizontalPadding = 16.dp,
                    topPadding = if (showUnknown) 8.dp else 0.dp,
                )
            }
        }

        itemsIndexed(
            items = apps,
            key = { _, app -> app.packageName },
            contentType = { _, _ -> "app_item" },
        ) { index, app ->
            val shape = when {
                apps.size == 1 -> singleShape
                index == 0 -> topShape
                index == apps.lastIndex -> bottomShape
                else -> middleShape
            }

            val isApplied = appliedPackageSet.contains(app.packageName)

            // Dispatch action to load the icon dynamically when the item becomes visible
            LaunchedEffect(app.packageName) {
                viewModel.dispatch(ApplyViewAction.LoadIcon(app.packageName))
            }

            // Retrieve the dynamically loaded icon from the managed state
            val iconBitmap = uiState.displayIcons[app.packageName]

            ApplyItemWidget(
                modifier = Modifier.animateItem(
                    placementSpec = spring(
                        stiffness = Spring.StiffnessMediumLow,
                        visibilityThreshold = IntOffset.VisibilityThreshold,
                    ),
                ),
                app = app,
                icon = iconBitmap,
                isApplied = isApplied,
                shape = shape,
                containerColor = MaterialTheme.colorScheme.surfaceBright,
                showPackageName = uiState.showPackageName,
                onToggle = { isChecked ->
                    viewModel.dispatch(ApplyViewAction.ApplyPackageName(app.packageName, isChecked))
                },
                onClick = {
                    viewModel.dispatch(
                        ApplyViewAction.ApplyPackageName(
                            app.packageName,
                            !isApplied,
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun ApplyOptionsDropdown(
    expanded: Boolean,
    uiState: ApplyViewState,
    onDismissRequest: () -> Unit,
    onAction: (ApplyViewAction) -> Unit,
) {
    data class OrderOption(
        val labelResId: Int,
        val type: ApplyViewState.OrderType,
    )

    val orderOptions = listOf(
        OrderOption(R.string.sort_by_label, ApplyViewState.OrderType.Label),
        OrderOption(R.string.sort_by_package_name, ApplyViewState.OrderType.PackageName),
        OrderOption(R.string.sort_by_install_time, ApplyViewState.OrderType.FirstInstallTime),
    )
    val toggleLabels = listOf(
        R.string.sort_by_reverse_order,
        R.string.sort_by_selected_first,
        R.string.sort_by_show_system_app,
        R.string.sort_by_show_package_name,
        R.string.sort_by_show_unknown_scope,
    )

    GroupedDropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        groupSizes = listOf(orderOptions.size, toggleLabels.size - 1, 1),
        keepOpenOnItemClick = true,
    ) { groupIndex, itemIndex, shape, dismissItem ->
        val checked = if (groupIndex == 0) {
            uiState.orderType == orderOptions[itemIndex].type
        } else if (groupIndex == 2) {
            uiState.showUnknownScope
        } else {
            when (itemIndex) {
                0 -> uiState.orderInReverse
                1 -> uiState.selectedFirst
                2 -> uiState.showSystemApp
                else -> uiState.showPackageName
            }
        }
        val labelResId = if (groupIndex == 0) {
            orderOptions[itemIndex].labelResId
        } else if (groupIndex == 2) {
            toggleLabels.last()
        } else {
            toggleLabels[itemIndex]
        }

        CheckableDropdownMenuItem(
            checked = checked,
            onCheckedChange = { enabled ->
                if (groupIndex == 0) {
                    onDismissRequest()
                    onAction(ApplyViewAction.Order(orderOptions[itemIndex].type))
                } else if (groupIndex == 2) {
                    dismissItem()
                    onAction(ApplyViewAction.ShowUnknownScope(enabled))
                } else {
                    dismissItem()
                    onAction(
                        when (itemIndex) {
                            0 -> ApplyViewAction.OrderInReverse(enabled)
                            1 -> ApplyViewAction.SelectedFirst(enabled)
                            2 -> ApplyViewAction.ShowSystemApp(enabled)
                            else -> ApplyViewAction.ShowPackageName(enabled)
                        },
                    )
                }
            },
            text = { Text(stringResource(labelResId)) },
            trailingContent = if (checked) {
                {
                    Icon(imageVector = AppIcons.Check, contentDescription = null)
                }
            } else {
                null
            },
            horizontalArrangement = Arrangement.SpaceBetween,
            shapes = shape,
        )
    }
}
