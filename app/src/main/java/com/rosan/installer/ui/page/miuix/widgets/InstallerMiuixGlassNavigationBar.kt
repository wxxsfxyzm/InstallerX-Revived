// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.widgets

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
import top.yukonga.miuix.kmp.glass.GlassDefaults
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassNavigationBar
import top.yukonga.miuix.kmp.glass.GlassNavigationBarDefaults
import top.yukonga.miuix.kmp.glass.GlassNavigationItem
import top.yukonga.miuix.kmp.glass.GlassShadows
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

    BoxWithConstraints(modifier = modifier) {
        // The app's trailing action uses the library's standard navigation-action size.
        val actionWidth = with(density) {
            if (trailingAction != null) {
                GlassNavigationBarDefaults.Height.roundToPx() + GlassNavigationBarDefaults.ActionSpacing.roundToPx()
            } else {
                0
            }
        }
        val labelWidth = with(density) {
            val contentWidth = constraints.maxWidth - actionWidth -
                GlassNavigationBarDefaults.ContentPaddingHorizontal.roundToPx() * 2
            (contentWidth / items.size.coerceAtLeast(1) - GlassNavigationBarDefaults.IndicatorOverhang.roundToPx() * 2 - 1)
                .coerceAtLeast(0)
        }
        val glassItems = remember(items, labelWidth, labelStyle, textMeasurer) {
            items.map { item ->
                val label = item.label.replace('\n', ' ').replace('\r', ' ')
                val layout = textMeasurer.measure(
                    text = label,
                    style = labelStyle,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                    maxLines = 1,
                    constraints = Constraints(maxWidth = labelWidth),
                )
                val fittedLabel = when {
                    labelWidth == 0 -> ""
                    layout.isLineEllipsized(0) -> label.take(layout.getLineEnd(0, visibleEnd = true)) + "…"
                    else -> label
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
            trailingAction = trailingAction,
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
