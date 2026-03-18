// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer.dialog.inner

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.ui.page.main.installer.InstallerStage
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParamsType

@Composable
fun installConfirmDialog(
    viewModel: InstallerViewModel
): DialogParams {
    val sessionInfo = viewModel.uiState.value.stage as? InstallerStage.InstallConfirm ?: return DialogParams()

    return DialogParams(
        icon = DialogInnerParams(DialogParamsType.InstallerConfirm.id) {
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                if (sessionInfo.appIcon != null) {
                    Image(
                        bitmap = sessionInfo.appIcon.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }
        },
        title = DialogInnerParams(DialogParamsType.InstallerConfirm.id) {
            Text(
                text = sessionInfo.appLabel.toString(),
                textAlign = TextAlign.Center
            )
        },
        text = DialogInnerParams(DialogParamsType.InstallerConfirm.id) {
            Text(
                text = stringResource(R.string.installer_prepare_type_unknown_confirm),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        },
        buttons = dialogButtons(DialogParamsType.InstallerConfirm.id) {
            listOf(
                DialogButton(stringResource(R.string.confirm)) {
                    viewModel.dispatch(InstallerViewAction.ApproveSession(sessionInfo.sessionId, true))
                },
                DialogButton(stringResource(R.string.cancel)) {
                    viewModel.dispatch(InstallerViewAction.ApproveSession(sessionInfo.sessionId, false))
                }
            )
        }
    )
}