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
import com.rosan.installer.domain.session.model.ConfirmationRequestType
import com.rosan.installer.ui.page.main.installer.InstallerStage
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.dialog.DialogButton
import com.rosan.installer.ui.page.main.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParamsType
import com.rosan.installer.ui.page.main.installer.dialog.dialogButtons

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
            val tipMessage = when {
                sessionInfo.requestType == ConfirmationRequestType.PRE_APPROVAL -> {
                    val initiator = sessionInfo.sourceAppLabel ?: stringResource(R.string.installer_label_unknown)
                    stringResource(R.string.install_confirm_pre_approval_tip, initiator)
                }

                sessionInfo.requestType == ConfirmationRequestType.PERMISSIONS -> {
                    val initiator = sessionInfo.sourceAppLabel ?: stringResource(R.string.installer_label_unknown)
                    stringResource(R.string.install_confirm_permissions_tip, initiator)
                }

                sessionInfo.isOwnershipConflict -> {
                    val owner = sessionInfo.sourceAppLabel ?: stringResource(R.string.installer_label_unknown)
                    stringResource(R.string.install_confirm_question_update_owner_reminder, owner)
                }

                !sessionInfo.isSelfSession -> {
                    val initiator = sessionInfo.sourceAppLabel ?: stringResource(R.string.installer_label_unknown)
                    stringResource(R.string.install_confirm_external_request_tip, initiator)
                }

                else -> stringResource(R.string.installer_prepare_type_unknown_confirm)
            }

            Text(
                text = tipMessage,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        },
        buttons = dialogButtons(DialogParamsType.InstallerConfirm.id) {
            // Change the confirm button text to "Update Anyway" if it's an ownership conflict
            val confirmText = if (sessionInfo.isOwnershipConflict) {
                stringResource(R.string.install_anyway)
            } else if (sessionInfo.requestType == ConfirmationRequestType.PRE_APPROVAL) {
                stringResource(R.string.pre_approve)
            } else {
                stringResource(R.string.confirm)
            }

            listOf(
                DialogButton(confirmText) {
                    viewModel.dispatch(InstallerViewAction.ApproveSession(sessionInfo.sessionId, true))
                },
                DialogButton(stringResource(R.string.cancel)) {
                    viewModel.dispatch(InstallerViewAction.ApproveSession(sessionInfo.sessionId, false))
                }
            )
        }
    )
}
