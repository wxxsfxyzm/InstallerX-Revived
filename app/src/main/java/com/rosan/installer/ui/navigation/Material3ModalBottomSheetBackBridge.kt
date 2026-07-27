// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.findViewTreeNavigationEventDispatcherOwner

/**
 * Bridges back events from Material3's modal-sheet window to the dispatcher inherited from the
 * parent composition. Material3 currently inherits the NavEntry dispatcher when it creates its
 * dialog composition, while the dialog window receives platform back events on its own dispatcher.
 *
 * TODO: Replace this local bridge with miuix-nav's WindowNavigationEventBridge and delete this file
 * once InstallerX updates to a dependency version that contains that API.
 */
@Composable
fun Material3ModalBottomSheetBackBridge() {
    val inheritedDispatcher =
        LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher ?: return
    val view = LocalView.current
    val windowDispatcher = remember(view) {
        view.findViewTreeNavigationEventDispatcherOwner()?.navigationEventDispatcher
    } ?: return
    if (windowDispatcher === inheritedDispatcher) return

    val forwardingInput = remember(inheritedDispatcher) { DirectNavigationEventInput() }
    val forwardingHandler = remember(windowDispatcher, forwardingInput) {
        object : NavigationEventHandler<NavigationEventInfo>(
            initialInfo = NavigationEventInfo.None,
            isBackEnabled = true,
            isForwardEnabled = false,
        ) {
            override fun onBackStarted(event: NavigationEvent) {
                forwardingInput.backStarted(event)
            }

            override fun onBackProgressed(event: NavigationEvent) {
                forwardingInput.backProgressed(event)
            }

            override fun onBackCancelled() {
                forwardingInput.backCancelled()
            }

            override fun onBackCompleted() {
                forwardingInput.backCompleted()
            }
        }
    }

    DisposableEffect(inheritedDispatcher, forwardingInput) {
        inheritedDispatcher.addInput(forwardingInput)
        onDispose { inheritedDispatcher.removeInput(forwardingInput) }
    }
    DisposableEffect(windowDispatcher, forwardingHandler) {
        windowDispatcher.addHandler(forwardingHandler)
        onDispose { forwardingHandler.remove() }
    }
}
