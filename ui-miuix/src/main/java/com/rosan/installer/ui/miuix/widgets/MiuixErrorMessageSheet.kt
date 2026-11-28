package com.rosan.installer.ui.miuix.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.window.WindowBottomSheet

@Composable
fun MiuixErrorMessageSheet(
    show: Boolean,
    title: String,
    closeIcon: ImageVector,
    closeLabel: String,
    cancelLabel: String,
    retryLabel: String,
    onDismissRequest: () -> Unit,
    closeContentDescription: String = closeLabel,
    onDismissFinished: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    errorContent: @Composable (Modifier) -> Unit,
) {
    WindowBottomSheet(
        show = show,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        title = title,
        startAction = {
            val dismissState = LocalDismissState.current
            MiuixBackButton(
                icon = closeIcon,
                contentDescription = closeContentDescription,
                onClick = { dismissState?.invoke() ?: onDismissRequest() },
            )
        },
    ) {
        val dismissState = LocalDismissState.current
        val dismissSheet = { dismissState?.invoke() ?: onDismissRequest() }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            errorContent(
                Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (onRetry != null) {
                    TextButton(
                        text = cancelLabel,
                        onClick = dismissSheet,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        text = retryLabel,
                        onClick = onRetry,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    TextButton(
                        text = closeLabel,
                        onClick = dismissSheet,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
