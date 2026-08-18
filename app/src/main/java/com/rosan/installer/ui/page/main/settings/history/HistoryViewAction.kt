// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.history

sealed interface HistoryViewAction {
    data object ClearHistory : HistoryViewAction
    data class UpdateSearchQuery(val query: String) : HistoryViewAction
    data class SelectSearchField(val field: HistorySearchField) : HistoryViewAction
    data object ClearSearch : HistoryViewAction
}
