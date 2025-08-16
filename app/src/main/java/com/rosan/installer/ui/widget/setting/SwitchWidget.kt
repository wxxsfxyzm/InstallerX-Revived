package com.rosan.installer.ui.widget.setting

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

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
    val toggleAction = {
        if (enabled) {
            onCheckedChange(!checked)
        }
    }

    BaseWidget(
        icon = icon,
        title = title,
        enabled = enabled,
        isError = isError,
        onClick = toggleAction,
        hapticFeedbackType = HapticFeedbackType.ToggleOn,
        description = description
    ) {
        Switch(
            enabled = enabled,
            checked = checked,
            onCheckedChange = null
        )
    }
}