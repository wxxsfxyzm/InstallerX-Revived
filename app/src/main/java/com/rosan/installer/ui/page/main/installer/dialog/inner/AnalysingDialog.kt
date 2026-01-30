package com.rosan.installer.ui.page.main.installer.dialog.inner

import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParamsType

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun analysingDialog(
    installer: InstallerRepo, viewModel: InstallerViewModel
): DialogParams {
    return DialogParams(
        icon = DialogInnerParams(
            DialogParamsType.IconWorking.id,
            if (viewModel.viewSettings.uiExpressive) {
                {
                    ContainedLoadingIndicator(
                        indicatorColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                }
            } else workingIcon
        ), title = DialogInnerParams(
            DialogParamsType.InstallerAnalysing.id,
        ) {
            Text(stringResource(R.string.installer_analysing))
        }, buttons = dialogButtons(
            DialogParamsType.ButtonsCancel.id
        ) {
            // disable the cancel button
            emptyList()
            /*listOf(DialogButton(stringResource(R.string.cancel)) {
                viewModel.dispatch(DialogViewAction.Close)
            })*/
        })
}