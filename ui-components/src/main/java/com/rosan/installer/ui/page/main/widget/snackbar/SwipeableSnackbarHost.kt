// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.widget.snackbar

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier

@Composable
fun SwipeableSnackbarHost(
    modifier: Modifier = Modifier,
    hostState: SnackbarHostState,
    snackbar: @Composable (SnackbarData) -> Unit = { Snackbar(it) },
) {
    val state = rememberSwipeToDismissBoxState()

    SwipeToDismissBox(
        state = state,
        backgroundContent = {},
        onDismiss = {
            hostState.currentSnackbarData?.dismiss()
        },
    ) {
        SnackbarHost(
            modifier = modifier,
            hostState = hostState,
            snackbar = snackbar,
        )
    }

    LaunchedEffect(hostState.currentSnackbarData) {
        if (hostState.currentSnackbarData == null) return@LaunchedEffect

        state.snapTo(SwipeToDismissBoxValue.Settled)
    }
}
