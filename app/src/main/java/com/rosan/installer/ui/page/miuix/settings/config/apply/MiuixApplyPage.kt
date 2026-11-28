// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.settings.config.apply

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.ui.feature.settings.config.apply.ApplyViewAction
import com.rosan.installer.ui.feature.settings.config.apply.ApplyViewApp
import com.rosan.installer.ui.feature.settings.config.apply.ApplyViewModel
import com.rosan.installer.ui.feature.settings.config.apply.ApplyViewState
import com.rosan.installer.ui.feature.settings.config.apply.ViewContent
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.miuix.theme.getMiuixAppBarColor
import com.rosan.installer.ui.miuix.theme.installerMiuixBlurEffect
import com.rosan.installer.ui.miuix.theme.rememberMiuixBlurBackdrop
import com.rosan.installer.ui.miuix.widgets.MiuixDropdown
import com.rosan.installer.ui.miuix.widgets.rememberMiuixPopupBackdropActive
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.miuix.widgets.AppMiuixBackButton
import com.rosan.installer.ui.page.miuix.widgets.InstallerMiuixBackButton
import com.rosan.installer.ui.page.miuix.widgets.InstallerMiuixTopAppBar
import com.rosan.installer.ui.page.miuix.widgets.InstallerMiuixTopBarIconButton
import com.rosan.installer.ui.page.miuix.widgets.installerMiuixTopBarScroll
import com.rosan.installer.ui.theme.InstallerTheme
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SearchClearButton
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.glass.GlassDropdownPopup
import top.yukonga.miuix.kmp.glass.GlassFloatingActionButton
import top.yukonga.miuix.kmp.glass.GlassIconDropdownMenu
import top.yukonga.miuix.kmp.glass.GlassPopupAnchor
import top.yukonga.miuix.kmp.glass.GlassPopupDefaults
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.glass.GlassSearchOverlayDefaults
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.glass.glass
import top.yukonga.miuix.kmp.glass.glassPopupAnchor
import top.yukonga.miuix.kmp.glass.glassPopupAnchorContent
import top.yukonga.miuix.kmp.glass.glassPopupAnchorValue
import top.yukonga.miuix.kmp.glass.rememberGlassPopupAnchor
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowUpDown
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.glass.Close
import top.yukonga.miuix.kmp.icon.glass.More
import top.yukonga.miuix.kmp.icon.glass.Search
import top.yukonga.miuix.kmp.material.GlassDefaults
import top.yukonga.miuix.kmp.material.GlassShape
import top.yukonga.miuix.kmp.material.drawGlassShadow
import top.yukonga.miuix.kmp.menu.WindowIconDropdownMenu
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

private val SearchListTopGap = 8.dp

private data class OrderOption(val labelResId: Int, val type: ApplyViewState.OrderType)

private val orderOptions = listOf(
    OrderOption(R.string.sort_by_label, ApplyViewState.OrderType.Label),
    OrderOption(R.string.sort_by_package_name, ApplyViewState.OrderType.PackageName),
    OrderOption(R.string.sort_by_install_time, ApplyViewState.OrderType.FirstInstallTime),
)

