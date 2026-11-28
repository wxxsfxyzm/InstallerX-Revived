// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.miuix.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import com.rosan.installer.ui.miuix.theme.getMiuixAppBarColor
import com.rosan.installer.ui.miuix.theme.installerMiuixBlurEffect
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.glass.GlassDropdownPreference
import top.yukonga.miuix.kmp.glass.GlassPopupDefaults
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.material.GlassDefaults
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
fun MiuixAdaptiveTopAppBar(
    title: String,
    useGlass: Boolean,
    isDark: Boolean,
    isContentScrolled: Boolean? = null,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = null,
    scrollBehavior: ScrollBehavior? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
) {
    if (useGlass) {
        val hasContentScroll by remember(scrollBehavior) {
            derivedStateOf { (scrollBehavior?.state?.contentOffset ?: 0f) < -1f }
        }
        val windowInfo = LocalWindowInfo.current
        val density = LocalDensity.current
        val isWide by remember(windowInfo, density) {
            derivedStateOf {
                val windowSize = windowInfo.containerSize
                with(density) {
                    val width = windowSize.width.toDp()
                    val height = windowSize.height.toDp()
                    width >= 840.dp || (width >= 600.dp && height < width * 1.2f)
                }
            }
        }
        GlassTopAppBar(
            title = title,
            isContentScrolled = isContentScrolled ?: hasContentScroll,
            modifier = modifier,
            expandable = !isWide,
            progressiveBlur = true,
            backdrop = backdrop,
            style = GlassDefaults.style(isDark),
            scrollBehavior = scrollBehavior,
            navigationIcon = navigationIcon,
            actions = actions,
            bottomContent = bottomContent,
        )
    } else {
        TopAppBar(
            title = title,
            modifier = modifier.installerMiuixBlurEffect(backdrop),
            color = backdrop.getMiuixAppBarColor(),
            scrollBehavior = scrollBehavior,
            navigationIcon = navigationIcon,
            actions = actions,
            bottomContent = bottomContent,
        )
    }
}

@Composable
fun Modifier.miuixTopBarScroll(scrollBehavior: ScrollBehavior, reportOverscroll: Boolean): Modifier {
    val state = scrollBehavior.state
    val onOffsetChanged: ((Float) -> Unit)? = if (reportOverscroll) {
        remember(state) { { offset: Float -> state.overscrollOffset = offset } }
    } else {
        null
    }
    return this
        .overScrollVertical(onOffsetChanged = onOffsetChanged)
        .nestedScroll(scrollBehavior.nestedScrollConnection)
}

@Composable
fun rememberMiuixPopupBackdropActive(show: Boolean): Boolean {
    var active by remember { mutableStateOf(show) }
    LaunchedEffect(show) {
        if (show) {
            active = true
        } else if (active) {
            // The public popup APIs have no dismissal-complete callback. Use the example's exit interval.
            delay(350)
            active = false
        }
    }
    return show || active
}

/** Fixed sampling viewport: overscroll moves inside this layer, while bars and popups stay outside. */
@Composable
fun InstallerMiuixBackdropSurface(
    backdrop: LayerBackdrop?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)) {
        content()
    }
}

/** Retains the existing spinner for OS3 and for options Glass cannot represent. */
@Composable
fun MiuixAdaptiveSpinnerPreference(
    items: List<DropdownItem>,
    selectedIndex: Int,
    title: String,
    useGlass: Boolean,
    isDark: Boolean,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = null,
    renderInRootScaffold: Boolean = true,
    summary: String? = null,
    enabled: Boolean = true,
    showValue: Boolean = true,
    insideMargin: PaddingValues = top.yukonga.miuix.kmp.basic.BasicComponentDefaults.InsideMargin,
) {
    val plainOptions = items.all {
        it.enabled && it.icon == null && it.summary == null && it.children.isNullOrEmpty()
    }
    if (useGlass && plainOptions) {
        GlassDropdownPreference(
            onSelectedIndexChange = { index ->
                onSelectedIndexChange(index)
                items[index].onClick?.invoke()
            },
            items = items.map { it.text },
            selectedIndex = selectedIndex,
            title = title,
            backdrop = backdrop,
            visuals = GlassPopupDefaults.visuals(style = GlassDefaults.style(isDark)),
            modifier = modifier,
            enabled = enabled,
            showValue = showValue,
            insideMargin = insideMargin,
            renderInRootScaffold = renderInRootScaffold,
            summary = summary,
        )
    } else {
        WindowSpinnerPreference(
            items = items,
            selectedIndex = selectedIndex,
            title = title,
            modifier = modifier,
            summary = summary,
            enabled = enabled,
            showValue = showValue,
            insideMargin = insideMargin,
            onSelectedIndexChange = onSelectedIndexChange,
        )
    }
}
