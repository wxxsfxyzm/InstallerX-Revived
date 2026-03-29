// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer.dialog.inner

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.ui.page.main.installer.InstallerStage
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParamsType

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun installingDialog(
    viewModel: InstallerViewModel
): DialogParams {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stage = uiState.stage
    val viewSettings = uiState.viewSettings

    val installingStage = stage as? InstallerStage.Installing

    // Call InstallInfoDialog for base structure (icon, title, subtitle with new version)
    val baseParams = installInfoDialog(
        viewModel = viewModel,
        onTitleExtraClick = {}
    )

    // Override text and buttons
    return baseParams.copy(
        text = DialogInnerParams(
            DialogParamsType.InstallerInstalling.id
        ) {
            Column {
                if (installingStage != null) {
                    val displayLabel = installingStage.appLabel ?: "Unknown"

                    val formattedText = if (installingStage.total > 1) {
                        stringResource(
                            R.string.installing_progress_text,
                            displayLabel,
                            installingStage.current,
                            installingStage.total
                        )
                    } else null

                    formattedText?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                    }
                }
                // --- M3E ---
                val currentProgress = installingStage?.progress
                if (currentProgress != null && currentProgress > 0f) {
                    val animatedProgress by animateFloatAsState(
                        targetValue = currentProgress,
                        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                        label = "ProgressBarAnimation"
                    )
                    if (viewSettings.uiExpressive)
                        LinearWavyProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                        )
                    else
                        LinearWavyProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxWidth(),
                            amplitude = { 0f }
                        )
                } else {
                    // other method have unspecified progress
                    LinearWavyProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        amplitude = 0f // not wavy
                    )
                }
            }
        },
        buttons = dialogButtons(DialogParamsType.ButtonsCancel.id) {
            // Provides a button to move to background
            listOf(
                DialogButton(stringResource(R.string.installer_silent_install)) {
                    viewModel.dispatch(InstallerViewAction.Background)
                }
            )
        }
    )
}
