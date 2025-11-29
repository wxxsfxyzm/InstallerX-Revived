package com.rosan.installer.ui.page.main.installer.dialog.inner

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParamsType

/**
 * Displays an error dialog when an uninstallation fails.
 * It shows the app info, the specific error, and provides actions to retry or close.
 *
 * This implementation is modeled after InstallFailedDialog.
 */
@Composable
fun uninstallFailedDialog(
    installer: InstallerRepo,
    viewModel: InstallerViewModel
): DialogParams {
    // Use the shared uninstallInfoDialog to get the base layout.
    // Provide a click handler to open the app's system settings page,
    // which is useful for debugging a failed uninstall.
    val baseParams = uninstallInfoDialog(
        viewModel = viewModel,
        onTitleExtraClick = {}
    )

    // Override the text and buttons sections to display the error and provide relevant actions.
    return baseParams.copy(
        text = DialogInnerParams(
            DialogParamsType.InstallerUninstallFailed.id
        ) {
            // Reuse the ErrorTextBlock to display the exception message from the installer repository.
            // No intelligent suggestions are added here, keeping it focused on displaying the error.
            ErrorTextBlock(installer.error)
        },
        buttons = DialogButtons(
            DialogParamsType.InstallerUninstallFailed.id
        ) {
            listOf(
                // A "Close" button dismisses the dialog.
                DialogButton(stringResource(R.string.close)) {
                    viewModel.dispatch(InstallerViewAction.Close)
                }
            )
        }
    )
}