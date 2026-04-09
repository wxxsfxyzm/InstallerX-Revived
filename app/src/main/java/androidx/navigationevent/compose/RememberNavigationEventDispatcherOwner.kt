/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.NavigationEventInput

/**
 * Remembers a new [NavigationEventDispatcherOwner] which creates a dispatcher linked to a parent
 * dispatcher found in the composition.
 *
 * This composable creates a dispatcher that links to any parent dispatcher found in the
 * composition, forming a parent-child relationship. If no parent exists, it automatically becomes a
 * new root dispatcher, this is the top-most parent in a hierarchy. This is useful for isolating
 * navigation handling within specific UI sections, such as a self-contained feature screen or tab.
 *
 * The dispatcher's lifecycle is automatically managed. It is created only once and automatically
 * disposed of when the composable leaves the composition, preventing memory leaks.
 *
 * When used to create a root dispatcher, you must use a [NavigationEventInput] to send it events.
 * Otherwise, the dispatcher will be detached and will not receive events.
 *
 * To provide the new [NavigationEventDispatcherOwner] to a sub-composition, use
 * [androidx.compose.runtime.CompositionLocalProvider]:
 *
 * @samples androidx.navigationevent.compose.samples.RememberNavigationEventDispatcherOwner
 *
 * **Null parent:** If [parent] is **EXPLICITLY** `null`, this creates a root dispatcher that runs
 * independently. By default, it requires a parent from the [LocalNavigationEventDispatcherOwner]
 * and will throw an [IllegalStateException] if one is not present.
 *
 * @param enabled Controls if the dispatcher is active. If this value changes, the dispatcher's
 *   `isEnabled` property will be updated. When `false`, this dispatcher and any of its children
 *   will not receive events. Defaults to `true`.
 * @param parent The [NavigationEventDispatcherOwner] to use as the parent, or `null` if it is a
 *   root. Defaults to the owner from [LocalNavigationEventDispatcherOwner].
 * @return A new [NavigationEventDispatcherOwner] that is remembered across compositions.
 */
@Composable
fun rememberNavigationEventDispatcherOwner(
    enabled: Boolean = true,
    parent: NavigationEventDispatcherOwner? =
        checkNotNull(LocalNavigationEventDispatcherOwner.current) {
            "No NavigationEventDispatcherOwner provided in LocalNavigationEventDispatcherOwner. " +
                    "If you intended to create a root dispatcher, explicitly pass null as the parent."
        },
): NavigationEventDispatcherOwner {
    val localDispatcher =
        remember(parent) {
            // If a parent dispatcher exists, link to it. Otherwise, create a new root dispatcher.
            if (parent != null) {
                NavigationEventDispatcher(parent = parent.navigationEventDispatcher)
            } else {
                NavigationEventDispatcher()
            }
        }

    LaunchedEffect(enabled) { localDispatcher.isEnabled = enabled }

    // Clean up the dispatcher on dispose to prevent memory leaks.
    DisposableEffect(localDispatcher) { onDispose { localDispatcher.dispose() } }

    return remember(localDispatcher) {
        ComposeNavigationEventDispatcherOwner(navigationEventDispatcher = localDispatcher)
    }
}

/**
 * A private, concrete implementation of [NavigationEventDispatcherOwner] that simply holds a given
 * [NavigationEventDispatcher].
 */
private class ComposeNavigationEventDispatcherOwner(
    override val navigationEventDispatcher: NavigationEventDispatcher
) : NavigationEventDispatcherOwner
