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
    modifier: Modifier = Modifier,
    selected: Boolean,
    useHaptic: Boolean = true,
    onClick: () -> Unit,
    label: String,
    icon: ImageVector,
) {
    val haptic = LocalHapticFeedback.current
    FilterChip(
        modifier = modifier,
        selected = selected,
        onClick = {
            if (useHaptic) haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            onClick()
        },
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize),
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
        ),
    )
}
