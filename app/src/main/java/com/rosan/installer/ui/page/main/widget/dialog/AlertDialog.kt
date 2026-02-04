package com.rosan.installer.ui.page.main.widget.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.build.RsConfig
import com.rosan.installer.build.model.entity.Manufacturer
import com.rosan.installer.data.app.model.enums.RootImplementation
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.util.help

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

/**
 * A reusable AlertDialog to display detailed information about an exception.
 *
 * @param exception The exception to display.
 * @param onDismissRequest Callback invoked when the user wants to dismiss the dialog.
 * @param onRetry Callback invoked when the user clicks the "Retry" button. Can be null if retry is not applicable.
 * @param title The title of the dialog.
 */
@Composable
fun ErrorDisplayDialog(
    exception: Throwable,
    onDismissRequest: () -> Unit,
    onRetry: (() -> Unit)? = null,
    title: String
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onErrorContainer) {
                LazyColumn(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(exception.help(), fontWeight = FontWeight.Bold)
                    }
                    item {
                        SelectionContainer {
                            Text(
                                if (RsConfig.isDebug) {
                                    exception.stackTraceToString()
                                } else {
                                    exception.message ?: "An unknown error occurred."
                                }.trim()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (onRetry != null) {
                TextButton(onClick = onRetry) {
                    Text(stringResource(R.string.retry))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(/*if (onRetry != null)*/ R.string.cancel/* else R.string.ok)*/))
            }
        }
    )
}

@Composable
fun HideLauncherIconWarningDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (show) {
        val dialogTitle = stringResource(R.string.warning)

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = dialogTitle) },
            text = {
                Column {
                    Text(stringResource(R.string.theme_settings_hide_launcher_icon_warning))
                    if (RsConfig.currentManufacturer == Manufacturer.XIAOMI)
                        Text(stringResource(R.string.theme_settings_hide_launcher_icon_warning_xiaomi))
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(text = stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }
}

/**
 * A standard AlertDialog to select Root Implementation.
 * Used when enabling the "Module Flashing" switch for the first time.
 */
@Composable
fun RootImplementationSelectionDialog(
    currentSelection: RootImplementation,
    onDismiss: () -> Unit,
    onConfirm: (RootImplementation) -> Unit
) {
    // Temporary state for the dialog selection
    val (selectedOption, onOptionSelected) = remember { mutableStateOf(currentSelection) }

    val options = mapOf(
        RootImplementation.Magisk to "Magisk",
        RootImplementation.KernelSU to "KernelSU",
        RootImplementation.APatch to "APatch"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.lab_module_select_root_impl)) },
        text = {
            Column(Modifier.selectableGroup()) {
                options.forEach { (impl, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (impl == selectedOption),
                                onClick = { onOptionSelected(impl) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (impl == selectedOption),
                            onClick = null // null recommended for accessibility with selectable row
                        )
                        Text(
                            text = label,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedOption) }
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * An AlertDialog for adding a new NamedPackage.
 */
@Composable
fun UninstallPackageDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var packageName by remember { mutableStateOf("") }
    val isConfirmEnabled = packageName.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.uninstall_enter_package_name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text(stringResource(R.string.config_package_name)) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(packageName) },
                enabled = isConfirmEnabled
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * A dialog to warn the user about unstable blur effects on Android 11 and below.
 */
@Composable
fun BlurWarningDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = stringResource(R.string.warning)) },
            text = {
                Text(text = stringResource(R.string.theme_settings_use_blur_warning))
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(text = stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }
}