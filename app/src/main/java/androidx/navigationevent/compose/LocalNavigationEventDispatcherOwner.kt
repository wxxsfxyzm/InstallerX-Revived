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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.findViewTreeNavigationEventDispatcherOwner

/** The CompositionLocal containing the current [NavigationEventDispatcher]. */
object LocalNavigationEventDispatcherOwner {
    private val LocalNavigationEventDispatcherOwner =
        compositionLocalOf<NavigationEventDispatcherOwner?> { null }

    /**
     * Returns current composition local value for the owner or `null` if one has not been provided
     * nor is one available via [findViewTreeNavigationEventDispatcherOwner] on the current
     * `androidx.compose.ui.platform.LocalView`.
     */
    val current: NavigationEventDispatcherOwner?
        @Composable
        get() =
            LocalNavigationEventDispatcherOwner.current
                ?: findViewTreeNavigationEventDispatcherOwner()

    /**
     * Associates a [LocalNavigationEventDispatcherOwner] key to a value in a call to
     * [CompositionLocalProvider].
     */
    infix fun provides(
        navigationEventDispatcherOwner: NavigationEventDispatcherOwner
    ): ProvidedValue<NavigationEventDispatcherOwner?> {
        return LocalNavigationEventDispatcherOwner.provides(navigationEventDispatcherOwner)
    }
}

@Composable
internal fun findViewTreeNavigationEventDispatcherOwner(): NavigationEventDispatcherOwner? =
    LocalView.current.findViewTreeNavigationEventDispatcherOwner()