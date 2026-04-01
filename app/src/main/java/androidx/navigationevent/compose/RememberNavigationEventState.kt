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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.navigationevent.NavigationEventInfo

/**
 * Remembers and returns a [NavigationEventState] instance.
 *
 * This composable creates and remembers a [NavigationEventState] object, which holds a
 * [NavigationEventHandler] internally. This is the state object that can be passed to
 * [NavigationEventHandler] (the composable) to "hoist" the state.
 *
 * The state's handler info (currentInfo, backInfo, forwardInfo) is kept in sync with the provided
 * parameters via a [SideEffect].
 *
 * @param T The type of [NavigationEventInfo] this state will manage.
 * @param currentInfo The object representing the current destination.
 * @param backInfo A list of destinations the user may navigate back to (nearest-first).
 * @param forwardInfo A list of destinations the user may navigate forward to (nearest-first).
 * @return A stable, remembered [NavigationEventState] instance.
 */
@Composable
fun <T : NavigationEventInfo> rememberNavigationEventState(
    currentInfo: T,
    backInfo: List<T> = emptyList(),
    forwardInfo: List<T> = emptyList(),
): NavigationEventState<T> {
    val state = remember { NavigationEventState(currentInfo, backInfo, forwardInfo) }
    SideEffect {
        state.currentInfo = currentInfo
        state.backInfo = backInfo
        state.forwardInfo = forwardInfo
    }
    return state
}
