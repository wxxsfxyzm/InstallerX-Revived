// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.config.apply

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * A reusable item widget for displaying app information with a toggle switch.
 * It handles entry animations internally. Icon loading has been hoisted.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ApplyItemWidget(
    modifier: Modifier = Modifier,
    app: ApplyViewApp,
    icon: ImageBitmap?, // Passed from the parent caller
    isApplied: Boolean,
    // Visual customization parameters
    shape: Shape = RoundedCornerShape(8.dp),
    containerColor: Color = Color.Transparent,
    isM3e: Boolean = true, // Controls Switch icons and text styles
    showPackageName: Boolean = true,
    // Callbacks
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    // Manually control the entry animation state.
    val animationState = remember { Animatable(0f) }

    // Trigger the animation once when the item enters the composition.
    LaunchedEffect(Unit) {
        animationState.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 300)
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                // Apply transformations in the draw phase to avoid relayout.
                val progress = animationState.value
                this.alpha = progress
                // Slight vertical slide-in effect (50px -> 0px)
                this.translationY = 50f * (1f - progress)
            }
            .background(containerColor, shape)
            .clip(shape)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = MaterialTheme.colorScheme.primary)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // Render the icon if available, otherwise use a placeholder to avoid layout shifts.
        if (icon != null) {
            Image(
                bitmap = icon,
                modifier = Modifier.size(40.dp),
                contentDescription = null
            )
        } else {
            Box(modifier = Modifier.size(40.dp))
        }

        // --- Text Content ---
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = app.label ?: app.packageName,
                style = if (isM3e)
                    MaterialTheme.typography.titleMediumEmphasized
                else
                    MaterialTheme.typography.titleMedium
            )

            AnimatedVisibility(visible = showPackageName) {
                Text(
                    text = app.packageName,
                    style = if (isM3e)
                        MaterialTheme.typography.bodySmall
                    else
                        MaterialTheme.typography.bodyMedium
                )
            }
        }

        // --- Switch ---
        Switch(
            checked = isApplied,
            onCheckedChange = onToggle,
            thumbContent = if (isM3e) {
                {
                    val iconVector = if (isApplied) Icons.Filled.Check else Icons.Filled.Close
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                }
            } else null
        )
    }
}
