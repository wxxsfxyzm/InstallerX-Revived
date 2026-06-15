// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer.dialog.inner

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import com.rosan.installer.ui.page.main.installer.InstallerStage
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.components.workingIcon
import com.rosan.installer.ui.page.main.installer.dialog.DialogButton
import com.rosan.installer.ui.page.main.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParamsType
import com.rosan.installer.ui.page.main.installer.dialog.dialogButtons

@Composable
fun unarchiveReadyDialog(viewModel: InstallerViewModel): DialogParams {
    val stage = viewModel.uiState.value.stage as? InstallerStage.UnarchiveReady ?: return DialogParams()
    return DialogParams(
        title = DialogInnerParams(DialogParamsType.InstallerUnarchiveReady.id) {
            Text(stringResource(R.string.unarchive_title, stage.appLabel))
        },
        text = DialogInnerParams(DialogParamsType.InstallerUnarchiveReady.id) {
            Text(stringResource(R.string.unarchive_message, stage.installerLabel))
        },
        buttons = dialogButtons(DialogParamsType.InstallerUnarchiveReady.id) {
            listOf(
                DialogButton(stringResource(R.string.unarchive_restore)) {
                    viewModel.dispatch(InstallerViewAction.StartUnarchive)
                },
                DialogButton(stringResource(R.string.cancel)) {
                    viewModel.dispatch(InstallerViewAction.Close)
                }
            )
        }
    )
}

@Composable
fun unarchivingDialog(): DialogParams = DialogParams(
    icon = DialogInnerParams(DialogParamsType.IconWorking.id, workingIcon),
    title = DialogInnerParams(DialogParamsType.InstallerUnarchiving.id) {
        Text(stringResource(R.string.unarchive_restoring))
    }
)
