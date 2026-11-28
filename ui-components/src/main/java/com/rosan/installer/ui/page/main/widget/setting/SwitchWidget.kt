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
 * @param modifier The [Modifier] to be applied to the widget.
 * @param icon The [ImageVector] to be displayed at the start of the widget.
 * @param title The primary text displayed in the widget.
 * @param description Optional supporting text displayed below the title.
 * @param enabled Whether the widget is enabled and interactive.
 * @param onClick Optional callback for the main/left area. When omitted, the main area toggles the switch for backwards compatibility.
 * @param checked Whether the switch is currently on or off.
 * @param trailingDivider Whether to display a vertical divider before the switch.
 * @param onCheckedChange Callback to be invoked when the switch state changes.
 * @param isError If true, applies an error state to the widget.
 */
@Composable
fun SwitchWidget(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconPlaceholder: Boolean = true,
    title: String,
    description: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailingDivider: Boolean = false,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
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

    val leftClickAction = if (onClick == null) {
        {
            if (enabled) {
                handleCheckedChange(!checked)
            }
        }
    } else {
        {
            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onClick()
        }
    }

    val trailingClickAction = if (onClick != null || trailingDivider) {
        { handleCheckedChange(!checked) }
    } else {
        null
    }
    val separateClickAreas = onClick != null || trailingDivider

    BaseWidget(
        modifier = modifier.semantics(mergeDescendants = true) {
            role = Role.Switch
            toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
        },
        icon = icon,
        iconPlaceholder = iconPlaceholder,
        title = title,
        enabled = enabled,
        isError = isError,
        onTrailingClick = if (separateClickAreas) null else trailingClickAction,
        trailingDivider = trailingDivider,
        onClick = leftClickAction,
        clickHaptic = null,
        description = description,
    ) { interactionSource ->
        Switch(
            modifier = Modifier.clearAndSetSemantics {},
            enabled = enabled,
            checked = checked,
            interactionSource = interactionSource,
            colors = SwitchDefaults.colors(
                checkedIconColor = MaterialTheme.colorScheme.primary,
                uncheckedIconColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
            thumbContent = {
                Icon(
                    imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            },
            // Use the switch's own touch handling when the left and trailing areas are separate.
            onCheckedChange = if (separateClickAreas) handleCheckedChange else null,
        )
    }
}
