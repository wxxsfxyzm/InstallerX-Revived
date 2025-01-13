package com.rosan.installer.ui.page.installer.dialog

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.installer.dialog.inner.*

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
): DialogParams {
    return when (viewModel.state) {
        is DialogViewState.Ready -> readyDialog(installer, viewModel)
        is DialogViewState.Resolving -> resolvingDialog(installer, viewModel)
        is DialogViewState.ResolveFailed -> resolveFailedDialog(installer, viewModel)
        is DialogViewState.Analysing -> analysingDialog(installer, viewModel)
        is DialogViewState.AnalyseFailed -> analyseFailedDialog(installer, viewModel)
        is DialogViewState.InstallChoice -> installChoiceDialog(installer, viewModel)
        is DialogViewState.InstallPrepare -> installPrepareDialog(installer, viewModel)
        is DialogViewState.Installing -> installingDialog(installer, viewModel)
        is DialogViewState.InstallSuccess -> installSuccessDialog(installer, viewModel)
        is DialogViewState.InstallFailed -> installFailedDialog(installer, viewModel)
        else -> readyDialog(installer, viewModel)
    }
}