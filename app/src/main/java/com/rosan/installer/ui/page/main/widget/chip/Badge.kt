// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors

package com.rosan.installer.ui.page.main.widget.chip

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.rosan.installer.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CapsuleTag(
    modifier: Modifier = Modifier,
    text: String,
    backgroundColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
    textColor: Color = MaterialTheme.colorScheme.primary,
    style: TextStyle = MaterialTheme.typography.labelMediumEmphasized
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            style = style
        )
    }
}

// Data model to hold both short tag and full description
data class WarningModel(
    val shortLabel: String,   // Displayed on the chip
    val fullDescription: String, // Displayed in the dialog
    val color: Color
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WarningChipGroup(
    modifier: Modifier = Modifier,
    warnings: List<WarningModel>
) {
    // State to track which warning is currently being viewed
    var selectedWarning by remember { mutableStateOf<WarningModel?>(null) }

    if (warnings.isEmpty()) return

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        warnings.forEach { item ->
            CapsuleTag(
                text = item.shortLabel,
                textColor = item.color,
                // Make background lighter for better contrast
                backgroundColor = item.color.copy(alpha = 0.12f),
                modifier = Modifier
                    .clip(CircleShape) // Ensure ripple is circular
                    .clickable { selectedWarning = item }
            )
        }
    }

    // Dialog handling
    selectedWarning?.let { warning ->
        AlertDialog(
            onDismissRequest = { selectedWarning = null },
            confirmButton = {
                TextButton(onClick = { selectedWarning = null }) {
                    Text(stringResource(R.string.confirm)) // Or use a common "OK" string
                }
            },
            title = {
                Text(
                    text = warning.shortLabel,
                    color = warning.color,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = warning.fullDescription,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}