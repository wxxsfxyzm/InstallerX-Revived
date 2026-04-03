/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.navigationevent.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState

/**
 * A composable that handles navigation events using simple lambda handlers, driven by a manually
 * hoisted [NavigationEventState].
 *
 * This is the core implementation of the navigation event handler. This overload must be used when
 * you need to hoist the [NavigationEventState] (by calling [rememberNavigationEventState] at a
 * higher level). Hoisting is necessary when other composables need to react to the gesture's
 * [NavigationEventTransitionState] (held within the `state` object), for example, to drive custom
 * animations.
 *
 * ## Precedence
 * When multiple [NavigationEventHandler] are present in the composition, the one that is composed
 * *last* among all enabled handlers will be invoked.
 *
 * ## Usage
 * It is important to call this composable **unconditionally**. Use [isBackEnabled] and
 * [isForwardEnabled] to control whether the handler is active. This is preferable to conditionally
 * calling [NavigationEventHandler] (e.g., inside an `if` block), as conditional calls can change
 * the order of composition, leading to unpredictable behavior where different handlers are invoked
 * after recomposition.
 *
 * ## Timing Consideration
 * There are cases where a predictive back or forward gesture may be dispatched within a rendering
 * frame before the corresponding `enabled` flag is updated, which can cause unexpected behavior
 * (see [b/375343407](https://issuetracker.google.com/375343407),
 * [b/384186542](https://issuetracker.google.com/384186542)). For example, if [isBackEnabled] is set
 * to `false`, a back gesture initiated in the same frame may still trigger this handler because the
 * system sees the stale `true` value.
 *
 * @param state The hoisted [NavigationEventState] (returned from [rememberNavigationEventState]) to
 *   be registered. This object links this handler's callbacks to the unique handler instance that
 *   is producing the state.
 * @param isForwardEnabled Controls whether forward navigation gestures are handled.
 * @param onForwardCancelled Called if a forward navigation gesture is cancelled.
 * @param onForwardCompleted Called when a forward navigation gesture completes.
 * @param isBackEnabled Controls whether back navigation gestures are handled.
 * @param onBackCancelled Called if a back navigation gesture is cancelled.
 * @param onBackCompleted Called when a back navigation gesture completes.
 * @throws IllegalArgumentException If the provided [NavigationEventState] is passed to multiple
 *   [NavigationEventHandler] Composable. Each handler must have its own unique state.
 */
@Composable
fun NavigationEventHandler(
    state: NavigationEventState<out NavigationEventInfo>,
    // ---- Forward Events ----
    isForwardEnabled: Boolean = true,
    onForwardCancelled: (() -> Unit) -> Unit = { callBack ->
        callBack()
    },
    onForwardCompleted: (() -> Unit) -> Unit = { callBack ->
        callBack()
    },
    // ---- Back Events ----
    isBackEnabled: Boolean = true,
    onBackCancelled: (() -> Unit) -> Unit = { callBack ->
        callBack()
    },
    onBackCompleted: (() -> Unit) -> Unit = { callBack ->
        callBack()
    },
) {
    if (LocalInspectionMode.current) {
        // TODO(b/462365661): Return early to prevent Preview crashes. Future work should implement
        //  full support for navigation events in Interactive Previews instead of disabling them.
        return
    }

    val dispatcher =
        checkNotNull(LocalNavigationEventDispatcherOwner.current) {
            "No NavigationEventDispatcher was provided via LocalNavigationEventDispatcherOwner"
        }
            .navigationEventDispatcher

    val sourceHandler =
        remember(state) {
            ComposeNavigationEventHandler(
                initialInfo = state.currentInfo,
                onTransitionStateChanged = { transitionState ->
                    state.transitionState = transitionState
                },
            )
        }

    SideEffect {
        sourceHandler.isForwardEnabled = isForwardEnabled
        sourceHandler.currentOnForwardCancelled = onForwardCancelled
        sourceHandler.currentOnForwardCompleted = onForwardCompleted

        sourceHandler.isBackEnabled = isBackEnabled
        sourceHandler.currentOnBackCancelled = onBackCancelled
        sourceHandler.currentOnBackCompleted = onBackCompleted

        sourceHandler.setInfo(state.currentInfo, state.backInfo, state.forwardInfo)
    }

    DisposableEffect(state) {
        require(state.sourceHandler == null) {
            "NavigationEventState '$state' is already registered with a NavigationEventHandler '$sourceHandler'."
        }

        state.sourceHandler = sourceHandler
        dispatcher.addHandler(sourceHandler)

        onDispose {
            sourceHandler.remove()
            state.sourceHandler = null
        }
    }
}

