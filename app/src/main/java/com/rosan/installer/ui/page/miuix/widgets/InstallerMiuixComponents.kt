// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.widgets

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppMiuixIcons
import com.rosan.installer.ui.theme.InstallerTheme
import com.rosan.installer.ui.theme.getMiuixAppBarColor
import com.rosan.installer.ui.theme.installerMiuixBlurEffect
import com.rosan.installer.ui.theme.rememberMiuixBlurBackdrop
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.glass.GlassDropdownPreference
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassPopupDefaults
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.ChevronBackward
import top.yukonga.miuix.kmp.material.GlassDefaults
import top.yukonga.miuix.kmp.material.isRenderEffectSupported
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal data class MiuixComponentScope(
    val useOs4: Boolean = false,
    val contentBackdrop: LayerBackdrop? = null,
    val pageBackdrop: LayerBackdrop? = null,
    val popupInRootScaffold: Boolean = true,
) {
    val contentBackdropModifier: Modifier
        get() = contentBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier

    val pageBackdropModifier: Modifier
        get() = pageBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier
}

private val LocalMiuixComponentScope = compositionLocalOf { MiuixComponentScope() }
private val LocalMiuixTopBarBackdrop = compositionLocalOf<LayerBackdrop?> { null }

/** Keeps the root popup host outside both the page and scroll-content recordings. */
@Composable
internal fun InstallerMiuixComponentHost(
    useOs4: Boolean,
    useBlur: Boolean,
    modifier: Modifier = Modifier,
    popupInRootScaffold: Boolean = true,
    containerColor: Color = MiuixTheme.colorScheme.surface,
    content: @Composable (MiuixComponentScope) -> Unit,
) {
    val useGlass = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && useOs4 && isRenderEffectSupported()
    val contentBackdrop = rememberMiuixBlurBackdrop(useGlass || useBlur)
    val pageBackdrop = rememberMiuixBlurBackdrop(useGlass)
    val scope = remember(useGlass, contentBackdrop, pageBackdrop, popupInRootScaffold) {
        MiuixComponentScope(useGlass, contentBackdrop, pageBackdrop, popupInRootScaffold)
    }

    CompositionLocalProvider(LocalMiuixComponentScope provides scope) {
        Scaffold(
            modifier = modifier,
            containerColor = containerColor,
            contentWindowInsets = WindowInsets(0),
        ) {
            content(scope)
        }
    }
}

@Composable
internal fun InstallerMiuixTopAppBar(
    title: String,
    isContentScrolled: Boolean? = null,
    modifier: Modifier = Modifier,
    useOs4: Boolean = LocalMiuixComponentScope.current.useOs4 || InstallerTheme.useMiuixOs4,
    backdrop: LayerBackdrop? = LocalMiuixComponentScope.current.contentBackdrop,
    scrollBehavior: ScrollBehavior? = null,
    persistentProgressiveBlur: Boolean = true,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val hasContentScroll by remember(scrollBehavior) {
        derivedStateOf { (scrollBehavior?.state?.contentOffset ?: 0f) < 0f }
    }
    val materialVisible = isContentScrolled ?: hasContentScroll
    if (useOs4) {
        if (persistentProgressiveBlur && backdrop != null) {
            InstallerMiuixProgressiveAppBarSurface(backdrop = backdrop, modifier = modifier) {
                GlassTopAppBar(
                    title = title,
                    isContentScrolled = materialVisible,
                    backdrop = backdrop,
                    style = GlassDefaults.style(InstallerTheme.isDark),
                    scrollBehavior = scrollBehavior,
                    backgroundAlpha = { 0f },
                    navigationIcon = navigationIcon,
                    actions = actions,
                )
            }
        } else {
            GlassTopAppBar(
                title = title,
                isContentScrolled = materialVisible,
                modifier = modifier,
                progressiveBlur = true,
                backdrop = backdrop,
                style = GlassDefaults.style(InstallerTheme.isDark),
                scrollBehavior = scrollBehavior,
                navigationIcon = navigationIcon,
                actions = actions,
            )
        }
    } else {
        TopAppBar(
            title = title,
            modifier = modifier.installerMiuixBlurEffect(backdrop),
            color = backdrop.getMiuixAppBarColor(),
            scrollBehavior = scrollBehavior,
            navigationIcon = navigationIcon,
            actions = actions,
        )
    }
}

