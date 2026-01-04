package com.rosan.installer.ui.page.main.widget.setting

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

@Composable
fun BaseWidget(
    icon: ImageVector? = null,
    iconPlaceholder: Boolean = true,
    title: String,
    description: String? = null,
    descriptionColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    enabled: Boolean = true,
    isError: Boolean = false,
    onClick: () -> Unit = {},
    hapticFeedbackType: HapticFeedbackType = HapticFeedbackType.ContextClick,
    foreContent: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val alpha = if (enabled) 1f else 0.38f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClick = {
                    haptic.performHapticFeedback(hapticFeedbackType)
                    onClick()
                }
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (icon != null)
            Icon(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.CenterVertically),
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
            )
        if (icon == null && iconPlaceholder)
            Spacer(modifier = Modifier.size(24.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
        ) {
            Column {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                    style = MaterialTheme.typography.titleMedium
                )
                description?.let {
                    val color = if (isError) MaterialTheme.colorScheme.error
                    else descriptionColor
                    Text(
                        text = it,
                        color = color.copy(alpha = alpha),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Box(Modifier.alpha(alpha)) {
                foreContent()
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .alpha(alpha)
        ) {
            content()
        }
    }
}