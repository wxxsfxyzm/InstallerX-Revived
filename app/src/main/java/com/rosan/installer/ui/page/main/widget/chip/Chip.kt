package com.rosan.installer.ui.page.main.widget.chip

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Chip(
    selected: Boolean,
    useHaptic: Boolean = true,
    onClick: () -> Unit,
    label: String,
    icon: ImageVector
) {
    val haptic = LocalHapticFeedback.current
    FilterChip(
        selected = selected,
        onClick = {
            if (useHaptic)
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            onClick()
        },
        label = {
            Text(label)
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null, // contentDescription 建议不为 null，而是提供有意义的描述
                modifier = Modifier.size(FilterChipDefaults.IconSize)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            // 指定未选中状态下，前置图标的颜色。
            // onSurfaceVariant 是主题中为次要图标和文本定义的标准颜色。
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
        )
    )
}