@Composable
fun MiuixApplyPage(id: Long, useBlur: Boolean, viewModel: ApplyViewModel = koinViewModel { parametersOf(id) }) {
    val navigator = LocalNavigator.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val scrollBehavior = MiuixScrollBehavior()
    val hapticFeedback = LocalHapticFeedback.current
    val useOs4 = InstallerTheme.useMiuixOs4

    val showFloating by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0
        }
    }
    val isContentScrolled by remember(lazyListState, scrollBehavior) {
        derivedStateOf { lazyListState.canScrollBackward || scrollBehavior.state.overscrollOffset < -1f }
    }
    // The list starts SearchListTopGap below the bar, so the search pill is only overlapped past that gap.
    val searchGapPx = with(LocalDensity.current) { SearchListTopGap.toPx() }
    val searchHasOverlap by remember(searchGapPx) {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > searchGapPx
        }
    }

    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    val topBarBackdrop = rememberMiuixBlurBackdrop(useBlur)
    // OS4 popups sample the whole page, bars included, so they are hosted outside the Scaffold.
    val menuBackdrop = rememberMiuixBlurBackdrop(useOs4)
    val moreMenuAnchor = rememberGlassPopupAnchor()
    var showMoreMenu by remember { mutableStateOf(false) }
    val orderMenuAnchor = rememberGlassPopupAnchor()
    var orderSelectorBounds by remember { mutableStateOf(Rect.Zero) }
    var showOrderMenu by remember { mutableStateOf(false) }
    val menuBackdropActive = rememberMiuixPopupBackdropActive(showMoreMenu || showOrderMenu)

    val orderItems = orderOptions.map { stringResource(it.labelResId) }
    val selectedOrderIndex = orderOptions.indexOfFirst { it.type == uiState.orderType }.coerceAtLeast(0)
    val onOrderIndexChange: (Int) -> Unit = { newIndex ->
        viewModel.dispatch(ApplyViewAction.Order(orderOptions[newIndex].type))
    }
    val menuEntries = rememberApplyMenuEntries(viewModel = viewModel, uiState = uiState)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .then(if (menuBackdrop != null && menuBackdropActive) Modifier.layerBackdrop(menuBackdrop) else Modifier),
            topBar = {
                if (useOs4) {
                    InstallerMiuixTopAppBar(
                        useOs4 = true,
                        backdrop = topBarBackdrop,
                        isContentScrolled = isContentScrolled,
                        scrollBehavior = scrollBehavior,
                        title = stringResource(R.string.config_scope),
                        navigationIcon = {
                            InstallerMiuixBackButton(
                                icon = MiuixIcons.Glass.Close,
                                onClick = { navigator.pop() },
                            )
                        },
                        actions = {
                            InstallerMiuixTopBarIconButton(
                                onClick = { showMoreMenu = !showMoreMenu },
                                modifier = Modifier.glassPopupAnchor(
                                    anchor = moreMenuAnchor,
                                    cornerRadius = GlassTopAppBarDefaults.ButtonSize / 2,
                                    floating = isContentScrolled,
                                ),
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Glass.More,
                                    contentDescription = stringResource(R.string.menu),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .glassPopupAnchorContent(moreMenuAnchor),
                                    tint = MiuixTheme.colorScheme.onSurface,
                                )
                            }
                        },
                        bottomContent = {
                            GlassSearchBar(
                                query = uiState.search,
                                onQueryChange = { viewModel.dispatch(ApplyViewAction.Search(it)) },
                                label = stringResource(R.string.search),
                                backdrop = topBarBackdrop,
                                hasOverlap = searchHasOverlap,
                                modifier = Modifier
                                    .padding(
                                        start = GlassSearchOverlayDefaults.HorizontalPadding +
                                            horizontalSafeInsets.calculateStartPadding(layoutDirection),
                                        end = GlassSearchOverlayDefaults.HorizontalPadding +
                                            horizontalSafeInsets.calculateEndPadding(layoutDirection),
                                    )
                                    .padding(top = 6.dp, bottom = 8.dp),
                            )
                        },
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .installerMiuixBlurEffect(topBarBackdrop)
                            .background(topBarBackdrop.getMiuixAppBarColor())
                            .pointerInput(Unit) { detectTapGestures(onTap = {}) },
                    ) {
                        TopAppBar(
                            color = Color.Transparent,
                            scrollBehavior = scrollBehavior,
                            title = stringResource(R.string.config_scope),
                            navigationIcon = {
                                AppMiuixBackButton(
                                    icon = MiuixIcons.Regular.Close,
                                    onClick = { navigator.pop() },
                                )
                            },
                            actions = { TopAppBarActions(menuEntries = menuEntries) },
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        InputField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 16.dp + horizontalSafeInsets.calculateStartPadding(
                                        layoutDirection,
                                    ),
                                    end = 16.dp + horizontalSafeInsets.calculateEndPadding(layoutDirection),
                                )
                                .padding(bottom = 8.dp),
                            query = uiState.search,
                            onQueryChange = { viewModel.dispatch(ApplyViewAction.Search(it)) },
                            label = stringResource(R.string.search),
                            expanded = false,
                            onExpandedChange = {},
                            onSearch = {},
                        )
                        Box(
                            modifier = Modifier
                                .padding(
                                    start = 6.dp + horizontalSafeInsets.calculateStartPadding(
                                        layoutDirection,
                                    ),
                                    end = 6.dp + horizontalSafeInsets.calculateEndPadding(layoutDirection),
                                )
                                .padding(bottom = 6.dp),
                        ) {
                            MiuixDropdown(
                                items = orderItems,
                                selectedIndex = selectedOrderIndex,
                                onSelectedIndexChange = onOrderIndexChange,
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = showFloating,
                    enter = scaleIn(),
                    exit = scaleOut(),
                    modifier = Modifier.padding(bottom = 16.dp),
                ) {
                    val scrollToTop: () -> Unit = {
                        coroutineScope.launch {
                            lazyListState.animateScrollToItem(0)
                            if (useOs4) {
                                scrollBehavior.state.heightOffset = 0f
                                scrollBehavior.state.contentOffset = 0f
                            }
                        }
                    }
                    if (useOs4) {
                        GlassFloatingActionButton(
                            modifier = Modifier.padding(end = 16.dp),
                            onClick = scrollToTop,
                            backdrop = topBarBackdrop,
                            style = GlassDefaults.style(InstallerTheme.isDark),
                        ) {
                            Icon(
                                imageVector = AppIcons.ArrowUp,
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.onPrimary,
                            )
                        }
                    } else {
                        FloatingActionButton(
                            modifier = Modifier.padding(end = 16.dp),
                            containerColor = MiuixTheme.colorScheme.surface,
                            onClick = scrollToTop,
                        ) {
                            Icon(
                                imageVector = AppIcons.ArrowUp,
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.primary,
                            )
                        }
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
                                InfiniteProgressIndicator()
                                Text(
                                    text = stringResource(id = R.string.loading),
                                    style = MiuixTheme.textStyles.main,
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
                        val unknownLabel = stringResource(R.string.config_scope_unknown)

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
                                stringResource(R.string.pull_to_refresh_hint4),
                            ),
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(topBarBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)
                                    .scrollEndHaptic()
                                    .installerMiuixTopBarScroll(scrollBehavior),
                                state = lazyListState,
                                contentPadding = PaddingValues(
                                    start = horizontalSafeInsets.calculateStartPadding(layoutDirection),
                                    top = paddingValues.calculateTopPadding() + SearchListTopGap,
                                    end = horizontalSafeInsets.calculateEndPadding(layoutDirection),
                                    bottom = paddingValues.calculateBottomPadding(),
                                ),
                                overscrollEffect = null,
                            ) {
                                val apps = uiState.checkedApps
                                val showUnknown = uiState.showUnknownScope &&
                                    (uiState.search.isBlank() || unknownLabel.contains(uiState.search, ignoreCase = true))

                                // OS4 moves the order selector out of the pinned bar so it scrolls with the list.
                                if (useOs4) {
                                    item(
                                        key = "order_selector",
                                        contentType = "order_selector",
                                    ) {
                                        GlassOrderSelector(
                                            text = orderItems[selectedOrderIndex],
                                            anchor = orderMenuAnchor,
                                            onClick = {
                                                if (!showOrderMenu) {
                                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                                                }
                                                showOrderMenu = !showOrderMenu
                                            },
                                            modifier = Modifier
                                                .padding(horizontal = 6.dp)
                                                .padding(bottom = 6.dp),
                                            onBoundsChange = { orderSelectorBounds = it },
                                        )
                                    }
                                }

                                if (showUnknown) {
                                    item(
                                        key = "unknown_scope",
                                        contentType = "scope_item",
                                    ) {
                                        val isApplied = appliedPackageSet.contains(null)
                                        UnknownScopeItemWidget(
                                            modifier = Modifier.padding(horizontal = 12.dp),
                                            title = unknownLabel,
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

                                if (showUnknown && apps.isNotEmpty()) {
                                    item(
                                        key = "unknown_apps_group_spacing",
                                        contentType = "scope_group_spacing",
                                    ) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }

                                itemsIndexed(
                                    items = apps,
                                    key = { _, app -> app.packageName },
                                    contentType = { _, _ -> "app_item" },
                                ) { index, app ->
                                    val cardRadius = CardDefaults.CornerRadius
                                    val shape = when {
                                        apps.size == 1 -> RoundedCornerShape(cardRadius)

                                        index == 0 -> RoundedCornerShape(
                                            topStart = cardRadius,
                                            topEnd = cardRadius,
                                            bottomStart = 0.dp,
                                            bottomEnd = 0.dp,
                                        )

                                        index == apps.lastIndex -> RoundedCornerShape(
                                            topStart = 0.dp,
                                            topEnd = 0.dp,
                                            bottomStart = cardRadius,
                                            bottomEnd = cardRadius,
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

                                    ItemWidget(
                                        modifier = Modifier
                                            .padding(horizontal = 12.dp)
                                            .zIndex(-index.toFloat())
                                            .animateItem(
                                                fadeInSpec = null,
                                                fadeOutSpec = null,
                                                placementSpec = spring(
                                                    stiffness = Spring.StiffnessMediumLow,
                                                    visibilityThreshold = IntOffset.VisibilityThreshold,
                                                ),
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
                                        showPackageName = uiState.showPackageName,
                                    )
                                }
                                item { Spacer(modifier = Modifier.navigationBarsPadding()) }
                            }
                        }
                    }
                }
            }
        }
        if (useOs4) {
            val glassVisuals = GlassPopupDefaults.visuals(style = GlassDefaults.style(InstallerTheme.isDark))
            GlassDropdownPopup(
                show = showOrderMenu,
                onDismissRequest = { showOrderMenu = false },
                anchorBounds = orderSelectorBounds,
                backdrop = menuBackdrop,
                anchor = orderMenuAnchor,
                visuals = glassVisuals,
            ) {
                orderItems.forEachIndexed { index, text ->
                    GlassPopupItem(
                        text = text,
                        selected = index == selectedOrderIndex,
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                            showOrderMenu = false
                            onOrderIndexChange(index)
                        },
                    )
                }
            }
            GlassIconDropdownMenu(
                show = showMoreMenu,
                onDismissRequest = { showMoreMenu = false },
                anchor = moreMenuAnchor,
                anchorContent = {
                    Icon(
                        imageVector = MiuixIcons.Glass.More,
                        contentDescription = stringResource(R.string.menu),
                        modifier = Modifier.size(24.dp),
                        tint = MiuixTheme.colorScheme.onSurface,
                    )
                },
                entries = menuEntries,
                backdrop = menuBackdrop,
                visuals = glassVisuals,
                collapseOnSelection = false,
            )
        }
    }
}

/**
 * OS4 inline search pill. Mirrors the [top.yukonga.miuix.kmp.glass.GlassSearchOverlay] pill, including its
 * solid-to-glass transition once list content scrolls under it, but filters the page list in place.
 */
@Composable
private fun GlassSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    label: String,
    backdrop: LayerBackdrop?,
    hasOverlap: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    val mutedColor = if (MiuixTheme.isDynamicColor) colors.onSurfaceVariantSummary else colors.disabledOnSurface
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val shape = remember { GlassShape(GlassSearchOverlayDefaults.BarHeight / 2) }
    val style = GlassDefaults.style(InstallerTheme.isDark)
    val material = GlassSearchOverlayDefaults.material()
    val stroke = GlassSearchOverlayDefaults.stroke()
    val shadow = GlassSearchOverlayDefaults.shadow()
    // Same ramp as the overlay's scrollMaterialThreshold mode; without a backdrop the pill stays solid.
    val materialProgress by animateFloatAsState(
        targetValue = if (backdrop != null && hasOverlap) 1f else 0f,
        animationSpec = tween(450, easing = LinearEasing),
        label = "applySearchPillMaterial",
    )

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(GlassSearchOverlayDefaults.BarHeight)
            .focusRequester(focusRequester),
        singleLine = true,
        textStyle = MiuixTheme.textStyles.body1.copy(color = colors.onSurface),
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
        decorationBox = { field ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind { drawGlassShadow(shape, shadow, materialProgress) }
                    .background(colors.surfaceContainerHigh, CircleShape),
            ) {
                if (backdrop != null && materialProgress > 0.001f) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer { alpha = materialProgress }
                            .glass(
                                backdrop = backdrop,
                                shape = shape,
                                style = style,
                                material = material,
                                stroke = stroke,
                                shading = false,
                            ),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = MiuixIcons.Glass.Search,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = mutedColor,
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(text = label, color = mutedColor, style = MiuixTheme.textStyles.body1)
                        }
                        field()
                    }
                    if (query.isNotEmpty()) {
                        SearchClearButton(
                            onClick = {
                                onQueryChange("")
                                focusRequester.requestFocus()
                            },
                            modifier = Modifier.size(24.dp),
                            contentDescription = stringResource(R.string.clear),
                        )
                    }
                }
            }
        },
    )
}

