// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 22026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.installer.notification

sealed class NotificationSettingsAction {
    data class ChangeStyle(val style: NotificationStyle) : NotificationSettingsAction()
    data class ChangeAutoClearSeconds(val seconds: Int) : NotificationSettingsAction()
    data class ChangeShowDialogOnPress(val show: Boolean) : NotificationSettingsAction()
    data class ChangeMiIslandBypassRestriction(val bypass: Boolean) : NotificationSettingsAction()
    data class ChangeMiIslandOuterGlow(val glow: Boolean) : NotificationSettingsAction()
    data class ChangeMiIslandBlockingInterval(val ms: Int) : NotificationSettingsAction()
}
