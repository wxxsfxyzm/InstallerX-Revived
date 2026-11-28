// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.ui.page.main.widget.card

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun FluidStatusCard(
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    useBlur: Boolean = false,
    onClick: (() -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
    buttons: (@Composable () -> Unit)? = null,
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (useBlur) {
                containerColor.copy(alpha = 0.15f)
            } else {
                containerColor
            },
            contentColor = contentColor,
        ),
        // Disable elevation when semi-transparent to prevent shadow core rendering
        elevation = if (useBlur) {
            CardDefaults.elevatedCardElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                focusedElevation = 0.dp,
                hoveredElevation = 0.dp,
                draggedElevation = 0.dp,
            )
        } else {
            CardDefaults.elevatedCardElevation()
        },
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Apply fluid background in the lower layer
            AnimatedFluidBackground(
                baseColor = containerColor,
                enabled = useBlur,
                modifier = Modifier.matchParentSize(),
            )

            // Original content logic
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = onClick != null, onClick = onClick ?: {})
                    .padding(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (icon != null) {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.secondary) {
                        Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            icon()
                        }
                    }
                }
                if (title != null) {
                    ProvideTextStyle(value = MaterialTheme.typography.titleLarge) {
                        Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            title()
                        }
                    }
                }
                if (content != null) {
                    Box {
                        content()
                    }
                }
                if (buttons != null) {
                    Box {
                        buttons()
                    }
                }
            }
        }
    }
}
