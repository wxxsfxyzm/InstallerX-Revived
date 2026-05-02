// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.auxiliaryinstall

import androidx.annotation.StringRes

sealed class AuxiliaryInstallSettingsEvent {
    data class ShowMessage(@StringRes val resId: Int) : AuxiliaryInstallSettingsEvent()
}
