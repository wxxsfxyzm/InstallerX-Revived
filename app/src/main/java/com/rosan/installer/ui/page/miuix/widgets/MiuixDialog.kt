package com.rosan.installer.ui.page.miuix.widgets

import android.app.ProgressDialog.show
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.build.RsConfig
import com.rosan.installer.util.help
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

/**
 * A reusable AlertDialog to display detailed information about an exception.
 *
 * @param exception The exception to display.
 * @param onDismissRequest Callback invoked when the user wants to dismiss the dialog.
 * @param onRetry Callback invoked when the user clicks the "Retry" button. Can be null if retry is not applicable.
 * @param title The title of the dialog.
 */
@Composable
fun MiuixErrorDisplayDialog(
    exception: Throwable,
    onDismissRequest: () -> Unit,
    onRetry: (() -> Unit)?,
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
            if (onRetry != null)
                TextButton(
                    onClick = onRetry,
                    text = stringResource(R.string.retry)
                )
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                text = stringResource(R.string.cancel)
            )
        }
    )
}