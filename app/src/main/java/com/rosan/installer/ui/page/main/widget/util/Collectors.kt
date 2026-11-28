// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.widget.util

import android.annotation.SuppressLint
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import com.rosan.installer.ui.feature.settings.config.edit.EditViewEvent
import com.rosan.installer.ui.feature.settings.config.edit.EditViewModel
import com.rosan.installer.ui.navigation.LocalNavigator
import kotlinx.coroutines.flow.collectLatest

@Composable
fun EditEventCollector(viewModel: EditViewModel, snackBarHostState: SnackbarHostState) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val unknownErrorString = stringResource(R.string.installer_unknown_error)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is EditViewEvent.SnackBar -> {
                    // Resolve priority:
                    // 1. Specific Resource ID
                    // 2. Explicit String message
                    // 3. Localized generic fallback
                    val snackBarText =
                        event.messageResId?.let {
                            @SuppressLint("LocalContextGetResourceValueCall")
                            context.getString(it)
                        }
                            ?: event.message
                            ?: unknownErrorString

                    snackBarHostState.showSnackbar(
                        message = snackBarText,
                        withDismissAction = true,
                    )
                }

                is EditViewEvent.Saved -> {
                    navigator.pop()
                }
            }
        }
    }
}