/**
 * A composable that handles only back navigation gestures, driven by a manually hoisted
 * [NavigationEventState].
 *
 * This is a convenience wrapper around the core [NavigationEventHandler] overload for cases where
 * forward navigation is not relevant. Use this overload when hoisting state (e.g., for custom
 * animations).
 *
 * Refer to the primary [NavigationEventHandler] KDoc for details on precedence, unconditional
 * usage, and timing considerations.
 *
 * @param state The hoisted [NavigationEventState] (returned from [rememberNavigationEventState]) to
 *   be registered.
 * @param isBackEnabled Controls whether back navigation gestures are handled.
 * @param onBackCancelled Called if a back navigation gesture is cancelled.
 * @param onBackCompleted Called when a back navigation gesture completes and navigation occurs.
 */
@Composable
fun NavigationBackHandler(
    state: NavigationEventState<out NavigationEventInfo>,
    isBackEnabled: Boolean = true,
    onBackCancelled: (() -> Unit) -> Unit = { callback ->
        callback()
    },
    onBackCompleted: (() -> Unit) -> Unit,
) {
    NavigationEventHandler(
        state = state,
        onForwardCancelled = {

        },
        onForwardCompleted = {},
        isForwardEnabled = false, // disable forward
        onBackCancelled = onBackCancelled,
        onBackCompleted = onBackCompleted,
        isBackEnabled = isBackEnabled,
    )
}

/**
 * A composable that handles only forward navigation gestures, driven by a manually hoisted
 * [NavigationEventState].
 *
 * This is a convenience wrapper around the core [NavigationEventHandler] overload for cases where
 * back navigation is not relevant. Use this overload when hoisting state.
 *
 * Refer to the primary [NavigationEventHandler] KDoc for details on precedence, unconditional
 * usage, and timing considerations.
 *
 * @param state The hoisted [NavigationEventState] (returned from [rememberNavigationEventState]) to
 *   be registered.
 * @param isForwardEnabled Controls whether forward navigation gestures are handled.
 * @param onForwardCancelled Called if a forward navigation gesture is cancelled.
 * @param onForwardCompleted Called when a forward navigation gesture completes and navigation
 *   occurs.
 */
@Composable
fun NavigationForwardHandler(
    state: NavigationEventState<out NavigationEventInfo>,
    isForwardEnabled: Boolean = true,
    onForwardCancelled: (() -> Unit) -> Unit = { callBack ->
        callBack()
    },
    onForwardCompleted: (() -> Unit) -> Unit,
) {
    NavigationEventHandler(
        state = state,
        onForwardCancelled = onForwardCancelled,
        onForwardCompleted = onForwardCompleted,
        isForwardEnabled = isForwardEnabled,
        onBackCancelled = { callBack -> callBack() },
        onBackCompleted = { callBack -> callBack() },
        isBackEnabled = false, // disable back
    )
}

