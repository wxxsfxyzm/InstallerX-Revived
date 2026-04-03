// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.animation.predictiveback

/**
 * Defines the direction of the page exit animation.
 */
enum class PredictiveExitDirection {
    /** Follows the user's swipe gesture direction (e.g., swipe left -> exit right). */
    FOLLOW_GESTURE,

    /** Always translates to the right, regardless of swipe edge. */
    ALWAYS_RIGHT,

    /** Always translates to the left, regardless of swipe edge. */
    ALWAYS_LEFT
}