/** Fixed sampling viewport: overscroll moves inside this layer, while bars and popups stay outside. */
@Composable
internal fun InstallerMiuixBackdropSurface(
    backdrop: LayerBackdrop?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)) {
        content()
    }
}

/** A spatial gradient over the sampled scene; it never fades out just because a list cannot scroll. */
@Composable
internal fun InstallerMiuixProgressiveAppBarSurface(
    backdrop: LayerBackdrop?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val renderedBackdrop = rememberLayerBackdrop()
    val density = LocalDensity.current
    val topInset = WindowInsets.statusBars.getTop(density)
    val blurColors = BlurColors(
        blendColors = listOf(BlendColorEntry(MiuixTheme.colorScheme.surface.copy(alpha = 0.3f))),
    )
    Box(modifier = modifier) {
        if (backdrop != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .layout { measurable, constraints ->
                        val bandHeight = (topInset + TopAppBarDefaults.CollapsedHeight.toPx() + 20.dp.toPx()) * 1.2f
                        val placeable = measurable.measure(
                            constraints.copy(minHeight = bandHeight.toInt(), maxHeight = bandHeight.toInt()),
                        )
                        layout(constraints.maxWidth, constraints.maxHeight) { placeable.placeRelative(0, 0) }
                    }
                    .layerBackdrop(renderedBackdrop)
                    .progressiveTextureBlur(
                        backdrop = backdrop,
                        shape = RectangleShape,
                        gradient = ProgressiveBlur.Top.copy(curve = 0.5f),
                        blurRadius = GlassTopAppBarDefaults.ProgressiveBlurRadius.value,
                        colors = blurColors,
                    ),
            )
        }
        CompositionLocalProvider(
            LocalMiuixTopBarBackdrop provides renderedBackdrop.takeIf { backdrop != null },
        ) {
            content()
        }
    }
}

@Composable
internal fun InstallerMiuixBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = MiuixTheme.colorScheme.onSurface,
    surfaceAlpha: Float = 1f,
) {
    if (LocalMiuixComponentScope.current.useOs4 || InstallerTheme.useMiuixOs4) {
        InstallerMiuixTopBarIconButton(onClick = onClick, modifier = modifier, surfaceAlpha = surfaceAlpha) {
            Icon(
                imageVector = icon ?: MiuixIcons.Glass.ChevronBackward,
                contentDescription = stringResource(R.string.back),
                modifier = Modifier.size(24.dp),
                tint = iconTint,
            )
        }
    } else {
        MiuixBackButton(onClick = onClick, modifier = modifier, icon = icon ?: AppMiuixIcons.Back, iconTint = iconTint)
    }
}

/** Sample the rendered bar band without reapplying the library's raw-content underlay. */
@Composable
internal fun InstallerMiuixTopBarIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    surfaceAlpha: Float = 1f,
    content: @Composable () -> Unit,
) {
    val backdrop = LocalMiuixTopBarBackdrop.current
    GlassIconButton(
        onClick = onClick,
        modifier = modifier,
        inheritTopBarMaterial = backdrop == null,
        backdrop = backdrop,
        surfaceAlpha = surfaceAlpha,
        content = content,
    )
}

/** Retains the existing spinner for OS3 and for options Glass cannot represent. */
@Composable
internal fun InstallerMiuixSpinnerPreference(
    items: List<DropdownItem>,
    selectedIndex: Int,
    title: String,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    showValue: Boolean = true,
    insideMargin: PaddingValues = top.yukonga.miuix.kmp.basic.BasicComponentDefaults.InsideMargin,
) {
    val scope = LocalMiuixComponentScope.current
    val plainOptions = items.all {
        it.enabled && it.icon == null && it.summary == null && it.children.isNullOrEmpty()
    }
    if (scope.useOs4 && plainOptions) {
        GlassDropdownPreference(
            onSelectedIndexChange = { index ->
                onSelectedIndexChange(index)
                items[index].onClick?.invoke()
            },
            items = items.map { it.text },
            selectedIndex = selectedIndex,
            title = title,
            backdrop = scope.pageBackdrop,
            visuals = GlassPopupDefaults.visuals(style = GlassDefaults.style(InstallerTheme.isDark)),
            modifier = modifier,
            enabled = enabled,
            showValue = showValue,
            insideMargin = insideMargin,
            renderInRootScaffold = scope.popupInRootScaffold,
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
