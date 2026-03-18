// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer.dialog

import android.annotation.SuppressLint
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.ui.page.main.installer.InstallerStage
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.dialog.inner.analyseFailedDialog
import com.rosan.installer.ui.page.main.installer.dialog.inner.analysingDialog
import com.rosan.installer.ui.page.main.installer.dialog.inner.installChoiceDialog
import com.rosan.installer.ui.page.main.installer.dialog.inner.installCompletedDialog
import com.rosan.installer.ui.page.main.installer.dialog.inner.installConfirmDialog
import com.rosan.installer.ui.page.main.installer.dialog.inner.installExtendedMenuDialog
import com.rosan.installer.ui.page.main.installer.dialog.inner.installExtendedMenuSubMenuDialog
import com.rosan.installer.ui.page.main.installer.dialog.inner.installFailedDialog
import com.rosan.installer.ui.page.main.installer.dialog.inner.installPrepareDialog
import com.rosan.installer.ui.page.main.installer.dialog.inner.installSuccessDialog
import com.rosan.installer.ui.page.main.installer.dialog.inner.installingDialog
import com.rosan.installer.ui.page.main.installer.dialog.inner.preparingDialog
import com.rosan.installer.ui.page.main.installer.dialog.inner.readyDialog
import com.rosan.installer.ui.page.main.installer.dialog.inner.resolveFailedDialog
import com.rosan.installer.ui.page.main.installer.dialog.inner.resolvingDialog
import com.rosan.installer.ui.page.main.installer.dialog.inner.uninstallFailedDialog
import com.rosan.installer.ui.page.main.installer.dialog.inner.uninstallReadyDialog
import com.rosan.installer.ui.page.main.installer.dialog.inner.uninstallSuccessDialog
import com.rosan.installer.ui.page.main.installer.dialog.inner.uninstallingDialog


// change the content when the id been changed
@SuppressLint("UnusedContentLambdaTargetStateParameter")
fun dialogInnerWidget(
    installer: InstallerSessionRepository,
    params: DialogInnerParams
): @Composable (() -> Unit)? =
    if (params.content == null) null
    else {
        {
            /*AnimatedContent(
                targetState = "${installer.id}_${params.id}"
            ) {
                params.content.invoke()
            }*/params.content.invoke()
        }
    }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun dialogGenerateParams(
    installer: InstallerSessionRepository, viewModel: InstallerViewModel
): DialogParams {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    return when (val stage = uiState.stage) {
        is InstallerStage.Ready -> readyDialog(viewModel)
        is InstallerStage.Resolving -> resolvingDialog(installer, viewModel)
        is InstallerStage.ResolveFailed -> resolveFailedDialog(installer, viewModel)
        is InstallerStage.Preparing -> preparingDialog(viewModel)
        is InstallerStage.Analysing -> analysingDialog(installer, viewModel)
        is InstallerStage.AnalyseFailed -> analyseFailedDialog(installer, viewModel)
        is InstallerStage.InstallChoice -> installChoiceDialog(installer, viewModel)
        is InstallerStage.InstallPrepare -> installPrepareDialog(installer, viewModel)
        is InstallerStage.InstallExtendedMenu -> installExtendedMenuDialog(installer, viewModel)
        is InstallerStage.InstallExtendedSubMenu -> installExtendedMenuSubMenuDialog(installer, viewModel)
        is InstallerStage.Installing -> installingDialog(installer, viewModel)
        is InstallerStage.InstallSuccess -> installSuccessDialog(installer, viewModel)
        is InstallerStage.InstallFailed -> installFailedDialog(installer, viewModel)
        is InstallerStage.InstallCompleted -> installCompletedDialog(installer, viewModel, stage.results)
        is InstallerStage.InstallConfirm -> installConfirmDialog(viewModel)
        is InstallerStage.InstallRetryDowngradeUsingUninstall -> installingDialog(installer, viewModel)
        is InstallerStage.UninstallReady -> uninstallReadyDialog(viewModel)
        is InstallerStage.UninstallSuccess -> uninstallSuccessDialog(viewModel)
        is InstallerStage.UninstallFailed -> uninstallFailedDialog(installer, viewModel)
        is InstallerStage.Uninstalling -> uninstallingDialog(installer, viewModel)
        is InstallerStage.UninstallResolveFailed -> uninstallFailedDialog(installer, viewModel)
        // when is exhaustive, so no need to handle the else case
        else -> readyDialog(viewModel)
    }
}
