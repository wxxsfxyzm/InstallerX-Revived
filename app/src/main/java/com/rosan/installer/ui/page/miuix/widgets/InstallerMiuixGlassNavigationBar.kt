// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.widgets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import com.rosan.installer.ui.theme.InstallerTheme
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

@Composable
internal fun InstallerMiuixGlassNavigationBar(
    items: List<NavigationItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val labelSize = with(density) {
        if (fontScale >= 1.6f) {
            GlassNavigationBarDefaults.LargeLabelSize.toSp()
        } else {
            GlassNavigationBarDefaults.LabelSize.toSp()
        }
    }
    val labelStyle = MiuixTheme.textStyles.footnote2.copy(fontSize = labelSize)

    val showAction = trailingAction != null
    // Mirror the library's reserved-space animation for label measurement only. The native
    // trailingAction still owns placement, scale, alpha, input handling and shadow outsets.
    val actionProgress = animateFloatAsState(
        targetValue = if (showAction) 1f else 0f,
        animationSpec = GlassMotion.navShowHide(),
        label = "installerNavigationLabelSpace",
    )
    BoxWithConstraints(modifier = modifier) {
        val actionWidth = with(density) {
            val fraction = actionProgress.value.coerceIn(0f, 1f)
            (GlassNavigationBarDefaults.Height.roundToPx() * fraction).toInt() +
                (GlassNavigationBarDefaults.ActionSpacing.toPx() * fraction).toInt()
        }
        val labelWidth = with(density) {
            val contentWidth = constraints.maxWidth - actionWidth -
                GlassNavigationBarDefaults.ContentPaddingHorizontal.roundToPx() * 2
            (contentWidth / items.size.coerceAtLeast(1) - GlassNavigationBarDefaults.IndicatorOverhang.roundToPx() * 2 - 1)
                .coerceAtLeast(0)
        }
        val glassItems = remember(items, labelWidth, labelStyle, textMeasurer) {
            items.map { item ->
                val label = item.label.replace(Regex("[\\r\\n\\u0085\\u2028\\u2029]"), " ")
                fun measure(text: String) = textMeasurer.measure(
                    text = text,
                    style = labelStyle,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                    maxLines = 1,
                    constraints = Constraints(maxWidth = labelWidth),
                )
                val layout = measure(label)
                val fittedLabel = if (labelWidth == 0) {
                    ""
                } else if (layout.isLineEllipsized(0)) {
                    var prefix = label.take(layout.getLineEnd(0, visibleEnd = true)).trimEnd()
                    // Validate the actual string passed to the library, which allows wrapping.
                    // The appended ellipsis must fit too, even with font fallback or shaping.
                    while (prefix.isNotEmpty() && measure("$prefix…").hasVisualOverflow) {
                        prefix = prefix.substring(0, prefix.offsetByCodePoints(prefix.length, -1)).trimEnd()
                    }
                    if (measure("$prefix…").hasVisualOverflow) "" else "$prefix…"
                } else {
                    label
                }
                GlassNavigationItem(
                    icon = item.icon,
                    label = fittedLabel,
                    contentDescription = item.label,
                )
            }
        }

        GlassNavigationBar(
            items = glassItems,
            selectedIndex = selectedIndex,
            onSelect = onSelect,
            backdrop = backdrop,
            defaultWindowInsetsPadding = false,
            style = GlassDefaults.style(InstallerTheme.isDark),
            trailingAction = trailingAction.takeIf { showAction },
        )
    }
}

@Composable
internal fun InstallerMiuixGlassNavigationAction(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    GlassIconButton(
        onClick = onClick,
        backdrop = backdrop,
        modifier = modifier,
        size = GlassNavigationBarDefaults.Height,
        style = GlassDefaults.style(InstallerTheme.isDark),
        shadow = GlassShadows.Float,
        content = content,
    )
}
