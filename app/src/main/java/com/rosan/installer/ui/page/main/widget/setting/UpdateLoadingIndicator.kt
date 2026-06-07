// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.widget.setting

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import com.rosan.installer.ui.page.main.settings.preferred.about.AboutEvent
import com.rosan.installer.ui.page.main.settings.preferred.about.AboutViewModel
import top.yukonga.miuix.kmp.blur.LayerBackdrop

@Composable
fun UpdateLoadingIndicator(
    backdrop: LayerBackdrop?,
    viewModel: AboutViewModel
) {
    var showUpdateLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is AboutEvent.ShowUpdateLoading -> showUpdateLoading = true
                is AboutEvent.HideUpdateLoading -> showUpdateLoading = false
                is AboutEvent.ShowInAppUpdateErrorDetail -> showUpdateLoading = false
                else -> Unit
            }
        }
    }

    BlockingLoadingIndicator(
        visible = showUpdateLoading,
        text = stringResource(R.string.updating),
        backdrop = backdrop
    )
}
