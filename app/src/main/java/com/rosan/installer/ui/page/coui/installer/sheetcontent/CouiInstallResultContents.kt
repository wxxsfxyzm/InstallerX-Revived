// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.installer.sheetcontent

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.itos.coui.icons.CouiIcons
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.layout.Divider
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.text.CouiTextKind
import com.itos.coui.theme.CouiTheme
import com.rosan.installer.R
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.privileged.usecase.OpenAppUseCase
import com.rosan.installer.domain.privileged.usecase.OpenAppUseCase.Companion.PRIVILEGED_START_TIMEOUT_MS
import com.rosan.installer.domain.privileged.usecase.OpenLSPosedUseCase
import com.rosan.installer.domain.session.model.InstallResult
import com.rosan.installer.domain.settings.model.config.isPrivileged
import com.rosan.installer.ui.page.coui.installer.components.CouiAppInfoSlot
import com.rosan.installer.ui.page.coui.installer.components.CouiAppInfoState
import com.rosan.installer.ui.page.coui.widgets.CouiActionButton
import com.rosan.installer.ui.page.coui.widgets.CouiCard
import com.rosan.installer.ui.page.coui.widgets.CouiErrorTextBlock
import com.rosan.installer.ui.page.coui.widgets.CouiNavigationItem
import com.rosan.installer.ui.page.coui.widgets.CouiUninstallConfirmationDialog
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.util.rememberErrorSuggestions
import com.rosan.installer.util.help
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import com.itos.coui.text.Text as CouiText

