// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.auxiliaryinstall

data class AuxiliaryInstallSettingsState(
    val autoConfirmUsbInstall: Boolean = false,
    val showToast: Boolean = false,
    val delayedRetry: Boolean = true,
    val requireScreenOn: Boolean = true,
    val accessibilityServiceEnabled: Boolean = false
)
