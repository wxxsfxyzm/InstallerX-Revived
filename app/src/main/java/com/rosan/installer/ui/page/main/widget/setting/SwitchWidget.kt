package com.rosan.installer.ui.page.main.widget.setting

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
    // Create a local state to drive the Switch's visual animation.
    // It always starts as 'false' to ensure the Switch is composed in the "off" state.
    // var visualState by remember { mutableStateOf(false) }

    // Use LaunchedEffect to synchronize the visual state with the authoritative state.
    // This runs when the composable first appears or when the 'checked' state changes.
    // The change from the initial 'false' to a 'true' authoritative state will trigger the animation.
    /*LaunchedEffect(checked) {
        visualState = checked
    }*/

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