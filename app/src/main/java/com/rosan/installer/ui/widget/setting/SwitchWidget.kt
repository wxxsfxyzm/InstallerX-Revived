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
    onCheckedChange: (Boolean) -> Unit,
    // --- 新增参数，用于实现更精美的UI ---
    isError: Boolean = false
) {
    val context = LocalContext.current
    val vibrator = context.getSystemService(Vibrator::class.java)

    // 1. 将所有操作逻辑统一到一个地方
    val toggleAction = {
        if (enabled) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            onCheckedChange(!checked)
        }
    }

    BaseWidget(
        icon = icon,
        title = title,
        enabled = enabled,
        isError = isError,
        onClick = toggleAction, // 2. 整个条目点击时，执行统一的逻辑
        description = description
    ) {
        Switch(
            enabled = enabled,
            checked = checked,
            // 4. Switch 的回调现在只负责调用统一逻辑，不再有振动等副作用
            //    为了防止重复触发，更好的做法是设为 null，因为父级已经处理了点击
            onCheckedChange = null
        )
    }
}