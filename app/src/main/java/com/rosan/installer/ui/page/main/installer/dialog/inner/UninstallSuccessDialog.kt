package com.rosan.installer.ui.page.main.installer.dialog.inner

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.rosan.installer.R
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParamsType

/**
 * Displays a confirmation dialog after a successful uninstallation.
 * It shows the information of the app that was just uninstalled and provides an action to close the dialog.
 *
 * This implementation is modeled after InstallSuccessDialog.
 */
@Composable
fun uninstallSuccessDialog(
    viewModel: InstallerViewModel
): DialogParams {
    // Use the shared uninstallInfoDialog to get the base layout with the app's icon, title, and subtitle.
    // Since the app has been uninstalled, there's no target for onTitleExtraClick, so we leave it empty.
    val baseParams = uninstallInfoDialog(
        viewModel = viewModel,
        onTitleExtraClick = {
            // The app is uninstalled, so opening its settings page is not possible.
            // This action is intentionally left blank.
        }
    )

    // Override the text and buttons sections to provide a success message and a finish button.
    return baseParams.copy(
        text = DialogInnerParams(
            DialogParamsType.InstallerUninstallSuccess.id,
        ) {
            // Display a clear success message to the user.
            Text(
                text = stringResource(R.string.uninstall_success_message),
                textAlign = TextAlign.Center
            )
        },
        buttons = DialogButtons(
            DialogParamsType.InstallerUninstallSuccess.id
        ) {
            // The button list contains only a "Finish" button to close the dialog.
            listOf(
                DialogButton(stringResource(R.string.finish)) {
                    viewModel.dispatch(InstallerViewAction.Close)
                }
            )
        }
    )
}