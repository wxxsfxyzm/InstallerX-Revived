// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
@file:OptIn(ExperimentalMaterial3Api::class)

package com.rosan.installer.ui.page.main.settings.config.all

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.config.ConfigModel
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.navigation.Route
import com.rosan.installer.ui.page.main.widget.card.ScopeTipCard
import com.rosan.installer.ui.page.main.widget.chip.CapsuleTag
import com.rosan.installer.ui.page.main.widget.snackbar.SwipeableSnackbarHost
import com.rosan.installer.ui.page.main.widget.util.AllViewEventCollector
import com.rosan.installer.ui.theme.getMaterial3AppBarColor
import com.rosan.installer.ui.theme.installerMaterial3BlurEffect
import com.rosan.installer.ui.theme.rememberMaterial3BlurBackdrop
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AllPage(
    useBlur: Boolean,
    viewModel: AllViewModel = koinViewModel(),
    title: String,
    outerPadding: PaddingValues = PaddingValues(0.dp),
    windowInsetsSides: WindowInsetsSides? = null
) {
    val navigator = LocalNavigator.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyGridState()
    val snackBarHostState = remember { SnackbarHostState() }
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val showFloatingState = remember { mutableStateOf(true) }
    val showFloating by showFloatingState

    LaunchedEffect(listState) {
        var previousIndex = listState.firstVisibleItemIndex
        var previousOffset = listState.firstVisibleItemScrollOffset

        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collectLatest { (index, offset) ->
                val isScrollingDown = when {
                    index > previousIndex -> true
                    index < previousIndex -> false
                    else -> offset > previousOffset
                }

                previousIndex = index
                previousOffset = offset

                val newShowFloating = !isScrollingDown
                if (showFloatingState.value != newShowFloating) {
                    showFloatingState.value = newShowFloating
                }
            }
    }

    AllViewEventCollector(
        viewModel = viewModel,
        navigator = navigator,
        onShowSnackbar = { message, actionLabel ->
            val result = snackBarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                withDismissAction = true
            )
            result == SnackbarResult.ActionPerformed
        }
    )

    val layoutDirection = LocalLayoutDirection.current

    val backdrop = rememberMaterial3BlurBackdrop(useBlur)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = windowInsetsSides?.let { ScaffoldDefaults.contentWindowInsets.only(it) }
            ?: ScaffoldDefaults.contentWindowInsets,
        topBar = {
            LargeFlexibleTopAppBar(
                modifier = Modifier.installerMaterial3BlurEffect(backdrop),
                windowInsets = windowInsetsSides?.let { TopAppBarDefaults.windowInsets.only(it) }
                    ?: TopAppBarDefaults.windowInsets,
                title = {
                    Text(
                        text = title,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backdrop.getMaterial3AppBarColor(),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    scrolledContainerColor = backdrop.getMaterial3AppBarColor()
                )
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                modifier = Modifier
                    .padding(
                        bottom = outerPadding.calculateBottomPadding()
                    )
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.End)),
                visible = showFloating,
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                SmallExtendedFloatingActionButton(
                    icon = {
                        Icon(
                            imageVector = AppIcons.Add,
                            contentDescription = stringResource(id = R.string.add)
                        )
                    },
                    text = {
                        Text(text = stringResource(id = R.string.add))
                    },
                    onClick = {
                        navigator.push(Route.EditConfig(-1))
                    }
                )
            }
        },
        snackbarHost = {
            SwipeableSnackbarHost(
                hostState = snackBarHostState,
                snackbar = { SnackbarHost(hostState = snackBarHostState) }
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (uiState.data.progress) {
                is AllViewState.Data.Progress.Loading if uiState.data.configs.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top = innerPadding.calculateTopPadding(),
                                bottom = outerPadding.calculateBottomPadding()
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ContainedLoadingIndicator(
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                            Text(
                                text = stringResource(id = R.string.loading),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }

                is AllViewState.Data.Progress.Loaded if uiState.data.configs.isEmpty() -> {
                    // Since we don't allow removing default profile,
                    // There is no need to handle an empty state.
                }

                else -> {
                    ShowDataWidget(
                        viewModel = viewModel,
                        listState = listState,
                        backdrop = backdrop,
                        contentPadding = PaddingValues(
                            top = innerPadding.calculateTopPadding() + 16.dp,
                            bottom = outerPadding.calculateBottomPadding() + 16.dp,
                            start = 16.dp + innerPadding.calculateStartPadding(layoutDirection) + outerPadding.calculateStartPadding(
                                layoutDirection
                            ),
                            end = 16.dp + innerPadding.calculateEndPadding(layoutDirection) + outerPadding.calculateEndPadding(
                                layoutDirection
                            )
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ShowDataWidget(
    viewModel: AllViewModel,
    listState: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    backdrop: LayerBackdrop? = null,
    adaptiveMinSize: Dp = 320.dp
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val configs = uiState.data.configs
    val minId = configs.minByOrNull { it.id }?.id

    LazyVerticalGrid(
        modifier = Modifier
            .fillMaxSize()
            .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
        columns = GridCells.Adaptive(adaptiveMinSize),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        state = listState,
    ) {
        // Insert the tip card as a list item to match MIUIX behavior
        if (!uiState.userReadScopeTips)
            item {
                ScopeTipCard(viewModel = viewModel)
            }

        items(configs) { entity ->
            DataItemWidget(
                viewModel = viewModel,
                entity = entity,
                isDefault = entity.id == minId
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DataItemWidget(
    viewModel: AllViewModel,
    entity: ConfigModel,
    isDefault: Boolean
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = entity.name,
                        style = MaterialTheme.typography.titleMediumEmphasized
                    )
                    if (isDefault) {
                        Spacer(modifier = Modifier.size(8.dp))
                        CapsuleTag(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            text = stringResource(R.string.config_global_default),
                        )
                    } else if (entity.scopeCount == 0) {
                        Spacer(modifier = Modifier.size(8.dp))
                        // Display a warning tag for configurations that are not default and have no scopes applied
                        CapsuleTag(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            text = stringResource(R.string.config_status_inactive),
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
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
                .padding(horizontal = 24.dp),
            color = MaterialTheme.colorScheme.outline
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
                        imageVector = AppIcons.Edit,
                        contentDescription = stringResource(id = R.string.edit)
                    )
                }
                if (!isDefault)
                    IconButton(onClick = { viewModel.dispatch(AllViewAction.DeleteDataConfig(entity)) }) {
                        Icon(
                            imageVector = AppIcons.Delete,
                            contentDescription = stringResource(id = R.string.delete)
                        )
                    }
                if (!isDefault) IconButton(onClick = {
                    viewModel.dispatch(AllViewAction.ApplyConfig(entity))
                }) {
                    Icon(
                        imageVector = AppIcons.Rule,
                        contentDescription = stringResource(id = R.string.apply)
                    )
                }
            }
        }
    }
}
