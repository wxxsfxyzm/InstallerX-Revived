package com.rosan.installer.ui.page.miuix.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.build.RsConfig
import com.rosan.installer.build.model.entity.Manufacturer
import com.rosan.installer.data.app.model.enums.RootImplementation
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.SuperBottomSheet
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.extra.WindowDialog
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * A dialog to confirm an action, dynamically showing specific errors or a generic message.
 * Refactored to use SuperDialog.
 *
 * @param showState A MutableState controlling the visibility of the dialog.
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

@Composable
fun MiuixUpdateDialog(
    showState: MutableState<Boolean>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    SuperDialog(
        show = showState,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.get_update),
        content = {
            Column {
                Card(
                    modifier = Modifier.padding(bottom = 8.dp),
                    colors = CardColors(
                        color = MiuixTheme.colorScheme.secondaryVariant,
                        contentColor = MiuixTheme.colorScheme.onSurface
                    )
                ) {
                    BasicComponent(
                        title = "GitHub",
                        onClick = {
                            uriHandler.openUri("https://github.com/wxxsfxyzm/InstallerX-Revived/releases")
                            onDismiss()
                        },
                        endActions = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_link_icon),
                                contentDescription = null
                            )
                        }
                    )
                    BasicComponent(
                        title = "Telegram",
                        onClick = {
                            uriHandler.openUri("https://t.me/installerx_revived")
                            onDismiss()
                        },
                        endActions = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_link_icon),
                                contentDescription = null
                            )
                        }
                    )
                }
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismiss,
                    text = stringResource(R.string.cancel)
                )
            }
        }
    )
}

/**
 * A miuix-style dialog to confirm the uninstallation of an application.
 *
 * @param showState A MutableState controlling the visibility of the dialog.
 * @param onDismiss Request to close the dialog.
 * @param onConfirm Request to perform the uninstall action.
 * @param keepData Indicates whether user data should be kept during uninstallation.
 */
@Composable
fun MiuixUninstallConfirmationDialog(
    showState: MutableState<Boolean>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    keepData: Boolean
) {
    WindowDialog(
        show = showState,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.suggestion_uninstall_alert_dialog_confirm_action),
        content = {
            Column {
                val message = if (keepData)
                    stringResource(R.string.suggestion_uninstall_alert_dialog_confirm_uninstall_keep_data_message)
                else
                    stringResource(R.string.suggestion_uninstall_alert_dialog_confirm_uninstall_no_data_message)

                Text(text = message)

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss,
                        text = stringResource(R.string.cancel)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = onConfirm,
                        text = stringResource(R.string.confirm),
                        colors = ButtonDefaults.textButtonColors(
                            textColor = MaterialTheme.colorScheme.error
                        )
                    )
                }
            }
        }
    )
}

/**
 * A reusable SuperBottomSheet to display detailed information about an exception.
 *
 * @param showState A MutableState controlling the visibility of the sheet.
 * @param exception The exception to display.
 * @param onDismissRequest Callback invoked when the user wants to dismiss the sheet.
 * @param onRetry Callback invoked when the user clicks the "Retry" button. Can be null if retry is not applicable.
 * @param title The title of the sheet.
 */
@Composable
fun ErrorDisplaySheet(
    showState: MutableState<Boolean>,
    exception: Throwable,
    onDismissRequest: () -> Unit,
    onRetry: (() -> Unit)? = null,
    title: String
) {
    SuperBottomSheet(
        show = showState,
        onDismissRequest = onDismissRequest,
        title = title,
        startAction = {
            MiuixBackButton(
                icon = MiuixIcons.Regular.Close,
                onClick = onDismissRequest
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            MiuixErrorTextBlock(
                error = exception,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onRetry != null) {
                    TextButton(
                        text = stringResource(R.string.cancel),
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        text = stringResource(R.string.retry),
                        onClick = onRetry,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    TextButton(
                        text = stringResource(R.string.close),
                        onClick = onDismissRequest,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/**
 * A dialog for selecting a root implementation before enabling a feature.
 *
 * @param showState MutableState to control the dialog's visibility.
 * @param onDismiss Callback for when the dialog is dismissed or cancel is clicked.
 * @param onConfirm Callback that provides the selected RootImplementation when confirm is clicked.
 */
@Composable
fun MiuixRootImplementationDialog(
    showState: MutableState<Boolean>,
    onDismiss: () -> Unit,
    onConfirm: (RootImplementation) -> Unit,
) {
    val rootImplementations = remember {
        listOf(
            RootImplementation.Magisk,
            RootImplementation.KernelSU,
            RootImplementation.APatch
        )
    }
    val implementationNames = remember {
        mapOf(
            RootImplementation.Magisk to "Magisk",
            RootImplementation.KernelSU to "KernelSU",
            RootImplementation.APatch to "APatch"
        )
    }

    var selectedImpl by remember { mutableStateOf(rootImplementations.first()) }

    SuperDialog(
        show = showState,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.lab_module_select_root_impl),
        insideMargin = DpSize(0.dp, 24.dp),
        content = {
            Column {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    rootImplementations.forEach { impl ->
                        val isSelected = selectedImpl == impl

                        SelectableRow(
                            text = implementationNames[impl] ?: impl.name,
                            isSelected = isSelected,
                            onClick = { selectedImpl = impl }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss,
                        text = stringResource(R.string.cancel)
                    )
                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onConfirm(selectedImpl)
                            onDismiss()
                        },
                        text = stringResource(R.string.confirm),
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                }
            }
        }
    )
}

@Composable
private fun SelectableRow(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) MiuixTheme.colorScheme.tertiaryContainer else Color.Transparent
    val contentColor = if (isSelected) MiuixTheme.colorScheme.onTertiaryContainer else MiuixTheme.colorScheme.onSurface
    val indicatorColor = if (isSelected) MiuixTheme.colorScheme.onTertiaryContainer else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(backgroundColor)
            .padding(horizontal = 28.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            color = contentColor
        )

        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = MiuixIcons.Basic.Check,
            contentDescription = if (isSelected) "Selected" else null,
            tint = indicatorColor
        )
    }
}

@Composable
fun MiuixUninstallPackageDialog(
    showState: MutableState<Boolean>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var packageName by remember { mutableStateOf("") }
    val isConfirmEnabled = packageName.isNotBlank()

    SuperDialog(
        show = showState,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.uninstall_enter_package_name),
        content = {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = stringResource(R.string.config_package_name),
                    useLabelAsPlaceholder = true,
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.cancel),
                        onClick = onDismiss
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.confirm),
                        onClick = {
                            onConfirm(packageName)
                            packageName = ""
                        },
                        enabled = isConfirmEnabled,
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                }
            }
        }
    )
}

/**
 * A miuix-style dialog to warn the user about unstable blur effects on Android 11 and below.
 */
@Composable
fun MiuixBlurWarningDialog(
    showState: MutableState<Boolean>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    SuperDialog(
        show = showState,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.warning),
        content = {
            Column {
                Text(stringResource(R.string.theme_settings_use_blur_warning))

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss,
                        text = stringResource(R.string.cancel)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = onConfirm,
                        text = stringResource(R.string.confirm),
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                }
            }
        }
    )
}