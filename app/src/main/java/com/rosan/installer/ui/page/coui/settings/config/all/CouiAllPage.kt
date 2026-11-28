// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.settings.config.all

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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.icons.CouiIcons
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.layout.Divider
import com.itos.coui.layout.Scaffold
import com.itos.coui.shape.CouiCapsuleShape
import com.itos.coui.theme.CouiTheme
import com.itos.coui.topappbar.CouiTopAppBarDefaults
import com.itos.coui.topappbar.LargeTopAppBar
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.config.ConfigModel
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.coui.widgets.CouiBadge
import com.rosan.installer.ui.page.coui.widgets.CouiCard
import com.rosan.installer.ui.page.coui.widgets.CouiScopeTipCard
import com.rosan.installer.ui.page.main.settings.config.all.AllViewAction
import com.rosan.installer.ui.page.main.settings.config.all.AllViewModel
import com.rosan.installer.ui.page.main.settings.config.all.AllViewState
import com.rosan.installer.ui.page.main.widget.util.AllViewEventCollector
import org.koin.androidx.compose.koinViewModel

@Composable
fun CouiAllPage(
    enableBlur: Boolean,
    viewModel: AllViewModel = koinViewModel(),
    title: String,
    outerPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
) {
    val navigator = LocalNavigator.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyGridState()
    val scrollBehavior = CouiTopAppBarDefaults.exitUntilCollapsedScrollBehavior {
        listState.canScrollForward || listState.canScrollBackward
    }
    val density = LocalDensity.current

    AllViewEventCollector(
        viewModel = viewModel,
        navigator = navigator,
        onShowSnackbar = { message, actionLabel ->
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                withDismissAction = true,
            )
            result == SnackbarResult.ActionPerformed
        },
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CouiTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(text = title) },
                scrollBehavior = scrollBehavior,
                useCouiBlur = enableBlur,
            )
        },
    ) { innerPadding ->
        val contentPadding = remember(scrollBehavior.state, innerPadding, density) {
            scrollBehavior.state.contentPadding(innerPadding, density)
        }
        val bottomPadding = maxOf(
            outerPadding.calculateBottomPadding(),
            contentPadding.calculateBottomPadding(),
        )

        when (uiState.data.progress) {
            is AllViewState.Data.Progress.Loading if uiState.data.configs.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .consumeWindowInsets(innerPadding)
                        .padding(
                            top = contentPadding.calculateTopPadding(),
                            bottom = bottomPadding,
                        ),
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

            is AllViewState.Data.Progress.Loaded if uiState.data.configs.isEmpty() -> {
                // Default profile cannot be removed.
            }

            else -> {
                val configs = uiState.data.configs
                val minId = configs.minByOrNull { it.id }?.id

                LazyVerticalGrid(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .consumeWindowInsets(innerPadding),
                    columns = GridCells.Adaptive(350.dp),
                    contentPadding = PaddingValues(
                        start = CouiPageDefaults.horizontalMargin,
                        end = CouiPageDefaults.horizontalMargin,
                        top = contentPadding.calculateTopPadding() + 8.dp,
                        bottom = bottomPadding + 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (!uiState.userReadScopeTips) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            CouiScopeTipCard(viewModel = viewModel)
                        }
                    }

                    items(configs, key = { it.id }) { entity ->
                        CouiConfigCard(
                            viewModel = viewModel,
                            entity = entity,
                            isDefault = entity.id == minId,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CouiConfigCard(viewModel: AllViewModel, entity: ConfigModel, isDefault: Boolean) {
    val dark = CouiTheme.isDark
    val scheme = CouiTheme.colorScheme
    val errorColor = if (dark) Color(0xFFFF6C61) else Color(0xFFDB382C)

    CouiCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = entity.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                )
                if (isDefault) {
                    CouiBadge(stringResource(id = R.string.config_global_default))
                } else if (entity.scopeCount == 0) {
                    CouiBadge(
                        text = stringResource(id = R.string.config_status_inactive),
                        textColor = errorColor,
                        containerColor = errorColor.copy(alpha = 0.16f),
                    )
                }
            }
            if (entity.description.isNotEmpty()) {
                Text(
                    text = entity.description,
                    color = scheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }
        }

        Divider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CouiCardActionPill(
                icon = CouiIcons.Outlined.Pen,
                contentDescription = stringResource(id = R.string.edit),
                onClick = { viewModel.dispatch(AllViewAction.EditDataConfig(entity)) },
            )
            if (!isDefault) {
                CouiCardActionPill(
                    icon = CouiIcons.Outlined.Delete,
                    contentDescription = stringResource(id = R.string.delete),
                    onClick = { viewModel.dispatch(AllViewAction.DeleteDataConfig(entity)) },
                )
                Spacer(modifier = Modifier.weight(1f))
                CouiCardActionPill(
                    icon = AppIcons.Rule,
                    label = stringResource(R.string.config_scope),
                    contentDescription = stringResource(id = R.string.apply),
                    onClick = { viewModel.dispatch(AllViewAction.ApplyConfig(entity)) },
                )
            }
        }
    }
}

@Composable
private fun CouiCardActionPill(
    icon: ImageVector,
    contentDescription: String,
    label: String? = null,
    onClick: () -> Unit,
) {
    val dark = CouiTheme.isDark
    val scheme = CouiTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val pillBg = scheme.onSurface.copy(alpha = if (dark) 0.12f else 0.06f)

    Row(
        modifier = Modifier
            .defaultMinSize(minWidth = 36.dp, minHeight = 36.dp)
            .clip(CouiCapsuleShape)
            .background(pillBg)
            .couiButtonFeedback(dark = dark, interactionSource = source)
            .clickable(
                interactionSource = source,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = if (label != null) 14.dp else 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp),
            tint = scheme.onSurface,
        )
        if (label != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = scheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
