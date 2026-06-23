// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer.dialog.inner

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.rememberUnknownSourcePermissionActionVisible
import com.rosan.installer.ui.page.main.installer.components.workingIcon
import com.rosan.installer.ui.page.main.installer.dialog.DialogButton
import com.rosan.installer.ui.page.main.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParamsType
import com.rosan.installer.ui.page.main.installer.dialog.dialogButtons

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun installWaitingUnknownSourceDialog(
    viewModel: InstallerViewModel
): DialogParams {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val sourceAppLabel = uiState.initiatorAppLabel ?: stringResource(R.string.installer_label_unknown)
    val description = stringResource(R.string.installer_waiting_unknown_source_desc, sourceAppLabel)
    val showPermissionAction = rememberUnknownSourcePermissionActionVisible(
        isWaitingUnknownSource = true
    )

    return DialogParams(
        icon = DialogInnerParams(
            DialogParamsType.IconWorking.id,
            workingIcon
        ),
        title = DialogInnerParams(
            DialogParamsType.InstallerUnknownSource.id
        ) {
            Text(stringResource(R.string.installer_waiting_unknown_source))
        },
        text = DialogInnerParams(
            DialogParamsType.InstallerUnknownSource.id
        ) {
            Column(
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                LinearWavyProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    amplitude = 0f
                )
            }
        },
        buttons = dialogButtons(
            DialogParamsType.ButtonsCancel.id
        ) {
            buildList {
                if (showPermissionAction) {
                    add(
                        DialogButton(stringResource(R.string.suggestion_allow_unknown_source)) {
                            viewModel.dispatch(InstallerViewAction.RequestUnknownSourcePermission)
                        }
                    )
                }
                add(
                    DialogButton(stringResource(R.string.cancel)) {
                        viewModel.dispatch(InstallerViewAction.Cancel)
                    }
                )
            }
        }
    )
}
