// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun rememberUnknownSourcePermissionActionVisible(
    isWaitingUnknownSource: Boolean
): Boolean {
    val lifecycleOwner = LocalLifecycleOwner.current
    var actionVisible by remember(isWaitingUnknownSource) {
        mutableStateOf(!isWaitingUnknownSource)
    }

    DisposableEffect(lifecycleOwner, isWaitingUnknownSource) {
        if (!isWaitingUnknownSource) return@DisposableEffect onDispose { }

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                actionVisible = true
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return actionVisible
}
