// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.installer.notification

data class NotificationSettingsState(
    val showLiveActivity: Boolean = false,
    val showMiIsland: Boolean = false,
    val successAutoClearSeconds: Int = 10,
    val showDialogOnPress: Boolean = false,
    val miIslandBlockingInterval: Int = 100
) {
    val currentStyle: NotificationStyle
        get() = when {
            showMiIsland -> NotificationStyle.MI_ISLAND
            showLiveActivity -> NotificationStyle.LIVE_ACTIVITY
            else -> NotificationStyle.STANDARD
        }
}
