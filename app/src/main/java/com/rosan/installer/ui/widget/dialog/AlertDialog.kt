package com.rosan.installer.ui.widget.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppIcons

/**
 * A dialog to confirm an action, dynamically showing specific errors or a generic message.
 *
 * @param show Controls the visibility of the dialog.
 * @param onDismiss Request to close the dialog.
 * @param onConfirm Request to perform the confirm action (e.g., discard and exit).
 * @param errorMessages A list of specific error messages to display. If empty, a generic message is shown.
 */
@Composable
fun UnsavedChangesDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    errorMessages: List<String>
) {
    if (show) {
        val hasSpecificErrors = errorMessages.isNotEmpty()

        // Determine the title based on whether there are specific errors.
        val dialogTitle = if (hasSpecificErrors) {
            stringResource(R.string.config_dialog_title_invalid)
        } else {
            stringResource(R.string.config_dialog_title_unsaved_changes)
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = dialogTitle) },
            text = {
                if (hasSpecificErrors) {
                    // If there are errors, display each one on a new line.
                    Column {
                        errorMessages.forEach { message ->
                            Text(text = message)
                        }
                    }
                } else {
                    // Otherwise, show the generic unsaved changes message.
                    Text(text = stringResource(R.string.config_dialog_message_unsaved_changes))
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(text = stringResource(R.string.discard))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.back))
                }
            }
        )
    }
}

@Composable
fun UninstallConfirmationDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    keepData: Boolean
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(AppIcons.Delete, contentDescription = null) },
            title = {
                Text(text = stringResource(R.string.suggestion_uninstall_alert_dialog_confirm_action))
            },
            text = {
                // Placeholder text as requested.
                // In a real app, you would provide a more detailed message.
                val message = if (keepData)
                    stringResource(R.string.suggestion_uninstall_alert_dialog_confirm_uninstall_keep_data_message)
                else
                    stringResource(R.string.suggestion_uninstall_alert_dialog_confirm_uninstall_no_data_message)

                Text(message)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm() // Execute the confirmation action
                        onDismiss() // Close the dialog
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss // Just close the dialog
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}