/** A simple [NavigationEventHandler] that delegates its methods to lambda functions. */
private class ComposeNavigationEventHandler<T : NavigationEventInfo>(
    initialInfo: T,
    private val onTransitionStateChanged: (NavigationEventTransitionState) -> Unit = {},
) :
    NavigationEventHandler<T>(
        initialInfo = initialInfo,
        isBackEnabled = false,
        isForwardEnabled = false,
    ) {

    var currentOnForwardCancelled: (() -> Unit) -> Unit = {}
    var currentOnForwardCompleted: (() -> Unit) -> Unit = {}
    var currentOnBackCancelled: (() -> Unit) -> Unit = {}
    var currentOnBackCompleted: (() -> Unit) -> Unit = {}

    override fun onForwardStarted(event: NavigationEvent) {
        onTransitionStateChanged(transitionState)
    }

    override fun onForwardProgressed(event: NavigationEvent) {
        onTransitionStateChanged(transitionState)
    }

    override fun onForwardCancelled() {
        currentOnForwardCancelled.invoke {
            onTransitionStateChanged(transitionState)
        }
    }

    override fun onForwardCompleted() {
        currentOnForwardCompleted.invoke {
            onTransitionStateChanged(transitionState)
        }
    }

    override fun onBackStarted(event: NavigationEvent) {
        onTransitionStateChanged(transitionState)
    }

    override fun onBackProgressed(event: NavigationEvent) {
        onTransitionStateChanged(transitionState)
    }

    override fun onBackCancelled() {
        currentOnBackCancelled.invoke {
            onTransitionStateChanged(transitionState)
        }
    }

    override fun onBackCompleted() {
        currentOnBackCompleted.invoke {
            onTransitionStateChanged(transitionState)
        }
    }
}

// Compatible with the fucking miuix
/**
 * A composable that handles only back navigation gestures, driven by a manually hoisted
 * [NavigationEventState].
 *
 * This is a convenience wrapper around the core [NavigationEventHandler] overload for cases where
 * forward navigation is not relevant. Use this overload when hoisting state (e.g., for custom
 * animations).
 *
 * Refer to the primary [NavigationEventHandler] KDoc for details on precedence, unconditional
 * usage, and timing considerations.
 *
 * @param state The hoisted [NavigationEventState] (returned from [rememberNavigationEventState]) to
 *   be registered.
 * @param isBackEnabled Controls whether back navigation gestures are handled.
 * @param onBackCancelled Called if a back navigation gesture is cancelled.
 * @param onBackCompleted Called when a back navigation gesture completes and navigation occurs.
 */
@Composable
@Suppress("unused") // Reason: Miuix Library use that
fun NavigationBackHandler(
    state: NavigationEventState<out NavigationEventInfo>,
    isBackEnabled: Boolean = true,
    onBackCancelled: () -> Unit = {},
    onBackCompleted: () -> Unit,
) {
    NavigationEventHandler(
        state = state,
        onForwardCancelled = {},
        onForwardCompleted = {},
        isForwardEnabled = false, // disable forward
        onBackCancelled = { callBack ->
            callBack()
            onBackCancelled()
        },
        onBackCompleted = { callBack ->
            callBack()
            onBackCompleted()
        },
        isBackEnabled = isBackEnabled,
    )
}

