package com.rosan.installer.ui.page.miuix.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.build.Manufacturer
import com.rosan.installer.build.RsConfig
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.extra.SuperDialog

/**
 * A dialog to confirm an action, dynamically showing specific errors or a generic message.
 * Refactored to use SuperDialog.
 *
 * @param show Controls the visibility of the dialog.
 * @param onDismiss Request to close the dialog.
 * @param onConfirm Request to perform the confirm action (e.g., discard and exit).
 * @param errorMessages A list of specific error messages to display. If empty, a generic message is shown.
 */
@Composable
fun MiuixUnsavedChangesDialog(
    showState: MutableState<Boolean>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    errorMessages: List<String>
) {
    val hasSpecificErrors = errorMessages.isNotEmpty()

    // Determine the title based on whether there are specific errors.
    val dialogTitle = if (hasSpecificErrors) {
        stringResource(R.string.config_dialog_title_invalid)
    } else {
        stringResource(R.string.config_dialog_title_unsaved_changes)
    }

    // Call SuperDialog instead of AlertDialog
    SuperDialog(
        // SuperDialog expects a MutableState. We create a temporary one that resets when 'show' changes.
        // onDismiss callback will trigger recomposition by changing the external state that controls 'show'.
        show = showState,
        onDismissRequest = onDismiss,
        title = dialogTitle,
        content = {
            // Reconstruct content: text body + action buttons
            Column {
                // Body content (errors or generic message)
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

                Spacer(modifier = Modifier.height(24.dp)) // Add spacing before buttons

                // Button Row, aligned to the end.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Miuix TextButton for dismiss action
                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss,
                        text = stringResource(R.string.back)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Miuix TextButton for confirm action with primary color styling
                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = onConfirm,
                        text = stringResource(R.string.discard), // Use text parameter directly
                        colors = ButtonDefaults.textButtonColorsPrimary() // Apply primary color style
                    )
                }
            }
        }
    )
}

@Composable
fun MiuixHideLauncherIconWarningDialog(
    showState: MutableState<Boolean>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    SuperDialog(
        show = showState,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.warning),
        content = {
            // Custom content layout with body text and action buttons
            Column {
                // Warning message
                Text(stringResource(R.string.theme_settings_hide_launcher_icon_warning))
                if (RsConfig.currentManufacturer == Manufacturer.XIAOMI)
                    Text(stringResource(R.string.theme_settings_hide_launcher_icon_warning_xiaomi))
                Spacer(modifier = Modifier.height(24.dp)) // Spacing before buttons

                // Action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Dismiss button
                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss,
                        text = stringResource(R.string.cancel)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Confirm button with primary color styling
                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = onConfirm,
                        text = stringResource(R.string.confirm),
                        colors = ButtonDefaults.textButtonColorsPrimary() // Apply primary color style
                    )
                }
            }
        }
    )
}