@Composable
fun CouiInstallSuccessContent(
    appInfo: CouiAppInfoState,
    viewModel: InstallerViewModel,
    closeSessionCountDown: Int,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val config = uiState.config
    val coroutineScope = rememberCoroutineScope()
    val capabilityProvider: DeviceCapabilityProvider = koinInject()
    val openAppUseCase: OpenAppUseCase = koinInject()
    val openLSPosedUseCase: OpenLSPosedUseCase = koinInject()
    val scheme = CouiTheme.colorScheme

    val isXposedModule = uiState.viewSettings.detectXposedModule &&
        (appInfo.primaryEntity as? AppEntity.BaseEntity)?.isXposedModule == true
    val hasPrivilege = config.isPrivileged(capabilityProvider)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CouiAppInfoSlot(appInfo = appInfo)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.installer_install_success),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = scheme.accentOnSurface,
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (isXposedModule && uiState.viewSettings.quickOpenLSPosed && hasPrivilege) {
            CouiActionButton(
                text = stringResource(R.string.open_lsposed),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                onClick = {
                    viewModel.dispatch(InstallerViewAction.PrepareClose)
                    coroutineScope.launch(Dispatchers.IO) {
                        val success = openLSPosedUseCase(config)
                        if (success) {
                            withContext(Dispatchers.Main) {
                                viewModel.dispatch(InstallerViewAction.Close)
                            }
                        }
                    }
                },
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val intent = if (appInfo.packageName.isNotEmpty()) {
                context.packageManager.getLaunchIntentForPackage(appInfo.packageName)
            } else {
                null
            }
            CouiActionButton(
                text = stringResource(R.string.finish),
                onClick = onClose,
                modifier = Modifier.weight(1f),
            )
            if (intent != null) {
                CouiActionButton(
                    text = stringResource(R.string.open),
                    primary = true,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.dispatch(InstallerViewAction.PrepareClose)
                        coroutineScope.launch(Dispatchers.IO) {
                            when (openAppUseCase(config = config, launchIntent = intent)) {
                                is OpenAppUseCase.Result.SuccessPrivileged -> {
                                    withContext(Dispatchers.Main) {
                                        viewModel.dispatch(InstallerViewAction.Close)
                                    }
                                }
                                is OpenAppUseCase.Result.FallbackRequired -> {
                                    withContext(Dispatchers.Main) {
                                        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                        if (!hasPrivilege) {
                                            delay(closeSessionCountDown * 1000L)
                                        } else {
                                            delay(PRIVILEGED_START_TIMEOUT_MS)
                                        }
                                        viewModel.dispatch(InstallerViewAction.Close)
                                    }
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
fun CouiInstallCompletedContent(results: List<InstallResult>, onClose: () -> Unit) {
    val filteredResults = remember(results) {
        results.groupBy { it.entity.app.packageName }.flatMap { (_, groupResults) ->
            val baseResult = groupResults.find { it.entity.app is AppEntity.BaseEntity }
            if (baseResult != null) listOf(baseResult) else listOfNotNull(groupResults.firstOrNull())
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.weight(1f, fill = false)) {
            LazyColumn(
                modifier = Modifier.wrapContentSize(),
                overscrollEffect = rememberCouiOverscrollEffect(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(filteredResults, key = { it.entity.app.packageName + it.entity.app.name }) { result ->
                    CouiResultItemCard(result = result)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 12.dp),
        ) {
            CouiActionButton(
                text = stringResource(R.string.finish),
                onClick = onClose,
                primary = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CouiResultItemCard(result: InstallResult) {
    val scheme = CouiTheme.colorScheme
    val app = result.entity.app
    val appLabel = (app as? AppEntity.BaseEntity)?.label ?: app.packageName

    CouiCard {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column {
                Text(text = appLabel, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                Text(text = app.packageName, fontSize = 13.sp, color = scheme.onSurfaceVariant)
            }

            if (result.success) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = scheme.accentOnSurface,
                    )
                    Text(
                        text = stringResource(R.string.installer_install_success),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.accentOnSurface,
                    )
                }
            } else if (result.error != null) {
                val errColor = if (CouiTheme.isDark) Color(0xFFFF7A70) else Color(0xFFD93025)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = CouiIcons.Filled.Close,
                            contentDescription = stringResource(R.string.installer_install_failed),
                            modifier = Modifier.size(18.dp),
                            tint = errColor,
                        )
                        Text(
                            text = result.error.help(),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = errColor,
                        )
                    }
                    Divider()
                    Text(
                        text = (result.error.message ?: "An unknown error occurred.").trim(),
                        fontSize = 13.sp,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun CouiInstallFailedContent(
    appInfo: CouiAppInfoState,
    viewModel: InstallerViewModel,
    onClose: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentError = uiState.error

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CouiAppInfoSlot(appInfo = appInfo)
        Spacer(modifier = Modifier.height(20.dp))
        CouiErrorTextBlock(
            error = currentError,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
        )
        Spacer(modifier = Modifier.height(12.dp))
        CouiErrorSuggestions(error = currentError, viewModel = viewModel)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 12.dp),
        ) {
            CouiActionButton(
                text = stringResource(R.string.close),
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CouiErrorSuggestions(error: Throwable, viewModel: InstallerViewModel) {
    val pageColors = CouiPageDefaults.themeColors()
    val showUninstallConfirmDialogState = remember { mutableStateOf(false) }
    var confirmKeepData by remember { mutableStateOf(false) }
    var pendingConflictingPackage by remember { mutableStateOf<String?>(null) }

    val visibleSuggestions = rememberErrorSuggestions(
        error = error,
        viewModel = viewModel,
        onShowUninstallConfirm = { keepData, conflictingPkg ->
            confirmKeepData = keepData
            pendingConflictingPackage = conflictingPkg
            showUninstallConfirmDialogState.value = true
        },
    )

    if (visibleSuggestions.isNotEmpty()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            CouiText(
                text = stringResource(R.string.smart_suggestions),
                kind = CouiTextKind.GroupTitle,
                color = pageColors.summary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
            CouiCard(modifier = Modifier.padding(bottom = 6.dp)) {
                visibleSuggestions.forEachIndexed { idx, suggestion ->
                    if (idx > 0) Divider()
                    CouiNavigationItem(
                        title = stringResource(id = suggestion.labelRes),
                        description = suggestion.descriptionRes?.let { stringResource(id = it) },
                        onClick = suggestion.onClick,
                    )
                }
            }
        }
    }

    CouiUninstallConfirmationDialog(
        showState = showUninstallConfirmDialogState,
        onDismiss = { showUninstallConfirmDialogState.value = false },
        onConfirm = {
            showUninstallConfirmDialogState.value = false
            viewModel.dispatch(
                InstallerViewAction.UninstallAndRetryInstall(
                    keepData = confirmKeepData,
                    conflictingPackage = pendingConflictingPackage,
                ),
            )
        },
        keepData = confirmKeepData,
    )
}

@Composable
fun CouiNonInstallFailedContent(error: Throwable, onClose: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CouiErrorTextBlock(
            error = error,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
        )
        Spacer(modifier = Modifier.height(16.dp))
        CouiActionButton(
            text = stringResource(R.string.close),
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        )
    }
}