/**
 * A composable that handles navigation events using simple lambda handlers, driven by a manually
 * hoisted [NavigationEventState].
 *
 * This is the core implementation of the navigation event handler. This overload must be used when
 * you need to hoist the [NavigationEventState] (by calling [rememberNavigationEventState] at a
 * higher level). Hoisting is necessary when other composables need to react to the gesture's
 * [NavigationEventTransitionState] (held within the `state` object), for example, to drive custom
 * animations.
 *
 * ## Precedence
 * When multiple [NavigationEventHandler] are present in the composition, the one that is composed
 * *last* among all enabled handlers will be invoked.
 *
 * ## Usage
 * It is important to call this composable **unconditionally**. Use [isBackEnabled] and
 * [isForwardEnabled] to control whether the handler is active. This is preferable to conditionally
 * calling [NavigationEventHandler] (e.g., inside an `if` block), as conditional calls can change
 * the order of composition, leading to unpredictable behavior where different handlers are invoked
 * after recomposition.
 *
 * ## Timing Consideration
 * There are cases where a predictive back or forward gesture may be dispatched within a rendering
 * frame before the corresponding `enabled` flag is updated, which can cause unexpected behavior
 * (see [b/375343407](https://issuetracker.google.com/375343407),
 * [b/384186542](https://issuetracker.google.com/384186542)). For example, if [isBackEnabled] is set
 * to `false`, a back gesture initiated in the same frame may still trigger this handler because the
 * system sees the stale `true` value.
 *
 * @param state The hoisted [NavigationEventState] (returned from [rememberNavigationEventState]) to
 *   be registered. This object links this handler's callbacks to the unique handler instance that
 *   is producing the state.
 * @param isForwardEnabled Controls whether forward navigation gestures are handled.
 * @param onForwardCancelled Called if a forward navigation gesture is cancelled.
 * @param onForwardCompleted Called when a forward navigation gesture completes.
 * @param isBackEnabled Controls whether back navigation gestures are handled.
 * @param onBackCancelled Called if a back navigation gesture is cancelled.
 * @param onBackCompleted Called when a back navigation gesture completes.
 * @throws IllegalArgumentException If the provided [NavigationEventState] is passed to multiple
 *   [NavigationEventHandler] Composable. Each handler must have its own unique state.
 */
@Composable
@Suppress("unused") // Reason: Keep same ABI
fun NavigationEventHandler(
    state: NavigationEventState<out NavigationEventInfo>,
    // ---- Forward Events ----
    isForwardEnabled: Boolean = true,
    onForwardCancelled: () -> Unit = {},
    onForwardCompleted: () -> Unit = {},
    // ---- Back Events ----
    isBackEnabled: Boolean = true,
    onBackCancelled: () -> Unit = {},
    onBackCompleted: () -> Unit = {},
) {
    NavigationEventHandler(
        state,
        isForwardEnabled,
        onForwardCancelled = { callBack ->
            callBack()
            onForwardCancelled()
        },
        onForwardCompleted = { callBack ->
            callBack()
            onForwardCompleted()
        },
        isBackEnabled,
        onBackCancelled = { callBack ->
            callBack()
            onBackCancelled()
        },
        onBackCompleted = { callBack ->
            callBack()
            onBackCompleted()
        }
    )
}

/**
 * A composable that handles only forward navigation gestures, driven by a manually hoisted
 * [NavigationEventState].
 *
 * This is a convenience wrapper around the core [NavigationEventHandler] overload for cases where
 * back navigation is not relevant. Use this overload when hoisting state.
 *
 * Refer to the primary [NavigationEventHandler] KDoc for details on precedence, unconditional
 * usage, and timing considerations.
 *
 * @param state The hoisted [NavigationEventState] (returned from [rememberNavigationEventState]) to
 *   be registered.
 * @param isForwardEnabled Controls whether forward navigation gestures are handled.
 * @param onForwardCancelled Called if a forward navigation gesture is cancelled.
 * @param onForwardCompleted Called when a forward navigation gesture completes and navigation
 *   occurs.
 */
@Composable
fun NavigationForwardHandler(
    state: NavigationEventState<out NavigationEventInfo>,
    isForwardEnabled: Boolean = true,
    onForwardCancelled: () -> Unit = {},
    onForwardCompleted: () -> Unit,
) {
    NavigationEventHandler(
        state = state,
        onForwardCancelled = { callBack ->
            callBack()
            onForwardCancelled()
        },
        onForwardCompleted = { callBack ->
            callBack()
            onForwardCompleted()
        },
        isForwardEnabled = isForwardEnabled,
        onBackCancelled = { callBack -> callBack() },
        onBackCompleted = { callBack -> callBack() },
        isBackEnabled = false, // disable back
    )
}