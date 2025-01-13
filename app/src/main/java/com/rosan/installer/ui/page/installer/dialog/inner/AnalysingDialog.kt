package com.rosan.installer.ui.page.installer.dialog.inner

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.installer.dialog.*

@Composable
fun analysingDialog(
    installer: InstallerRepo, viewModel: DialogViewModel
): DialogParams {
    return DialogParams(icon = DialogInnerParams(
        DialogParamsType.IconWorking.id, workingIcon
    ), title = DialogInnerParams(
        DialogParamsType.InstallerAnalysing.id,
    ) {
        Text(stringResource(R.string.installer_analysing))
    }, buttons = DialogButtons(
        DialogParamsType.ButtonsCancel.id
    ) {
        listOf(DialogButton(stringResource(R.string.cancel)) {
            viewModel.dispatch(DialogViewAction.Close)
        })
    })
}