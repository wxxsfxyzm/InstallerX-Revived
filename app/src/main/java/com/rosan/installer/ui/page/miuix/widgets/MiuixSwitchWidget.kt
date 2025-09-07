package com.rosan.installer.ui.page.miuix.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Switch

@Composable
fun MiuixSwitchWidget(
    icon: ImageVector? = null,
    title: String,
    description: String? = null,
    enabled: Boolean = true,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
    // Note: The 'isError' parameter is removed as it's not supported by the standard BasicComponent.
) {
    // This action makes the entire row clickable to toggle the switch.
    val toggleAction = {
        if (enabled) {
            onCheckedChange(!checked)
        }
    }

    BasicComponent(
        title = title,
        summary = description,
        enabled = enabled,
        onClick = toggleAction,
        rightActions = {
            // Place the Switch component at the end of the row.
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    )
}