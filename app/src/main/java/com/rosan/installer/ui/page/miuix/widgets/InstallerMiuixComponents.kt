// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.widgets

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppMiuixIcons
import com.rosan.installer.ui.theme.InstallerTheme
import com.rosan.installer.ui.theme.getMiuixAppBarColor
import com.rosan.installer.ui.theme.installerMiuixBlurEffect
import com.rosan.installer.ui.theme.rememberMiuixBlurBackdrop
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.glass.GlassDropdownPreference
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassPopupDefaults
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.ChevronBackward
import top.yukonga.miuix.kmp.material.GlassDefaults
import top.yukonga.miuix.kmp.material.isRenderEffectSupported
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

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
    if (useOs4) {
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
            style = GlassDefaults.style(InstallerTheme.isDark),
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
internal fun Modifier.installerMiuixTopBarScroll(scrollBehavior: ScrollBehavior): Modifier {
    val state = scrollBehavior.state
    val onOffsetChanged: ((Float) -> Unit)? = if (InstallerTheme.useMiuixOs4) {
        remember(state) { { offset: Float -> state.overscrollOffset = offset } }
    } else {
        null
    }
    return this
        .overScrollVertical(onOffsetChanged = onOffsetChanged)
        .nestedScroll(scrollBehavior.nestedScrollConnection)
}

@Composable
internal fun rememberMiuixPopupBackdropActive(show: Boolean): Boolean {
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
internal fun InstallerMiuixBackdropSurface(
    backdrop: LayerBackdrop?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)) {
        content()
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
