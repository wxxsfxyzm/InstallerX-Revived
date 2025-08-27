package com.rosan.installer.ui.page.installer.dialog.inner

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.installer.dialog.DialogParams
import com.rosan.installer.ui.page.installer.dialog.DialogParamsType
import com.rosan.installer.ui.page.installer.dialog.DialogViewAction
import com.rosan.installer.ui.page.installer.dialog.DialogViewModel

@Composable
fun readyDialog(
    installer: InstallerRepo, viewModel: DialogViewModel
): DialogParams {
    return DialogParams(
        icon = DialogInnerParams(
            DialogParamsType.IconWorking.id, workingIcon
        ), title = DialogInnerParams(
            DialogParamsType.InstallerReady.id,
        ) {
            Text(stringResource(R.string.installer_ready))
        }, buttons = DialogButtons(
            DialogParamsType.ButtonsCancel.id
        ) {
            listOf(DialogButton(stringResource(R.string.cancel)) {
                viewModel.dispatch(DialogViewAction.Close)
            })
        })
}