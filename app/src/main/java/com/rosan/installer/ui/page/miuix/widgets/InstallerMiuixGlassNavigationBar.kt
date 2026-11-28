// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import com.rosan.installer.ui.theme.InstallerTheme
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassNavigationBar
import top.yukonga.miuix.kmp.glass.GlassNavigationBarDefaults
import top.yukonga.miuix.kmp.glass.GlassNavigationItem
import top.yukonga.miuix.kmp.material.GlassDefaults
import top.yukonga.miuix.kmp.material.GlassShadows

@Composable
internal fun InstallerMiuixGlassNavigationBar(
    items: List<NavigationItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    val glassItems = remember(items) {
        items.map { GlassNavigationItem(icon = it.icon, label = it.label) }
    }
    GlassNavigationBar(
        items = glassItems,
        selectedIndex = selectedIndex,
        onSelect = onSelect,
        backdrop = backdrop,
        defaultWindowInsetsPadding = false,
        modifier = modifier.layout { measurable, constraints ->
            val actionWidth = (GlassNavigationBarDefaults.Height + GlassNavigationBarDefaults.ActionSpacing).roundToPx()
            val available = constraints.maxWidth - actionWidth + (10.dp * (items.size - 1) - 6.dp).roundToPx()
            val width = if (constraints.hasBoundedWidth && available > 400.dp.roundToPx()) {
                constraints.constrainWidth(344.dp.roundToPx() + actionWidth)
            } else {
                constraints.maxWidth
            }
            val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
            layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
        },
        style = GlassDefaults.style(InstallerTheme.isDark),
        trailingAction = trailingAction,
    )
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
