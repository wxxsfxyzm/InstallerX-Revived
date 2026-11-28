// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.settings.config.apply

import android.os.Build
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
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.floatingactionbutton.Fab
import com.itos.coui.icons.CouiIcons
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.layout.Scaffold
import com.itos.coui.menu.CouiMenuItem
import com.itos.coui.menu.Menu
import com.itos.coui.shape.CouiCapsuleShape
import com.itos.coui.switch.Switch
import com.itos.coui.text.CouiTextKind
import com.itos.coui.theme.CouiTheme
import com.itos.coui.topappbar.CouiTopAppBarDefaults
import com.itos.coui.topappbar.TopAppBarSurface
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.coui.widgets.CouiBackButton
import com.rosan.installer.ui.page.coui.widgets.CouiTopBarIconButton
import com.rosan.installer.ui.page.main.settings.config.apply.ApplyViewAction
import com.rosan.installer.ui.page.main.settings.config.apply.ApplyViewApp
import com.rosan.installer.ui.page.main.settings.config.apply.ApplyViewModel
import com.rosan.installer.ui.page.main.settings.config.apply.ApplyViewState
import com.rosan.installer.ui.page.main.settings.config.apply.ViewContent
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import com.itos.coui.text.Text as CouiText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouiApplyPage(id: Long, useBlur: Boolean, viewModel: ApplyViewModel = koinViewModel { parametersOf(id) }) {
    val navigator = LocalNavigator.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val showFloating by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CouiTheme.colorScheme.background,
        topBar = {
            TopAppBarSurface(
                modifier = Modifier.fillMaxWidth(),
                useCouiBlur = useBlur,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(CouiTopAppBarDefaults.windowInsets)
                        .padding(bottom = 8.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(CouiTopAppBarDefaults.height)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CouiBackButton(
                            icon = CouiIcons.Filled.Close,
                            onClick = { navigator.pop() },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        CouiText(
                            text = stringResource(R.string.config_scope),
                            kind = CouiTextKind.TopBar,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        CouiApplyOverflowMenu(viewModel = viewModel, uiState = uiState)
                    }

                    CouiApplySearchBar(
                        query = uiState.search,
                        onQueryChange = { viewModel.dispatch(ApplyViewAction.Search(it)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    )

                    CouiApplySortSelector(
                        currentOrder = uiState.orderType,
                        onOrderSelected = { viewModel.dispatch(ApplyViewAction.Order(it)) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showFloating,
                enter = scaleIn(),
                exit = scaleOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp),
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    Fab(
                        onClick = {
                            coroutineScope.launch { lazyListState.animateScrollToItem(0) }
                        },
                    ) {
                        Icon(
                            imageVector = AppIcons.ArrowUp,
                            contentDescription = null,
                        )
                    }
                } else {
                    FloatingActionButton(
                        containerColor = CouiTheme.colorScheme.controlAccent,
                        contentColor = Color.White,
                        onClick = {
                            coroutineScope.launch { lazyListState.animateScrollToItem(0) }
                        },
                    ) {
                        Icon(
                            imageVector = AppIcons.ArrowUp,
                            contentDescription = null,
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        when {
            uiState.apps.progress is ViewContent.Progress.Loading && uiState.apps.data.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .consumeWindowInsets(paddingValues)
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CircularProgressIndicator(
                            color = CouiTheme.colorScheme.accent,
                            strokeWidth = 3.dp,
                        )
                        Text(
                            text = stringResource(id = R.string.loading),
                            color = CouiTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
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

                PullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = { viewModel.dispatch(ApplyViewAction.LoadApps) },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .consumeWindowInsets(paddingValues),
                        state = lazyListState,
                        contentPadding = PaddingValues(
                            top = paddingValues.calculateTopPadding() + 8.dp,
                            bottom = paddingValues.calculateBottomPadding() + 16.dp,
                        ),
                    ) {
                        val apps = uiState.checkedApps
                        val showUnknown = uiState.showUnknownScope &&
                            (uiState.search.isBlank() || unknownLabel.contains(uiState.search, ignoreCase = true))

                        if (showUnknown) {
                            item(key = "unknown_scope", contentType = "scope_item") {
                                val isApplied = appliedPackageSet.contains(null)
                                CouiUnknownScopeItemWidget(
                                    modifier = Modifier.padding(horizontal = CouiPageDefaults.horizontalMargin),
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
                            item(key = "unknown_apps_group_spacing", contentType = "scope_group_spacing") {
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }

                        itemsIndexed(
                            items = apps,
                            key = { _, app -> app.packageName },
                            contentType = { _, _ -> "app_item" },
                        ) { index, app ->
                            val cardRadius = 20.dp
                            val shape = when {
                                apps.size == 1 -> RoundedCornerShape(cardRadius)
                                index == 0 -> RoundedCornerShape(topStart = cardRadius, topEnd = cardRadius)
                                index == apps.lastIndex -> RoundedCornerShape(bottomStart = cardRadius, bottomEnd = cardRadius)
                                else -> RoundedCornerShape(0.dp)
                            }

                            val isApplied = appliedPackageSet.contains(app.packageName)
                            LaunchedEffect(app.packageName) {
                                viewModel.dispatch(ApplyViewAction.LoadIcon(app.packageName))
                            }
                            val iconBitmap = uiState.displayIcons[app.packageName]

                            CouiApplyAppItem(
                                modifier = Modifier
                                    .padding(horizontal = CouiPageDefaults.horizontalMargin)
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
                                icon = iconBitmap,
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
                    }
                }
            }
        }
    }
}

@Composable
private fun CouiApplySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = CouiTheme.colorScheme
    val dark = CouiTheme.isDark
    val surfaceColor = scheme.onSurface.copy(alpha = if (dark) 0.10f else 0.06f)

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = TextStyle(
            color = scheme.onSurface,
            fontSize = 15.sp,
        ),
        cursorBrush = SolidColor(scheme.accent),
        modifier = modifier
            .height(40.dp)
            .clip(CouiCapsuleShape)
            .background(surfaceColor),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = CouiIcons.Outlined.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = scheme.onSurfaceVariant,
                )
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search),
                            color = scheme.onSurfaceVariant,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = CouiIcons.Filled.Close,
                            contentDescription = stringResource(R.string.clear),
                            modifier = Modifier.size(16.dp),
                            tint = scheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun CouiApplySortSelector(
    currentOrder: ApplyViewState.OrderType,
    onOrderSelected: (ApplyViewState.OrderType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val labelText = stringResource(R.string.sort_by_label)
    val pkgText = stringResource(R.string.sort_by_package_name)
    val installTimeText = stringResource(R.string.sort_by_install_time)

    val options = remember(labelText, pkgText, installTimeText) {
        listOf(
            ApplyViewState.OrderType.Label to labelText,
            ApplyViewState.OrderType.PackageName to pkgText,
            ApplyViewState.OrderType.FirstInstallTime to installTimeText,
        )
    }
    val selectedLabel = options.firstOrNull { it.first == currentOrder }?.second ?: labelText
    var expanded by remember { mutableStateOf(false) }
    val dark = CouiTheme.isDark
    val scheme = CouiTheme.colorScheme
    val source = remember { MutableInteractionSource() }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(CouiCapsuleShape)
                .background(scheme.onSurface.copy(alpha = if (dark) 0.10f else 0.06f))
                .couiButtonFeedback(dark = dark, interactionSource = source)
                .clickable(
                    interactionSource = source,
                    indication = null,
                    role = Role.Button,
                    onClick = { expanded = true },
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = selectedLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = scheme.onSurface,
            )
            Icon(
                imageVector = CouiIcons.Outlined.Popup,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = scheme.onSurfaceVariant,
            )
        }

        Menu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            items = options.map { (type, text) ->
                CouiMenuItem(
                    value = type,
                    text = text,
                    selected = type == currentOrder,
                )
            },
            onItemClick = { clicked ->
                expanded = false
                onOrderSelected(clicked.value)
            },
            offset = DpOffset(0.dp, 4.dp),
        )
    }
}

private enum class ApplyMenuOption {
    Reverse,
    SelectedFirst,
    ShowSystemApp,
    ShowPackageName,
    ShowUnknownScope,
}

@Composable
private fun CouiApplyOverflowMenu(viewModel: ApplyViewModel, uiState: ApplyViewState) {
    var expanded by remember { mutableStateOf(false) }
    val reverseText = stringResource(R.string.sort_by_reverse_order)
    val selectedFirstText = stringResource(R.string.sort_by_selected_first)
    val systemAppText = stringResource(R.string.sort_by_show_system_app)
    val packageNameText = stringResource(R.string.sort_by_show_package_name)
    val unknownScopeText = stringResource(R.string.sort_by_show_unknown_scope)

    val menuItems = remember(
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
            CouiMenuItem(
                value = ApplyMenuOption.Reverse,
                text = reverseText,
                checked = uiState.orderInReverse,
            ),
            CouiMenuItem(
                value = ApplyMenuOption.SelectedFirst,
                text = selectedFirstText,
                checked = uiState.selectedFirst,
            ),
            CouiMenuItem(
                value = ApplyMenuOption.ShowSystemApp,
                text = systemAppText,
                checked = uiState.showSystemApp,
            ),
            CouiMenuItem(
                value = ApplyMenuOption.ShowPackageName,
                text = packageNameText,
                checked = uiState.showPackageName,
            ),
            CouiMenuItem(
                value = ApplyMenuOption.ShowUnknownScope,
                text = unknownScopeText,
                checked = uiState.showUnknownScope,
            ),
        )
    }

    Box {
        CouiTopBarIconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = CouiIcons.Outlined.More,
                contentDescription = stringResource(R.string.menu),
            )
        }
        Menu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            items = menuItems,
            onItemClick = { clicked ->
                when (clicked.value) {
                    ApplyMenuOption.Reverse -> viewModel.dispatch(ApplyViewAction.OrderInReverse(!uiState.orderInReverse))
                    ApplyMenuOption.SelectedFirst -> viewModel.dispatch(ApplyViewAction.SelectedFirst(!uiState.selectedFirst))
                    ApplyMenuOption.ShowSystemApp -> viewModel.dispatch(ApplyViewAction.ShowSystemApp(!uiState.showSystemApp))
                    ApplyMenuOption.ShowPackageName -> viewModel.dispatch(ApplyViewAction.ShowPackageName(!uiState.showPackageName))
                    ApplyMenuOption.ShowUnknownScope -> viewModel.dispatch(ApplyViewAction.ShowUnknownScope(!uiState.showUnknownScope))
                }
            },
            offset = DpOffset(0.dp, 4.dp),
        )
    }
}

@Composable
private fun CouiApplyAppItem(
    modifier: Modifier = Modifier,
    app: ApplyViewApp,
    icon: ImageBitmap?,
    isApplied: Boolean,
    shape: Shape,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    showPackageName: Boolean,
) {
    val dark = CouiTheme.isDark
    val scheme = CouiTheme.colorScheme
    val source = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(scheme.card),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .couiButtonFeedback(dark = dark, interactionSource = source, list = true)
                .clickable(
                    interactionSource = source,
                    indication = null,
                    onClick = onClick,
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Image(
                    bitmap = icon,
                    modifier = Modifier.size(40.dp),
                    contentDescription = null,
                )
            } else {
                Box(modifier = Modifier.size(40.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                CouiText(
                    text = app.label ?: app.packageName,
                    kind = CouiTextKind.ItemTitle,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AnimatedVisibility(showPackageName) {
                    CouiText(
                        text = app.packageName,
                        kind = CouiTextKind.ItemSummary,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Switch(
                checked = isApplied,
                onCheckedChange = null,
                onDragCheckedChange = onToggle,
                interactionSource = source,
            )
        }
    }
}

@Composable
private fun CouiUnknownScopeItemWidget(
    modifier: Modifier = Modifier,
    title: String,
    isApplied: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    val dark = CouiTheme.isDark
    val scheme = CouiTheme.colorScheme
    val source = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(CouiPageDefaults.shape)
            .background(scheme.card),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .couiButtonFeedback(dark = dark, interactionSource = source, list = true)
                .clickable(
                    interactionSource = source,
                    indication = null,
                    onClick = onClick,
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = AppIcons.InstallSource,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = scheme.accent,
            )
            CouiText(
                modifier = Modifier.weight(1f),
                text = title,
                kind = CouiTextKind.ItemTitle,
                color = scheme.onSurface,
            )
            Switch(
                checked = isApplied,
                onCheckedChange = null,
                onDragCheckedChange = onToggle,
                interactionSource = source,
            )
        }
    }
}
