// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.settings.preferred.about.ossLicensePage

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.rosan.installer.R
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.miuix.widgets.InstallerMiuixBackButton
import com.rosan.installer.ui.page.miuix.widgets.InstallerMiuixBackdropSurface
import com.rosan.installer.ui.page.miuix.widgets.InstallerMiuixTopAppBar
import com.rosan.installer.ui.theme.rememberMiuixBlurBackdrop
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun MiuixOpenSourceLicensePage(useBlur: Boolean) {
    val navigator = LocalNavigator.current
    val libraries by produceLibraries(R.raw.aboutlibraries)
    val scrollBehavior = MiuixScrollBehavior()

    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    val topBarBackdrop = rememberMiuixBlurBackdrop(useBlur)

    Scaffold(
        topBar = {
            InstallerMiuixTopAppBar(
                backdrop = topBarBackdrop,
                title = stringResource(id = R.string.open_source_license),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    InstallerMiuixBackButton(onClick = { navigator.pop() })
                },
            )
        },
    ) { paddingValues ->
        InstallerMiuixBackdropSurface(
            backdrop = topBarBackdrop,
            modifier = Modifier.fillMaxSize(),
        ) {
            LibrariesContainer(
                modifier = Modifier
                    .fillMaxSize()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    start = horizontalSafeInsets.calculateStartPadding(layoutDirection),
                    top = paddingValues.calculateTopPadding(),
                    end = horizontalSafeInsets.calculateEndPadding(layoutDirection),
                ),
                libraries = libraries,
            )
        }
    }
}
