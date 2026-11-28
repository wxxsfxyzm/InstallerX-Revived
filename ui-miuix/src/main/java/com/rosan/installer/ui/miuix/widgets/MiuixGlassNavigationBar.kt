// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.miuix.widgets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassMotion
import top.yukonga.miuix.kmp.glass.GlassNavigationBar
import top.yukonga.miuix.kmp.glass.GlassNavigationBarDefaults
import top.yukonga.miuix.kmp.glass.GlassNavigationItem
import top.yukonga.miuix.kmp.material.GlassDefaults
import top.yukonga.miuix.kmp.material.GlassShadows
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val LabelLineBreaks = Regex("[\\r\\n\\u0085\\u2028\\u2029]")

@Composable
fun MiuixGlassNavigationBar(
    items: List<NavigationItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: Backdrop,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    if (items.isEmpty()) return

    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val labelSize = with(density) {
        if (fontScale >= 1.6f) GlassNavigationBarDefaults.LargeLabelSize.toSp() else GlassNavigationBarDefaults.LabelSize.toSp()
    }
    val labelStyle = MiuixTheme.textStyles.footnote2.copy(fontSize = labelSize)
    // Match the space reserved by Miuix while its trailing button animates.
    val actionProgress = animateFloatAsState(
        targetValue = if (trailingAction != null) 1f else 0f,
        animationSpec = GlassMotion.navShowHide(),
        label = "glassNavigationLabelSpace",
    )
    BoxWithConstraints(
        modifier = modifier.layout { measurable, constraints ->
            val barConstraints = if (constraints.hasBoundedWidth) {
                val actionWidth = (GlassNavigationBarDefaults.Height + GlassNavigationBarDefaults.ActionSpacing).roundToPx()
                val available = constraints.maxWidth - actionWidth + (10.dp * (items.size - 1) - 6.dp).roundToPx()
                val width = if (available > 400.dp.roundToPx()) {
                    constraints.constrainWidth(344.dp.roundToPx() + actionWidth)
                } else {
                    constraints.maxWidth
                }
                constraints.copy(minWidth = width, maxWidth = width)
            } else {
                constraints
            }
            val placeable = measurable.measure(barConstraints)
            layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
        },
    ) {
        val labelWidth = if (constraints.hasBoundedWidth) {
            with(density) {
                val fraction = actionProgress.value.coerceIn(0f, 1f)
                val actionWidth = (GlassNavigationBarDefaults.Height.roundToPx() * fraction).toInt()
                val actionGap = (GlassNavigationBarDefaults.ActionSpacing.toPx() * fraction).toInt()
                val contentWidth = constraints.maxWidth - actionWidth - actionGap -
                    GlassNavigationBarDefaults.ContentPaddingHorizontal.roundToPx() * 2
                // A weighted slot may be one pixel narrower after Miuix distributes rounding.
                (contentWidth / items.size - GlassNavigationBarDefaults.IndicatorOverhang.roundToPx() * 2 - 1)
                    .coerceAtLeast(0)
            }
        } else {
            Constraints.Infinity
        }
        val glassItems = remember(items, labelWidth, labelStyle, textMeasurer) {
            items.map { item ->
                val label = item.label.replace(LabelLineBreaks, " ")
                fun measure(text: String, overflow: TextOverflow) = textMeasurer.measure(
                    text = text,
                    style = labelStyle,
                    overflow = overflow,
                    softWrap = false,
                    maxLines = 1,
                    constraints = Constraints(maxWidth = labelWidth),
                )

                val fittedLabel = when (labelWidth) {
                    0 -> ""

                    Constraints.Infinity -> label

                    else -> {
                        val layout = measure(label, TextOverflow.Ellipsis)
                        if (layout.isLineEllipsized(0)) {
                            var prefix = label.take(layout.getLineEnd(0, visibleEnd = true)).trimEnd()
                            // Check the final text as well, since the added ellipsis takes space.
                            while (prefix.isNotEmpty() && measure("$prefix…", TextOverflow.Clip).hasVisualOverflow) {
                                prefix = prefix.substring(0, prefix.offsetByCodePoints(prefix.length, -1)).trimEnd()
                            }
                            if (measure("$prefix…", TextOverflow.Clip).hasVisualOverflow) "" else "$prefix…"
                        } else {
                            label
                        }
                    }
                }
                GlassNavigationItem(icon = item.icon, label = fittedLabel, contentDescription = item.label)
            }
        }
        GlassNavigationBar(
            items = glassItems,
            selectedIndex = selectedIndex,
            onSelect = onSelect,
            backdrop = backdrop,
            defaultWindowInsetsPadding = false,
            style = GlassDefaults.style(isDark),
            trailingAction = trailingAction,
        )
    }
}

@Composable
fun MiuixGlassNavigationAction(
    onClick: () -> Unit,
    backdrop: Backdrop,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    GlassIconButton(
        onClick = onClick,
        backdrop = backdrop,
        modifier = modifier,
        size = GlassNavigationBarDefaults.Height,
        style = GlassDefaults.style(isDark),
        shadow = GlassShadows.Float,
        content = content,
    )
}
