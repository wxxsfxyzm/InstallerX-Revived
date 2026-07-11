// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.installer.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppIcons
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun AdaptiveInfoRow(
    @StringRes labelResId: Int,
    newValue: String,
    oldValue: String?,
    isUninstalled: Boolean = false,
    isArchived: Boolean = false,
    hideIdenticalComparison: Boolean = false
) {
    val showComparison = oldValue != null && (!hideIdenticalComparison || newValue != oldValue)
    val oldTextContent = when {
        isArchived -> stringResource(R.string.old_version_archived)
        isUninstalled -> if (oldValue.isNullOrEmpty()) stringResource(R.string.old_version_uninstalled) else oldValue
        else -> oldValue.orEmpty()
    }

    Layout(
        content = {
            // Index 0: Label
            Text(
                text = stringResource(labelResId),
                style = MiuixTheme.textStyles.body2,
                fontWeight = FontWeight.SemiBold
            )

            if (showComparison) {
                // Index 1: Old text
                Text(
                    text = oldTextContent,
                    style = MiuixTheme.textStyles.body2,
                    textAlign = TextAlign.End
                )
                // Index 2: Arrow
                Icon(
                    imageVector = AppIcons.ArrowIndicator,
                    contentDescription = "to",
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(16.dp)
                )
                // Index 3: New text
                Text(
                    text = newValue,
                    style = MiuixTheme.textStyles.body2,
                    textAlign = TextAlign.End
                )
            } else {
                // Index 1: Single text when no comparison
                Text(
                    text = newValue,
                    style = MiuixTheme.textStyles.body2,
                    textAlign = TextAlign.End
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) { measurables, constraints ->
        val spacing = 16.dp.roundToPx()

        // Measure label exactly once
        val labelPlaceable = measurables[0].measure(Constraints(minWidth = 0, maxWidth = constraints.maxWidth))

        if (showComparison) {
            val oldMeasurable = measurables[1]
            val arrowMeasurable = measurables[2]
            val newMeasurable = measurables[3]

            // Use Intrinsic measurements to check required width WITHOUT calling measure() multiple times
            val oldMaxWidthReq = oldMeasurable.maxIntrinsicWidth(constraints.maxHeight)
            val arrowMaxWidthReq = arrowMeasurable.maxIntrinsicWidth(constraints.maxHeight)
            val newMaxWidthReq = newMeasurable.maxIntrinsicWidth(constraints.maxHeight)

            val totalSingleLineWidth = labelPlaceable.width + spacing + oldMaxWidthReq + arrowMaxWidthReq + newMaxWidthReq

            if (totalSingleLineWidth <= constraints.maxWidth) {
                // Single line mode: Space is sufficient. We can safely measure them now.
                val oldPlaceable = oldMeasurable.measure(Constraints(minWidth = 0, maxWidth = oldMaxWidthReq))
                val arrowPlaceable = arrowMeasurable.measure(Constraints(minWidth = 0, maxWidth = arrowMaxWidthReq))
                val newPlaceable = newMeasurable.measure(Constraints(minWidth = 0, maxWidth = newMaxWidthReq))

                val height = maxOf(labelPlaceable.height, oldPlaceable.height, arrowPlaceable.height, newPlaceable.height)

                layout(constraints.maxWidth, height) {
                    labelPlaceable.placeRelative(
                        x = 0,
                        y = Alignment.CenterVertically.align(labelPlaceable.height, height)
                    )

                    var currentX = constraints.maxWidth

                    currentX -= newPlaceable.width
                    newPlaceable.placeRelative(
                        x = currentX,
                        y = Alignment.CenterVertically.align(newPlaceable.height, height)
                    )

                    currentX -= arrowPlaceable.width
                    arrowPlaceable.placeRelative(
                        x = currentX,
                        y = Alignment.CenterVertically.align(arrowPlaceable.height, height)
                    )

                    currentX -= oldPlaceable.width
                    oldPlaceable.placeRelative(
                        x = currentX,
                        y = Alignment.CenterVertically.align(oldPlaceable.height, height)
                    )
                }
            } else {
                // Stacked mode: Space is insufficient. Two rows.

                // Line 1: Old text shares the row with the Label
                val oldMaxWidth = maxOf(0, constraints.maxWidth - labelPlaceable.width - spacing)
                val oldPlaceable = oldMeasurable.measure(Constraints(minWidth = 0, maxWidth = oldMaxWidth))

                // Line 2: New text takes full width minus the arrow
                val arrowPlaceable = arrowMeasurable.measure(Constraints())
                val newMaxWidth = maxOf(0, constraints.maxWidth - arrowPlaceable.width)
                val newPlaceable = newMeasurable.measure(Constraints(minWidth = 0, maxWidth = newMaxWidth))

                val verticalSpacing = 4.dp.roundToPx()
                val line1Height = maxOf(labelPlaceable.height, oldPlaceable.height)
                val line2Height = maxOf(arrowPlaceable.height, newPlaceable.height)
                val totalHeight = line1Height + verticalSpacing + line2Height

                layout(constraints.maxWidth, totalHeight) {
                    // Place Line 1
                    labelPlaceable.placeRelative(
                        x = 0,
                        y = Alignment.CenterVertically.align(labelPlaceable.height, line1Height)
                    )
                    oldPlaceable.placeRelative(
                        x = constraints.maxWidth - oldPlaceable.width,
                        y = Alignment.CenterVertically.align(oldPlaceable.height, line1Height)
                    )

                    // Place Line 2
                    val line2Y = line1Height + verticalSpacing
                    newPlaceable.placeRelative(
                        x = constraints.maxWidth - newPlaceable.width,
                        y = line2Y + Alignment.CenterVertically.align(newPlaceable.height, line2Height)
                    )
                    // Anchor arrow directly to the left of the new text
                    arrowPlaceable.placeRelative(
                        x = constraints.maxWidth - newPlaceable.width - arrowPlaceable.width,
                        y = line2Y + Alignment.CenterVertically.align(arrowPlaceable.height, line2Height)
                    )
                }
            }
        } else {
            // Single value mode
            val valueMaxWidth = maxOf(0, constraints.maxWidth - labelPlaceable.width - spacing)
            val valuePlaceable = measurables[1].measure(Constraints(minWidth = 0, maxWidth = valueMaxWidth))

            val height = maxOf(labelPlaceable.height, valuePlaceable.height)
            layout(constraints.maxWidth, height) {
                labelPlaceable.placeRelative(
                    x = 0,
                    y = Alignment.CenterVertically.align(labelPlaceable.height, height)
                )
                valuePlaceable.placeRelative(
                    x = constraints.maxWidth - valuePlaceable.width,
                    y = Alignment.CenterVertically.align(valuePlaceable.height, height)
                )
            }
        }
    }
}
