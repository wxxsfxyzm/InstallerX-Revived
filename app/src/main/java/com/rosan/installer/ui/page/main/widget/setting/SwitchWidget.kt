// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.ui.page.main.widget.setting

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState

/**
 * A setting widget with a [Switch] trailing content.
 *
 * @param icon The [ImageVector] to be displayed at the start of the widget.
 * @param title The primary text displayed in the widget.
 * @param description Optional supporting text displayed below the title.
 * @param enabled Whether the widget is enabled and interactive.
 * @param checked Whether the switch is currently on or off.
 * @param onCheckedChange Callback to be invoked when the switch state changes.
 * @param isError If true, applies an error state to the widget.
 */
@Composable
fun SwitchWidget(
    icon: ImageVector? = null,
    title: String,
    description: String? = null,
    enabled: Boolean = true,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isError: Boolean = false
) {
    val haptic = LocalHapticFeedback.current

    val handleCheckedChange: (Boolean) -> Unit = { newValue ->
        if (newValue) {
            haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
        }
        onCheckedChange(newValue)
    }

    val rowClickAction = {
        if (enabled) {
            handleCheckedChange(!checked)
        }
    }

    BaseWidget(
        modifier = Modifier.semantics(mergeDescendants = true) {
            role = Role.Switch
            toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
        },
        icon = icon,
        title = title,
        enabled = enabled,
        isError = isError,
        onClick = rowClickAction,
        clickHaptic = null,
        description = description
    ) {
        Switch(
            modifier = Modifier.clearAndSetSemantics {},
            enabled = enabled,
            checked = checked,
            thumbContent = if (checked) {
                {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                }
            } else {
                {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                }
            },
            onCheckedChange = { newValue ->
                handleCheckedChange(newValue)
            }
        )
    }
}
