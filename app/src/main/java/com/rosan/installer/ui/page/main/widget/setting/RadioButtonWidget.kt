// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.widget.setting

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle

// A dedicated widget for radio button settings items
@Composable
fun RadioButtonWidget(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    descriptionStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    icon: ImageVector? = null,
    iconPlaceholder: Boolean = true,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    BaseWidget(
        // Merge semantics and define role/state for screen readers
        modifier = modifier.semantics(mergeDescendants = true) {
            role = Role.RadioButton
            this.selected = selected
        },
        icon = icon,
        iconPlaceholder = iconPlaceholder,
        title = title,
        description = description,
        descriptionStyle = descriptionStyle,
        selected = selected,
        enabled = enabled,
        onClick = onClick
    ) { interactionSource -> // Receive the shared interactionSource
        RadioButton(
            selected = selected,
            // The click event is already handled by BaseWidget's ListItem,
            // passing null here avoids double-triggering ripples
            onClick = null,
            // Clear child semantics to avoid double reading by TalkBack
            modifier = Modifier.clearAndSetSemantics {},
            enabled = enabled,
            // Inherit the dynamic content color provided by BaseWidget
            colors = RadioButtonDefaults.colors(
                selectedColor = LocalContentColor.current
            ),
            // Bind the shared interactionSource for synchronized visual feedback
            interactionSource = interactionSource
        )
    }
}
