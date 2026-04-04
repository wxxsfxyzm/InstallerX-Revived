// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.model

/**
 * Defines the direction of the page exit animation.
 */
enum class PredictiveBackExitDirection(val value: String) {
    /** Follows the user's swipe gesture direction (e.g., swipe left -> exit right). */
    FOLLOW_GESTURE("follow_gesture"),

    /** Always translates to the right, regardless of swipe edge. */
    ALWAYS_RIGHT("always_right"),

    /** Always translates to the left, regardless of swipe edge. */
    ALWAYS_LEFT("always_left")
}
