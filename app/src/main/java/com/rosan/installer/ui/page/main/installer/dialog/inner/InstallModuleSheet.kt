package com.rosan.installer.ui.page.main.installer.dialog.inner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rosan.installer.R
import com.rosan.installer.ui.util.KeyEventBlocker

/**
 * Replicates the logic of Miuix InstallModuleContent using Material 3 components.
 * Features an auto-scrolling log terminal and a bottom action button.
 */
@Composable
fun ModuleInstallSheetContent(
    outputLines: List<String>,
    isFinished: Boolean,
    onReboot: () -> Unit,
    onClose: () -> Unit,
    colorScheme: ColorScheme
) {
    KeyEventBlocker {
        it.key == Key.VolumeDown || it.key == Key.VolumeUp
    }
    val lazyListState = rememberLazyListState()

    // Auto-scroll to the bottom when new lines are added, provided it's not finished yet
    LaunchedEffect(outputLines.size) {
        if (!isFinished && outputLines.isNotEmpty()) {
            lazyListState.animateScrollToItem(index = outputLines.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp), // Bottom padding for navigation bar/visual balance
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Text(
            text = if (isFinished) stringResource(R.string.installer_install_complete)
            else stringResource(R.string.installer_installing_module),
            style = MaterialTheme.typography.titleLarge,
            color = colorScheme.onSurface
        )

        // Terminal Log Container
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 300.dp, max = 500.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorScheme.surfaceContainerHigh
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                items(outputLines) { line ->
                    Text(
                        text = line,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        // Highlight errors in red (error color), standard text in onSurfaceVariant
                        color = if (line.startsWith("ERROR:")) colorScheme.error else colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Action Button
        if (isFinished) {
            Column {
                Button(
                    onClick = onReboot,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.reboot))
                }
                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        } else {
            Button(
                enabled = false, // Disabled while installing
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(stringResource(R.string.installer_installing))
                }
            }
        }
    }
}