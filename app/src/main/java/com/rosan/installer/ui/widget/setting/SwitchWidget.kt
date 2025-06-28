package com.rosan.installer.ui.widget.setting

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext

@Composable
fun SwitchWidget(
    icon: ImageVector? = null,
    title: String,
    description: String? = null,
    enabled: Boolean = true,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val vibrator = context.getSystemService(Vibrator::class.java)

    BaseWidget(
        icon = icon,
        title = title,
        description = description,
        enabled = enabled,
        onClick = {
            onCheckedChange(!checked)
        }
    ) {
        Switch(
            enabled = enabled,
            checked = checked,
            onCheckedChange = {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                onCheckedChange(!checked)
            }
        )
    }
}