// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.widget.setting

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun IntNumberPickerWidget(
    icon: ImageVector? = null,
    title: String,
    description: String? = null,
    enabled: Boolean = true,
    value: Int,
    startInt: Int,
    endInt: Int,
    stepSize: Int = 0, // 0 for continuous dragging, > 0 for discrete snap steps
    showTooltip: Boolean = true, // Toggle for the floating tooltip
    onValueChange: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var lastIntValue by remember(value) { mutableIntStateOf(value) }

    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()

    // Calculate steps. 0 means continuous dragging without snap points.
    val stepsCount = if (stepSize > 0) {
        maxOf(0, (endInt - startInt) / stepSize - 1)
    } else {
        0
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = icon,
                    contentDescription = null,
                )
            } else {
                Spacer(modifier = Modifier.size(24.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium
                )
                if (description != null) {
                    Text(
                        text = description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 56.dp, end = 36.dp)
        ) {
            Slider(
                value = value.toFloat(),
                onValueChange = {
                    val intValue = it.roundToInt()
                    if (intValue != lastIntValue) {
                        if (stepSize == 0) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        else haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        lastIntValue = intValue
                        onValueChange(intValue)
                    }
                },
                valueRange = startInt.toFloat()..endInt.toFloat(),
                steps = stepsCount, // Apply the dynamically calculated steps
                enabled = enabled,
                modifier = Modifier.weight(1f),
                interactionSource = interactionSource,
                // Highly customized thumb slot for seamless visual experience
                thumb = {
                    TooltipSliderThumbDefinitive(
                        interactionSource = interactionSource,
                        enabled = enabled,
                        showTooltip = showTooltip,
                        isDragged = isDragged,
                        currentValue = lastIntValue
                    )
                }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = value.toString(),
                modifier = Modifier.defaultMinSize(minWidth = 36.dp),
                textAlign = TextAlign.End
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
    }
}

@Composable
private fun TooltipSliderThumbDefinitive(
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
    showTooltip: Boolean,
    isDragged: Boolean,
    currentValue: Int
) {
    Box(contentAlignment = Alignment.Center) {
        SliderDefaults.Thumb(
            interactionSource = interactionSource,
            enabled = enabled
        )

        if (showTooltip) {
            Box(
                modifier = Modifier.layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))

                    layout(width = 0, height = 0) {
                        placeable.placeRelative(
                            x = -placeable.width / 2,
                            y = -placeable.height - 16.dp.roundToPx()
                        )
                    }
                }
            ) {
                AnimatedVisibility(
                    visible = isDragged,
                    enter = fadeIn(animationSpec = tween(150)),
                    exit = fadeOut(animationSpec = tween(150))
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.inverseSurface,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = currentValue.toString(),
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
