// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.framework.notification.builder

import com.rosan.installer.domain.session.model.ProgressEntity

/**
 * A layered payload containing all necessary context for building a notification.
 * This ensures that all UI builders receive a consistent snapshot of the state.
 */
data class NotificationPayload(
    val state: InstallState,
    val settings: UserSettings,
    val animation: AnimationContext
)

data class InstallState(
    val progress: ProgressEntity,
    val background: Boolean,
    val isSameState: Boolean
)

data class UserSettings(
    val showDialog: Boolean,
    val preferSystemIcon: Boolean,
    val preferDynamicColor: Boolean,
    val miIslandOuterGlow: Boolean
)

data class AnimationContext(
    val fakeItemProgress: Float
)
