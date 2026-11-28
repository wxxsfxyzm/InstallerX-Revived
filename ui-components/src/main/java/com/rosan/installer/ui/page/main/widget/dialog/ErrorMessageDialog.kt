package com.rosan.installer.ui.page.main.widget.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ErrorMessageDialog(
    title: String,
    message: String,
    details: String,
    dismissLabel: String,
    retryLabel: String,
    onDismissRequest: () -> Unit,
    onRetry: (() -> Unit)? = null,
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
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Text(message, fontWeight = FontWeight.Bold)
                    }
                    item {
                        SelectionContainer {
                            Text(details)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (onRetry != null) {
                TextButton(onClick = onRetry) {
                    Text(retryLabel)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(dismissLabel)
            }
        },
    )
}
