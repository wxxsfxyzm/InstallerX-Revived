// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.widgets

import android.os.Build
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppMiuixIcons
import com.rosan.installer.ui.miuix.theme.rememberMiuixBlurBackdrop
import com.rosan.installer.ui.miuix.widgets.MiuixAdaptiveSpinnerPreference
import com.rosan.installer.ui.miuix.widgets.MiuixAdaptiveTopAppBar
import com.rosan.installer.ui.miuix.widgets.miuixTopBarScroll
import com.rosan.installer.ui.theme.InstallerTheme
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.ChevronBackward
import top.yukonga.miuix.kmp.material.isRenderEffectSupported
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
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
) {
    MiuixAdaptiveTopAppBar(
        title = title,
        useGlass = useOs4,
        isDark = InstallerTheme.isDark,
        isContentScrolled = isContentScrolled,
        modifier = modifier,
        backdrop = backdrop,
        scrollBehavior = scrollBehavior,
        navigationIcon = navigationIcon,
        actions = actions,
        bottomContent = bottomContent,
    )
}

@Composable
internal fun Modifier.installerMiuixTopBarScroll(scrollBehavior: ScrollBehavior): Modifier = miuixTopBarScroll(scrollBehavior, reportOverscroll = InstallerTheme.useMiuixOs4)

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
        AppMiuixBackButton(onClick = onClick, modifier = modifier, icon = icon ?: AppMiuixIcons.Back, iconTint = iconTint)
    }
}

@Composable
internal fun InstallerMiuixTopBarIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    surfaceAlpha: Float = 1f,
    content: @Composable () -> Unit,
) {
    GlassIconButton(
        onClick = onClick,
        modifier = modifier,
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
    MiuixAdaptiveSpinnerPreference(
        items = items,
        selectedIndex = selectedIndex,
        title = title,
        useGlass = scope.useOs4,
        isDark = InstallerTheme.isDark,
        onSelectedIndexChange = onSelectedIndexChange,
        modifier = modifier,
        backdrop = scope.pageBackdrop,
        renderInRootScaffold = scope.popupInRootScaffold,
        summary = summary,
        enabled = enabled,
        showValue = showValue,
        insideMargin = insideMargin,
    )
}
