// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.history

sealed interface HistoryViewAction {
    data object ClearHistory : HistoryViewAction

    data class SetHistoryEnabled(val enabled: Boolean, val clearHistory: Boolean = false) : HistoryViewAction

    data class SetIndicatorsEnabled(val enabled: Boolean) : HistoryViewAction
}
