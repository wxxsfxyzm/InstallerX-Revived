// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.core.widgets.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * A generic Composable to listen for specific lifecycle events.
 *
 * @param event The lifecycle event to listen for (e.g., Lifecycle.Event.ON_RESUME).
 * @param lifecycleOwner The lifecycle owner, defaults to the current LocalLifecycleOwner.
 * @param onEvent The action to perform when the event occurs.
 */
@Composable
fun OnLifecycleEvent(event: Lifecycle.Event, lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current, onEvent: () -> Unit) {
    // Use rememberUpdatedState to ensure the latest lambda is captured
    // without restarting the effect if the lambda reference changes.
    val currentOnEvent by rememberUpdatedState(onEvent)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, e ->
            if (e == event) {
                currentOnEvent()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
