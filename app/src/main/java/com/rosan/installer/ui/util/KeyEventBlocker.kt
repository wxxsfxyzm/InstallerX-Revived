// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors

// Portions of this file are derived from weishu/KernelSU
// (https://github.com/tiann/KernelSU)
// Copyright (C) KernelSU contributors
// Licensed under GPL-3.0
package com.rosan.installer.ui.util

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onKeyEvent

// Code from weishu/KernelSU
// Licensed under GPLv3
@Composable
fun KeyEventBlocker(predicate: (KeyEvent) -> Boolean) {
    val requester = remember { FocusRequester() }
    Box(
        Modifier
            .onKeyEvent {
                predicate(it)
            }
            .focusRequester(requester)
            .focusable()
    )
    LaunchedEffect(Unit) {
        requester.requestFocus()
    }
}