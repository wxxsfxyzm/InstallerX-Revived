// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.history

import com.rosan.installer.domain.history.model.OperationHistoryModel

data class HistoryViewState(
    val records: List<OperationHistoryModel> = emptyList(),
    val isLoading: Boolean = true,
    val isSystemApp: Boolean = false
)
