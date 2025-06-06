package com.rosan.installer.ui.page.installer.dialog

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.installer.dialog.inner.analyseFailedDialog
import com.rosan.installer.ui.page.installer.dialog.inner.analysingDialog
import com.rosan.installer.ui.page.installer.dialog.inner.installChoiceDialog
import com.rosan.installer.ui.page.installer.dialog.inner.installCompletedDialog
import com.rosan.installer.ui.page.installer.dialog.inner.installExtendedMenuDialog
import com.rosan.installer.ui.page.installer.dialog.inner.installExtendedMenuSubMenuDialog
import com.rosan.installer.ui.page.installer.dialog.inner.installFailedDialog
import com.rosan.installer.ui.page.installer.dialog.inner.installPrepareDialog
import com.rosan.installer.ui.page.installer.dialog.inner.installSuccessDialog
import com.rosan.installer.ui.page.installer.dialog.inner.installingDialog
import com.rosan.installer.ui.page.installer.dialog.inner.preparingDialog
import com.rosan.installer.ui.page.installer.dialog.inner.readyDialog
import com.rosan.installer.ui.page.installer.dialog.inner.resolveFailedDialog
import com.rosan.installer.ui.page.installer.dialog.inner.resolvingDialog
import com.rosan.installer.ui.page.installer.dialog.inner.uninstallFailedDialog
import com.rosan.installer.ui.page.installer.dialog.inner.uninstallReadyDialog
import com.rosan.installer.ui.page.installer.dialog.inner.uninstallSuccessDialog
import com.rosan.installer.ui.page.installer.dialog.inner.uninstallingDialog

// change the content when the id been changed
@SuppressLint("UnusedContentLambdaTargetStateParameter")
fun dialogInnerWidget(
    installer: InstallerRepo,
    params: DialogInnerParams
): @Composable (() -> Unit)? =
    if (params.content == null) null
    else {
        {
            AnimatedContent(
                targetState = "${installer.id}_${params.id}"
            ) {
                params.content.invoke()
            }
        }
    }

@Composable
fun dialogGenerateParams(
    installer: InstallerRepo, viewModel: DialogViewModel
): DialogParams =
    when (viewModel.state) {
        is DialogViewState.Ready -> readyDialog(installer, viewModel)
        is DialogViewState.Resolving -> resolvingDialog(installer, viewModel)
        is DialogViewState.ResolveFailed -> resolveFailedDialog(installer, viewModel)
        is DialogViewState.Preparing -> preparingDialog(installer, viewModel)
        is DialogViewState.Analysing -> analysingDialog(installer, viewModel)
        is DialogViewState.AnalyseFailed -> analyseFailedDialog(installer, viewModel)
        is DialogViewState.InstallChoice -> installChoiceDialog(installer, viewModel)
        is DialogViewState.InstallPrepare -> installPrepareDialog(installer, viewModel)
        is DialogViewState.InstallExtendedMenu -> installExtendedMenuDialog(installer, viewModel)
        is DialogViewState.InstallExtendedSubMenu -> installExtendedMenuSubMenuDialog(installer, viewModel)
        is DialogViewState.Installing -> installingDialog(installer, viewModel)
        is DialogViewState.InstallSuccess -> installSuccessDialog(installer, viewModel)
        is DialogViewState.InstallFailed -> installFailedDialog(installer, viewModel)
        is DialogViewState.InstallCompleted -> installCompletedDialog(
            installer,
            viewModel,
            (viewModel.state as DialogViewState.InstallCompleted).results
        )

        is DialogViewState.InstallRetryDowngradeUsingUninstall -> installingDialog(installer, viewModel)
        is DialogViewState.UninstallReady -> uninstallReadyDialog(viewModel)
        is DialogViewState.UninstallSuccess -> uninstallSuccessDialog(viewModel)
        is DialogViewState.UninstallFailed -> uninstallFailedDialog(installer, viewModel)
        is DialogViewState.Uninstalling -> uninstallingDialog(installer, viewModel)
        is DialogViewState.UninstallResolveFailed -> uninstallFailedDialog(installer, viewModel)
        // when is exhaustive, so no need to handle the else case
        // else -> readyDialog(installer, viewModel)
    }