package com.rosan.installer.ui.widget.setting

import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@Composable
fun SettingsAboutItemWidget(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    imageContentDescription: String? = null,
    headlineContentText: String,
    supportingContentText: String? = null,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
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
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            onClick()
        }
    )
}