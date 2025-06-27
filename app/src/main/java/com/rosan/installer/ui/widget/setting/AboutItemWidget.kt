package com.rosan.installer.ui.widget.setting

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

// TODO extend BaseWidget to support more features like icon size, text styles, etc.
@Composable
fun SettingsAboutItemWidget(
    context: Context,
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    imageContentDescription: String? = null,
    headlineContentText: String,
    supportingContentText: String? = null,
    onClick: () -> Unit
) {
    val vibrator = context.getSystemService(Vibrator::class.java)
    ListItem(
        leadingContent = {
            Icon(
                imageVector = imageVector,
                contentDescription = imageContentDescription,
                modifier = modifier
            )
        },
        headlineContent = { Text(text = headlineContentText) },
        supportingContent = { supportingContentText?.let { Text(text = it) } },
        modifier = Modifier.clickable {
            onClick()
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        }
    )
}