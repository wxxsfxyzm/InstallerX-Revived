package com.rosan.installer.ui.page.installer.dialog.inner

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
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
fun installExtendedMenuDialog(
    installer: InstallerRepo, viewModel: DialogViewModel
): DialogParams {
    return DialogParams(
        icon = DialogInnerParams(DialogParamsType.IconMenu.id, menuIcon),
        title = DialogInnerParams(
            DialogParamsType.InstallExtendedMenu.id,
        ) {
            Text("Extended Menu")
        },
        content = DialogInnerParams(DialogParamsType.InstallExtendedMenu.id) {
            LazyColumn {
                item {
                    Text(
                        text = "This is an extended menu for installation options.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                // Add more items here as needed
            }
        },
        buttons = DialogButtons(
            DialogParamsType.InstallChoice.id
        ) {
            listOf(DialogButton(stringResource(R.string.next)) {
                viewModel.dispatch(DialogViewAction.InstallPrepare)
            }, DialogButton(stringResource(R.string.cancel)) {
                viewModel.dispatch(DialogViewAction.Close)
            })
        })
}