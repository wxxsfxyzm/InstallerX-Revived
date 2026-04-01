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

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.NavigationEventTransitionState.Idle

/**
 * This class serves as the Compose-layer adapter for the navigation event system. It holds the
 * developer-defined history partitions ([currentInfo], [backInfo], [forwardInfo]) and is updated
 * with the local [transitionState] by the [NavigationEventHandler] it is provided to.
 *
 * This object is created via [rememberNavigationEventState] and consumed by
 * [NavigationEventHandler] to link the hoisted history state with the active handler's callbacks
 * and gesture state.
 *
 * @see androidx.navigationevent.compose.NavigationEventHandler
 */
@Stable
class NavigationEventState<T : NavigationEventInfo>
internal constructor(
    currentInfo: T,
    backInfo: List<T> = emptyList(),
    forwardInfo: List<T> = emptyList(),
) {

    /**
     * The current physical gesture state from the dispatcher. This value is collected from the
     * local [NavigationEventHandler] and will be either [NavigationEventTransitionState.Idle] or
     * [NavigationEventTransitionState.InProgress]. This property will update frequently during a
     * gesture.
     */
    var transitionState: NavigationEventTransitionState by mutableStateOf(Idle)

    /** History partitions relative to the current position. */

    /** A list of destinations the user may navigate back to. */
    var backInfo: List<T> by mutableStateOf(backInfo)

    /** The contextual information for the currently active destination. */
    var currentInfo: T by mutableStateOf(currentInfo)

    /** A list of destinations the user may navigate forward to. */
    var forwardInfo: List<T> by mutableStateOf(forwardInfo)

    /**
     * The internal handler instance associated with this state object. This handler is created and
     * remembered by [rememberNavigationEventState] and is registered with the dispatcher when
     * passed to [NavigationEventHandler]. This guarantees the link between the hoisted state and
     * the active handler.
     */
    var sourceHandler: NavigationEventHandler<out NavigationEventInfo>? = null
}
