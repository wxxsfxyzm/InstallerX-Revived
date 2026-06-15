// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer.dialog.inner

import android.text.format.Formatter
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import com.rosan.installer.domain.archive.model.UnarchiveErrorAction
import com.rosan.installer.domain.archive.model.UnarchiveStatus
import com.rosan.installer.ui.page.main.installer.InstallerStage
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.components.ErrorTextBlock
import com.rosan.installer.ui.page.main.installer.components.failedIcon
import com.rosan.installer.ui.page.main.installer.dialog.DialogButton
import com.rosan.installer.ui.page.main.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParamsType
import com.rosan.installer.ui.page.main.installer.dialog.dialogButtons

@Composable
fun unarchiveErrorDialog(viewModel: InstallerViewModel): DialogParams {
    val stage = viewModel.uiState.value.stage as? InstallerStage.UnarchiveError ?: return DialogParams()
    return DialogParams(
        icon = DialogInnerParams(DialogParamsType.IconError.id, failedIcon),
        title = DialogInnerParams(DialogParamsType.InstallerUnarchiveError.id) {
            Text(unarchiveErrorTitle(stage.status, stage.installerLabel))
        },
        text = DialogInnerParams(DialogParamsType.InstallerUnarchiveError.id) {
            Text(unarchiveErrorMessage(stage.status, stage.requiredBytes, stage.installerLabel))
        },
        buttons = dialogButtons(DialogParamsType.InstallerUnarchiveError.id) {
            val buttons = mutableListOf<DialogButton>()
            if (stage.status.primaryAction != UnarchiveErrorAction.CLOSE) {
                buttons += DialogButton(stringResource(stage.status.actionLabelResId)) {
                    viewModel.dispatch(InstallerViewAction.OpenUnarchiveErrorAction)
                }
            }
            buttons += DialogButton(stringResource(R.string.close)) {
                viewModel.dispatch(InstallerViewAction.Close)
            }
            buttons
        }
    )
}

@Composable
fun unarchiveFailedDialog(viewModel: InstallerViewModel): DialogParams {
    val currentError = viewModel.uiState.value.error
    return DialogParams(
        icon = DialogInnerParams(DialogParamsType.IconError.id, failedIcon),
        title = DialogInnerParams(DialogParamsType.InstallerUnarchiveError.id) {
            Text(stringResource(R.string.unarchive_failed))
        },
        text = DialogInnerParams(DialogParamsType.InstallerUnarchiveError.id) {
            ErrorTextBlock(currentError)
        },
        buttons = dialogButtons(DialogParamsType.InstallerUnarchiveError.id) {
            listOf(
                DialogButton(stringResource(R.string.close)) {
                    viewModel.dispatch(InstallerViewAction.Close)
                }
            )
        }
    )
}

@Composable
private fun unarchiveErrorTitle(
    status: UnarchiveStatus,
    installerLabel: CharSequence?
): String = when (status) {
    UnarchiveStatus.InstallerDisabled,
    UnarchiveStatus.InstallerUninstalled -> stringResource(
        status.titleResId,
        installerLabel ?: stringResource(R.string.installer_label_unknown)
    )

    else -> stringResource(status.titleResId)
}

@Composable
private fun unarchiveErrorMessage(
    status: UnarchiveStatus,
    requiredBytes: Long,
    installerLabel: CharSequence?
): String {
    val context = LocalContext.current
    return when (status) {
        UnarchiveStatus.InsufficientStorage -> stringResource(
            status.messageResId,
            Formatter.formatShortFileSize(context, requiredBytes)
        )

        UnarchiveStatus.InstallerDisabled,
        UnarchiveStatus.InstallerUninstalled -> stringResource(
            status.messageResId,
            installerLabel ?: stringResource(R.string.installer_label_unknown)
        )

        else -> stringResource(status.messageResId)
    }
}
