// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.widget.chip

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.rosan.installer.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CapsuleTag(
    modifier: Modifier = Modifier,
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
    contentColor: Color = MaterialTheme.colorScheme.primary,
    style: TextStyle = MaterialTheme.typography.labelMediumEmphasized
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 24.dp)
            .clip(CircleShape)
            .background(containerColor)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            style = style.copy(
                platformStyle = PlatformTextStyle(
                    includeFontPadding = false
                )
            )
        )
    }
}

// Data model to hold both short tag and full description
data class NoticeModel(
    val shortLabel: String,      // Displayed on the chip
    val fullDescription: String, // Displayed in the dialog
    val color: Color             // Color of the chip
)

@Composable
fun InstallInfoChipGroup(
    modifier: Modifier = Modifier,
    notices: List<NoticeModel>
) {
    // State to track which notice is currently being viewed
    var selectedNotice by remember { mutableStateOf<NoticeModel?>(null) }

    if (notices.isEmpty()) return

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        notices.forEach { item ->
            CapsuleTag(
                text = item.shortLabel,
                contentColor = item.color,
                // Make background lighter for better contrast
                containerColor = item.color.copy(alpha = 0.2f),
                modifier = Modifier
                    .clip(CircleShape) // Ensure ripple is circular
                    .clickable { selectedNotice = item }
            )
        }
    }

    // Dialog handling
    selectedNotice?.let { notice ->
        AlertDialog(
            onDismissRequest = { selectedNotice = null },
            confirmButton = {
                TextButton(onClick = { selectedNotice = null }) {
                    Text(stringResource(R.string.confirm)) // Or use a common "OK" string
                }
            },
            title = {
                Text(
                    text = notice.shortLabel,
                    color = notice.color,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = notice.fullDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                )
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}
