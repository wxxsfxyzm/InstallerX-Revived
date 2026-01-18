package com.rosan.installer.ui.activity


import android.content.pm.PackageInstaller
import android.text.format.Formatter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.rosan.installer.data.installer.model.entity.UnarchiveInfo

@Composable
fun UnarchivePage(
    state: UnarchiveUiState,
    onConfirmRestore: (UnarchiveInfo) -> Unit,
    onErrorAction: (UnarchiveUiState.Error) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    when (state) {
        is UnarchiveUiState.Confirmation -> {
            // --- 显示确认恢复弹窗 ---
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Restore app?") },
                text = { Text("Do you want to restore ${state.info.packageName}? Your data will be preserved.") },
                confirmButton = {
                    TextButton(onClick = { onConfirmRestore(state.info) }) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            )
        }

        is UnarchiveUiState.Error -> {
            // --- 显示错误弹窗 ---
            val (title, msg, btnText) = when (state.status) {
                PackageInstaller.UNARCHIVAL_ERROR_INSUFFICIENT_STORAGE -> {
                    val size = Formatter.formatFileSize(context, state.requiredBytes)
                    Triple("Storage full", "Free up $size to restore app.", "Manage Storage")
                }

                PackageInstaller.UNARCHIVAL_ERROR_USER_ACTION_NEEDED -> {
                    Triple("Action required", "Confirmation needed to restore.", "Continue")
                }

                PackageInstaller.UNARCHIVAL_ERROR_NO_CONNECTIVITY -> {
                    Triple("No internet", "Connect to internet to restore.", "OK")
                }

                else -> {
                    Triple("Restore failed", "Could not restore app (Error code: ${state.status})", "OK")
                }
            }

            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(title) },
                text = { Text(msg) },
                confirmButton = {
                    TextButton(onClick = {
                        // 如果是普通错误(Generic/NoNet)，点击OK就关闭
                        // 如果是需要操作的错误(Storage/Action)，点击调用回调处理
                        if (state.status == PackageInstaller.UNARCHIVAL_ERROR_NO_CONNECTIVITY ||
                            state.status == PackageInstaller.UNARCHIVAL_GENERIC_ERROR
                        ) {
                            onDismiss()
                        } else {
                            onErrorAction(state)
                        }
                    }) {
                        Text(btnText)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            )
        }

        UnarchiveUiState.Invalid -> {
            // Should not happen, activity handles it
        }
    }
}