/** OS4 order selector; keeps the OS3 [MiuixDropdown] look but anchors a glass dropdown to its value. */
@Composable
private fun GlassOrderSelector(
    text: String,
    anchor: GlassPopupAnchor,
    onClick: () -> Unit,
    onBoundsChange: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
                .padding(horizontal = 28.dp, vertical = 8.dp)
                .onGloballyPositioned { onBoundsChange(it.boundsInWindow()) }
                .glassPopupAnchorValue(anchor),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = text,
                fontSize = MiuixTheme.textStyles.subtitle.fontSize,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
            )
            Image(
                modifier = Modifier.size(10.dp, 16.dp),
                imageVector = MiuixIcons.Basic.ArrowUpDown,
                colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurfaceVariantActions),
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun ItemWidget(
    modifier: Modifier = Modifier,
    app: ApplyViewApp,
    icon: ImageBitmap?,
    isApplied: Boolean,
    shape: Shape,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    showPackageName: Boolean,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardDefaults.defaultColors().color),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onClick,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = MiuixTheme.colorScheme.primary),
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
                    contentDescription = null,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.CenterVertically),
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(1f),
            ) {
                Text(
                    text = app.label ?: app.packageName,
                    style = MiuixTheme.textStyles.title4,
                )
                AnimatedVisibility(showPackageName) {
                    Text(
                        text = app.packageName,
                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                        color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                    )
                }
            }
            Switch(
                modifier = Modifier.align(Alignment.CenterVertically),
                checked = isApplied,
                onCheckedChange = onToggle,
            )
        }
    }
}

