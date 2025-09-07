package com.rosan.installer.ui.page.main.installer.dialog.inner

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.main.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParamsType
import com.rosan.installer.ui.page.main.installer.dialog.DialogViewAction
import com.rosan.installer.ui.page.main.installer.dialog.DialogViewModel

@Composable
fun analyseFailedDialog(
    installer: InstallerRepo, viewModel: DialogViewModel
): DialogParams {
    return DialogParams(
        icon = DialogInnerParams(
            DialogParamsType.IconPausing.id, pausingIcon
        ), title = DialogInnerParams(
            DialogParamsType.InstallerAnalyseFailed.id
        ) {
            Text(stringResource(R.string.installer_analyse_failed))
        }, text = DialogInnerParams(
            DialogParamsType.InstallerAnalyseFailed.id, { ErrorTextBlock(installer.error) }
        ), buttons = DialogButtons(
            DialogParamsType.ButtonsCancel.id
        ) {
            listOf(DialogButton(stringResource(R.string.cancel)) {
                viewModel.dispatch(DialogViewAction.Close)
            })
        })
}