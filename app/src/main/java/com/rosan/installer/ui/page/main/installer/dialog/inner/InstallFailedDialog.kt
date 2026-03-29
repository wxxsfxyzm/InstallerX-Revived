// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer.dialog.inner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParamsType
import com.rosan.installer.ui.page.main.widget.chip.Chip
import com.rosan.installer.ui.page.main.widget.dialog.UninstallConfirmationDialog
import com.rosan.installer.ui.util.rememberErrorSuggestions
import kotlinx.coroutines.delay

@Composable
fun installFailedDialog(
    viewModel: InstallerViewModel
): DialogParams {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val viewSettings = uiState.viewSettings
    val currentError = uiState.error

    // Call InstallInfoDialog for base structure
    val baseParams = installInfoDialog(
        viewModel = viewModel,
        onTitleExtraClick = {}
    )

    // Override text and buttons
    return baseParams.copy(
        text = DialogInnerParams(
            DialogParamsType.InstallerInstallFailed.id,
            {
                ErrorTextBlock(
                    currentError,
                    suggestions = {
                        if (viewSettings.showSmartSuggestion)
                            ErrorSuggestions(
                                error = currentError,
                                viewModel = viewModel
                            )
                    }
                )
            }
        ),
        buttons = dialogButtons(
            DialogParamsType.InstallerInstallFailed.id
        ) {
            listOf(
                DialogButton(stringResource(R.string.close)) {
                    viewModel.dispatch(InstallerViewAction.Close)
                }
            )
        }
    )
}

@Composable
private fun ErrorSuggestions(
    error: Throwable,
    viewModel: InstallerViewModel
) {
    var showUninstallConfirmDialog by remember { mutableStateOf(false) }
    var confirmKeepData by remember { mutableStateOf(false) }
    var pendingConflictingPackage by remember { mutableStateOf<String?>(null) }

    val visibleSuggestions = rememberErrorSuggestions(
        error = error,
        viewModel = viewModel,
        onShowUninstallConfirm = { keepData, conflictingPkg ->
            confirmKeepData = keepData
            pendingConflictingPackage = conflictingPkg
            showUninstallConfirmDialog = true
        }
    )

    if (visibleSuggestions.isNotEmpty()) {
        FlowRow(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            visibleSuggestions.forEachIndexed { index, suggestion ->
                var animatedVisibility by remember { mutableStateOf(false) }

                LaunchedEffect(suggestion.labelRes) {
                    delay(50L + index * 50L)
                    animatedVisibility = true
                }

                AnimatedVisibility(
                    visible = animatedVisibility,
                    enter = fadeIn(animationSpec = tween(durationMillis = 200)) + slideInVertically { it / 2 },
                    exit = fadeOut(animationSpec = tween(durationMillis = 150))
                ) {
                    Chip(
                        modifier = Modifier.height(32.dp),
                        selected = true,
                        onClick = suggestion.onClick,
                        useHaptic = true,
                        label = stringResource(id = suggestion.labelRes),
                        icon = suggestion.icon ?: AppIcons.BugReport // Fallback if needed
                    )
                }
            }
        }
        UninstallConfirmationDialog(
            showDialog = showUninstallConfirmDialog,
            onDismiss = { showUninstallConfirmDialog = false },
            onConfirm = {
                viewModel.dispatch(
                    InstallerViewAction.UninstallAndRetryInstall(
                        keepData = confirmKeepData,
                        conflictingPackage = pendingConflictingPackage
                    )
                )
            },
            keepData = confirmKeepData
        )
    }
}
