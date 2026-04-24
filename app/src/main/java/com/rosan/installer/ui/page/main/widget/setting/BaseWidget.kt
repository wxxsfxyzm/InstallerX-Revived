// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.ui.page.main.widget.setting

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.rosan.installer.ui.theme.CornerRadius

/**
 * A base widget component designed for setting items and list entries.
 * It follows Material Design 3 guidelines with support for icons, headlines, supporting text,
 * and custom trailing content.
 *
 * @param modifier The [Modifier] to be applied to the widget.
 * @param icon The [ImageVector] to be displayed at the start of the widget.
 * @param iconColor The color applied to the [icon].
 * @param iconPlaceholder If true, maintains a consistent leading space even when [icon] is null.
 * @param title The primary headline text of the widget.
 * @param description Optional supporting text displayed below the title.
 * @param descriptionColor The color applied to the [description] text.
 * @param enabled Controls the enabled state of the widget and its interactivity.
 * @param isError If true, applies the error color to the description text.
 * @param selected If true, highlights the widget with a primary container background.
 * @param onClick Callback to be invoked when the widget is clicked. If null, the widget is not clickable.
 * @param clickHaptic The type of haptic feedback to perform on click. Set to null to disable.
 * @param foreContent A composable slot for content displayed alongside/over the headline.
 * @param content A composable slot for trailing content (e.g., switches, checkboxes, or arrows).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BaseWidget(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconColor: Color? = null,
    iconPlaceholder: Boolean = true,
    title: String,
    description: String? = null,
    descriptionColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    enabled: Boolean = true,
    isError: Boolean = false,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    clickHaptic: HapticFeedbackType? = HapticFeedbackType.VirtualKey,
    foreContent: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    val alpha = if (enabled) 1f else 0.38f

    // Determine if the widget is meant to be interacted with.
    val isClickable = onClick != null

    // Calculate dynamic internal padding based on fontScale to maintain visual rhythm
    val density = LocalDensity.current
    val dynamicInternalPadding = (4 * density.fontScale).dp

    // Read the shape provided by the parent SplicedColumnGroup.
    // If not provided (used outside the group), it will use the fallback.
    val baseShape = LocalSegmentedItemShape.current

    val backgroundColor = if (selected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceBright

    val baseContentColor = if (selected) MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.primaryContainer)
    else MaterialTheme.colorScheme.onSurface

    val resolvedIconColor = iconColor
        ?: if (selected) {
            baseContentColor
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    val finalDescriptionColor = when {
        isError -> MaterialTheme.colorScheme.error
        descriptionColor == MaterialTheme.colorScheme.onSurfaceVariant -> baseContentColor.copy(alpha = 0.7f)
        else -> descriptionColor
    }

    val colors = ListItemDefaults.colors(
        containerColor = backgroundColor,
        contentColor = baseContentColor,
        leadingContentColor = resolvedIconColor,
        trailingContentColor = resolvedIconColor,
        supportingContentColor = finalDescriptionColor,
        disabledContainerColor = backgroundColor,
        // Multiply the original alpha with the disabled alpha to preserve relative opacity
        disabledContentColor = baseContentColor.copy(alpha = baseContentColor.alpha * alpha),
        disabledLeadingContentColor = resolvedIconColor.copy(alpha = resolvedIconColor.alpha * alpha),
        disabledTrailingContentColor = baseContentColor.copy(alpha = baseContentColor.alpha * alpha),
        disabledSupportingContentColor = finalDescriptionColor.copy(alpha = finalDescriptionColor.alpha * alpha)
    )

    // Configure the shapes for morphing interactions
    val shapes = ListItemDefaults.shapes(
        shape = baseShape,
        pressedShape = RoundedCornerShape(CornerRadius),
        selectedShape = baseShape,
        focusedShape = baseShape,
        hoveredShape = baseShape
    )

    ListItem(
        modifier = modifier.fillMaxWidth(),
        onClick = {
            // Trigger haptic feedback only if a constant is provided
            onClick?.let { action ->
                clickHaptic?.let { haptic.performHapticFeedback(it) }
                action.invoke()
            }
        },
        enabled = enabled && isClickable,
        colors = colors,
        shapes = shapes,
        // Override the default alignment logic to force vertical centering even for multi-line items.
        // This bypasses the InteractiveListVerticalAlignmentBreakpoint in M3 source.
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = if (icon != null || iconPlaceholder) {
            {
                // Ensure the icon slot itself is centered to align perfectly with the trailing slot
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (enabled) resolvedIconColor else colors.leadingContentColor
                        )
                    } else {
                        // Maintain consistent width when icon is null but placeholder is enabled
                        Spacer(modifier = Modifier.size(24.dp))
                    }
                }
            }
        } else null,
        supportingContent = description?.let {
            {
                Text(
                    text = it,
                    // Apply dynamic bottom padding within the container
                    modifier = Modifier.padding(bottom = dynamicInternalPadding)
                )
            }
        },
        trailingContent = {
            // Explicitly center-align trailing content box
            Box(
                modifier = Modifier.alpha(alpha),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    ) {
        // Headline content wrapper with dynamic vertical margins
        Box(
            modifier = Modifier.padding(
                top = dynamicInternalPadding,
                // Add bottom padding only when there is no supporting content to keep vertical symmetry
                bottom = if (description == null) dynamicInternalPadding else 0.dp
            )
        ) {
            Text(text = title)
            Box(Modifier.alpha(alpha)) {
                foreContent()
            }
        }
    }
}
