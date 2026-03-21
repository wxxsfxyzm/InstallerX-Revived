// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.settings.config.apply

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.config.apply.ApplyViewAction
import com.rosan.installer.ui.page.main.settings.config.apply.ApplyViewApp
import com.rosan.installer.ui.page.main.settings.config.apply.ApplyViewModel
import com.rosan.installer.ui.page.main.settings.config.apply.ApplyViewState
import com.rosan.installer.ui.page.main.settings.config.apply.ViewContent
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import com.rosan.installer.ui.page.miuix.widgets.MiuixDropdown
import com.rosan.installer.ui.theme.getMiuixAppBarColor
import com.rosan.installer.ui.theme.installerHazeEffect
import com.rosan.installer.ui.theme.rememberMiuixHazeStyle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.extra.WindowListPopup
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.More
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
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val scrollBehavior = MiuixScrollBehavior()

    val hazeState = if (uiState.useBlur) remember { HazeState() } else null
    val hazeStyle = rememberMiuixHazeStyle()

    val showFloating by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .installerHazeEffect(hazeState, hazeStyle)
                    .background(hazeState.getMiuixAppBarColor())
            ) {
                TopAppBar(
                    color = Color.Transparent,
                    scrollBehavior = scrollBehavior,
                    title = stringResource(R.string.config_scope),
                    navigationIcon = {
                        MiuixBackButton(
                            modifier = Modifier.padding(start = 16.dp),
                            icon = MiuixIcons.Regular.Close,
                            onClick = { navController.navigateUp() })
                    },
                    actions = { TopAppBarActions(viewModel = viewModel, uiState = uiState) }
                )
                Spacer(modifier = Modifier.size(6.dp))
                InputField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    query = uiState.search,
                    onQueryChange = { viewModel.dispatch(ApplyViewAction.Search(it)) },
                    label = stringResource(R.string.search),
                    expanded = false,
                    onExpandedChange = {},
                    onSearch = {}
                )

                data class OrderData(val labelResId: Int, val type: ApplyViewState.OrderType)

                val orderOptions = remember {
                    listOf(
                        OrderData(R.string.sort_by_label, ApplyViewState.OrderType.Label),
                        OrderData(R.string.sort_by_package_name, ApplyViewState.OrderType.PackageName),
                        OrderData(R.string.sort_by_install_time, ApplyViewState.OrderType.FirstInstallTime)
                    )
                }

                val dropdownItems = orderOptions.map { stringResource(it.labelResId) }
                val selectedIndex = orderOptions.indexOfFirst { it.type == uiState.orderType }.coerceAtLeast(0)

                Box(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .padding(bottom = 6.dp)
                ) {
                    MiuixDropdown(
                        items = dropdownItems,
                        selectedIndex = selectedIndex,
                        onSelectedIndexChange = { newIndex ->
                            val newOrderType = orderOptions[newIndex].type
                            viewModel.dispatch(ApplyViewAction.Order(newOrderType))
                        }
                    )
                }
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
                            InfiniteProgressIndicator()
                            Text(
                                text = stringResource(id = R.string.loading),
                                style = MiuixTheme.textStyles.main
                            )
                        }
                    }
                }

                else -> {
                    val refreshing = uiState.apps.progress is ViewContent.Progress.Loading

                    val appliedPackageSet by remember(uiState.appEntities.data) {
                        derivedStateOf {
                            uiState.appEntities.data.map { it.packageName }.toHashSet()
                        }
                    }

                    PullToRefresh(
                        isRefreshing = refreshing,
                        onRefresh = { viewModel.dispatch(ApplyViewAction.LoadApps) },
                        modifier = Modifier.fillMaxSize(),
                        topAppBarScrollBehavior = scrollBehavior,
                        contentPadding = paddingValues,
                        refreshTexts = listOf(
                            stringResource(R.string.pull_to_refresh_hint1),
                            stringResource(R.string.pull_to_refresh_hint2),
                            stringResource(R.string.pull_to_refresh_hint3),
                            stringResource(R.string.pull_to_refresh_hint4)
                        ),
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(hazeState?.let { Modifier.hazeSource(it) } ?: Modifier)
                                .scrollEndHaptic()
                                .overScrollVertical()
                                .nestedScroll(scrollBehavior.nestedScrollConnection),
                            state = lazyListState,
                            contentPadding = PaddingValues(
                                top = paddingValues.calculateTopPadding() + 8.dp,
                                bottom = paddingValues.calculateBottomPadding()
                            ),
                            overscrollEffect = null
                        ) {
                            val apps = uiState.checkedApps
                            itemsIndexed(
                                items = apps,
                                key = { _, app -> app.packageName },
                                contentType = { _, _ -> "app_item" }
                            ) { index, app ->
                                val cardRadius = CardDefaults.CornerRadius
                                val shape = when {
                                    apps.size == 1 -> RoundedCornerShape(cardRadius)
                                    index == 0 -> RoundedCornerShape(
                                        topStart = cardRadius,
                                        topEnd = cardRadius,
                                        bottomStart = 0.dp,
                                        bottomEnd = 0.dp
                                    )

                                    index == apps.lastIndex -> RoundedCornerShape(
                                        topStart = 0.dp,
                                        topEnd = 0.dp,
                                        bottomStart = cardRadius,
                                        bottomEnd = cardRadius
                                    )

                                    else -> RoundedCornerShape(0.dp)
                                }

                                val isApplied = appliedPackageSet.contains(app.packageName)
                                // Dispatch action to load the icon when the item becomes visible
                                LaunchedEffect(app.packageName) {
                                    viewModel.dispatch(ApplyViewAction.LoadIcon(app.packageName))
                                }

                                // Retrieve the dynamically loaded icon from the state
                                val iconBitmap = uiState.displayIcons[app.packageName]

                                MiuixItemWidget(
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp)
                                        .zIndex(-index.toFloat())
                                        .animateItem(
                                            fadeInSpec = null,
                                            fadeOutSpec = null,
                                            placementSpec = spring(
                                                stiffness = Spring.StiffnessMediumLow,
                                                visibilityThreshold = IntOffset.VisibilityThreshold
                                            )
                                        ),
                                    app = app,
                                    icon = iconBitmap, // Pass the managed state
                                    isApplied = isApplied,
                                    shape = shape,
                                    onToggle = { isChecked ->
                                        viewModel.dispatch(ApplyViewAction.ApplyPackageName(app.packageName, isChecked))
                                    },
                                    onClick = {
                                        viewModel.dispatch(ApplyViewAction.ApplyPackageName(app.packageName, !isApplied))
                                    },
                                    showPackageName = uiState.showPackageName
                                )
                            }
                            item { Spacer(modifier = Modifier.navigationBarsPadding()) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiuixItemWidget(
    modifier: Modifier = Modifier,
    app: ApplyViewApp,
    icon: ImageBitmap?,
    isApplied: Boolean,
    shape: Shape,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    showPackageName: Boolean
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardDefaults.defaultColors().color)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onClick,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = MiuixTheme.colorScheme.primary)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // The redundant side-effect logic and Context usage have been completely removed.

            if (icon != null) {
                Image(
                    bitmap = icon,
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.CenterVertically),
                    contentDescription = null
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.CenterVertically)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(1f)
            ) {
                Text(
                    text = app.label ?: app.packageName,
                    style = MiuixTheme.textStyles.title4
                )
                AnimatedVisibility(showPackageName) {
                    Text(
                        text = app.packageName,
                        style = MiuixTheme.textStyles.subtitle,
                        color = MiuixTheme.colorScheme.onSurfaceVariantActions
                    )
                }
            }
            Switch(
                modifier = Modifier.align(Alignment.CenterVertically),
                checked = isApplied,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
private fun TopAppBarActions(viewModel: ApplyViewModel, uiState: ApplyViewState) {
    val showMenu = remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current

    val menuOptions = remember(
        uiState.orderInReverse,
        uiState.selectedFirst,
        uiState.showSystemApp,
        uiState.showPackageName
    ) {
        listOf(
            R.string.sort_by_reverse_order to uiState.orderInReverse,
            R.string.sort_by_selected_first to uiState.selectedFirst,
            R.string.sort_by_show_system_app to uiState.showSystemApp,
            R.string.sort_by_show_package_name to uiState.showPackageName
        )
    }

    WindowListPopup(
        show = showMenu.value,
        popupPositionProvider = ListPopupDefaults.ContextMenuPositionProvider,
        alignment = PopupPositionProvider.Align.TopEnd,
        onDismissRequest = {
            showMenu.value = false
        }
    ) {
        ListPopupColumn {
            menuOptions.forEachIndexed { index, (labelResId, isSelected) ->
                DropdownImpl(
                    text = stringResource(labelResId),
                    optionSize = menuOptions.size,
                    isSelected = isSelected,
                    onSelectedIndexChange = { selectedIndex ->
                        when (selectedIndex) {
                            0 -> viewModel.dispatch(ApplyViewAction.OrderInReverse(!uiState.orderInReverse))
                            1 -> viewModel.dispatch(ApplyViewAction.SelectedFirst(!uiState.selectedFirst))
                            2 -> viewModel.dispatch(ApplyViewAction.ShowSystemApp(!uiState.showSystemApp))
                            3 -> viewModel.dispatch(ApplyViewAction.ShowPackageName(!uiState.showPackageName))
                        }
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                    },
                    index = index
                )
            }
        }
    }

    IconButton(
        modifier = Modifier.padding(end = 12.dp),
        onClick = {
            showMenu.value = true
        },
        holdDownState = showMenu.value
    ) {
        Icon(
            imageVector = MiuixIcons.Regular.More,
            tint = MiuixTheme.colorScheme.onBackground,
            contentDescription = "More Options"
        )
    }
}
