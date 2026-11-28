// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.widget.card

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ColorSwatch(
    baseColor: Color,
    scheme: ColorScheme?,
    isSelected: Boolean,
    label: String?,
    textStyle: TextStyle,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        // Direct rendering based on available state. Crossfade removed.
        if (scheme != null) {
            // Full Material 3 style (either cached or just loaded)
            FullSwatchContent(scheme, isSelected)
        } else {
            // Minimalistic fallback UI using base color while calculating
            FallbackSwatchContent(baseColor, isSelected)
        }

        if (label != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = label,
                style = textStyle,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FullSwatchContent(scheme: ColorScheme, isSelected: Boolean) {
    // Optimization: Cache alpha variations to save calculation time during recomposition
    val primaryForSwatch = remember(scheme) { scheme.primaryContainer.copy(alpha = 0.9f) }
    val secondaryForSwatch = remember(scheme) { scheme.secondaryContainer.copy(alpha = 0.6f) }
    val tertiaryForSwatch = remember(scheme) { scheme.tertiaryContainer.copy(alpha = 0.9f) }
    val squircleBackgroundColor = remember(scheme) { scheme.primary.copy(alpha = 0.3f) }

    Box(
        modifier = Modifier
            .size(64.dp)
            .background(color = squircleBackgroundColor, shape = RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(color = primaryForSwatch, startAngle = 180f, sweepAngle = 180f, useCenter = true)
                drawArc(color = tertiaryForSwatch, startAngle = 90f, sweepAngle = 90f, useCenter = true)
                drawArc(color = secondaryForSwatch, startAngle = 0f, sweepAngle = 90f, useCenter = true)
            }

            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(scheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = scheme.inversePrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FallbackSwatchContent(baseColor: Color, isSelected: Boolean) {
    // Minimalistic fallback UI
    Box(
        modifier = Modifier
            .size(64.dp)
            .background(color = baseColor.copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(baseColor.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(baseColor),
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}
