package com.rosan.installer.ui.page.main.installer.dialog

import android.annotation.SuppressLint
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.InstallerViewState
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
    installer: InstallerRepo,
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
    installer: InstallerRepo, viewModel: InstallerViewModel
): DialogParams =
    when (viewModel.state) {
        is InstallerViewState.Ready -> readyDialog(viewModel)
        is InstallerViewState.Resolving -> resolvingDialog(installer, viewModel)
        is InstallerViewState.ResolveFailed -> resolveFailedDialog(installer, viewModel)
        is InstallerViewState.Preparing -> preparingDialog(viewModel)
        is InstallerViewState.Analysing -> analysingDialog(installer, viewModel)
        is InstallerViewState.AnalyseFailed -> analyseFailedDialog(installer, viewModel)
        is InstallerViewState.InstallChoice -> installChoiceDialog(installer, viewModel)
        is InstallerViewState.InstallPrepare -> installPrepareDialog(installer, viewModel)
        is InstallerViewState.InstallExtendedMenu -> installExtendedMenuDialog(installer, viewModel)
        is InstallerViewState.InstallExtendedSubMenu -> installExtendedMenuSubMenuDialog(installer, viewModel)
        is InstallerViewState.Installing -> installingDialog(installer, viewModel)
        is InstallerViewState.InstallSuccess -> installSuccessDialog(installer, viewModel)
        is InstallerViewState.InstallFailed -> installFailedDialog(installer, viewModel)
        is InstallerViewState.InstallCompleted -> installCompletedDialog(
            installer,
            viewModel,
            (viewModel.state as InstallerViewState.InstallCompleted).results
        )

        is InstallerViewState.InstallConfirm -> installConfirmDialog(viewModel)
        is InstallerViewState.InstallRetryDowngradeUsingUninstall -> installingDialog(installer, viewModel)
        is InstallerViewState.UninstallReady -> uninstallReadyDialog(viewModel)
        is InstallerViewState.UninstallSuccess -> uninstallSuccessDialog(viewModel)
        is InstallerViewState.UninstallFailed -> uninstallFailedDialog(installer, viewModel)
        is InstallerViewState.Uninstalling -> uninstallingDialog(installer, viewModel)
        is InstallerViewState.UninstallResolveFailed -> uninstallFailedDialog(installer, viewModel)
        // when is exhaustive, so no need to handle the else case
        else -> readyDialog(viewModel)
    }