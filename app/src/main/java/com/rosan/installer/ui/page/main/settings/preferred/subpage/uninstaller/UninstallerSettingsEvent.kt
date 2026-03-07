// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.subpage.uninstaller

import androidx.annotation.StringRes

sealed class UninstallerSettingsEvent {
    data class ShowMessage(@param:StringRes val resId: Int) : UninstallerSettingsEvent()
}