@Composable
private fun UnknownScopeItemWidget(
    modifier: Modifier = Modifier,
    title: String,
    isApplied: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardDefaults.CornerRadius))
            .background(CardDefaults.defaultColors().color),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onClick,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = MiuixTheme.colorScheme.primary),
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = AppIcons.InstallSource,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MiuixTheme.colorScheme.primary,
            )

            Text(
                modifier = Modifier.weight(1f),
                text = title,
                style = MiuixTheme.textStyles.title4,
            )

            Switch(
                checked = isApplied,
                onCheckedChange = onToggle,
            )
        }
    }
}

@Composable
private fun rememberApplyMenuEntries(viewModel: ApplyViewModel, uiState: ApplyViewState): List<DropdownEntry> {
    val reverseText = stringResource(R.string.sort_by_reverse_order)
    val selectedFirstText = stringResource(R.string.sort_by_selected_first)
    val systemAppText = stringResource(R.string.sort_by_show_system_app)
    val packageNameText = stringResource(R.string.sort_by_show_package_name)
    val unknownScopeText = stringResource(R.string.sort_by_show_unknown_scope)
    return remember(
        uiState.orderInReverse,
        uiState.selectedFirst,
        uiState.showSystemApp,
        uiState.showPackageName,
        uiState.showUnknownScope,
        reverseText,
        selectedFirstText,
        systemAppText,
        packageNameText,
        unknownScopeText,
    ) {
        listOf(
            DropdownEntry(
                items = listOf(
                    DropdownItem(
                        text = reverseText,
                        selected = uiState.orderInReverse,
                        onClick = { viewModel.dispatch(ApplyViewAction.OrderInReverse(!uiState.orderInReverse)) },
                    ),
                    DropdownItem(
                        text = selectedFirstText,
                        selected = uiState.selectedFirst,
                        onClick = { viewModel.dispatch(ApplyViewAction.SelectedFirst(!uiState.selectedFirst)) },
                    ),
                    DropdownItem(
                        text = systemAppText,
                        selected = uiState.showSystemApp,
                        onClick = { viewModel.dispatch(ApplyViewAction.ShowSystemApp(!uiState.showSystemApp)) },
                    ),
                    DropdownItem(
                        text = packageNameText,
                        selected = uiState.showPackageName,
                        onClick = { viewModel.dispatch(ApplyViewAction.ShowPackageName(!uiState.showPackageName)) },
                    ),
                ),
            ),
            DropdownEntry(
                items = listOf(
                    DropdownItem(
                        text = unknownScopeText,
                        selected = uiState.showUnknownScope,
                        onClick = { viewModel.dispatch(ApplyViewAction.ShowUnknownScope(!uiState.showUnknownScope)) },
                    ),
                ),
            ),
        )
    }
}

@Composable
private fun TopAppBarActions(menuEntries: List<DropdownEntry>) {
    WindowIconDropdownMenu(
        entries = menuEntries,
        collapseOnSelection = false,
    ) {
        Icon(
            imageVector = MiuixIcons.Regular.More,
            tint = MiuixTheme.colorScheme.onBackground,
            contentDescription = stringResource(R.string.menu),
        )
    }
}
