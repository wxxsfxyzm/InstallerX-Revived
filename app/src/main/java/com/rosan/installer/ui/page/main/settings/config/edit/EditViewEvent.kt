// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.config.edit

import androidx.annotation.StringRes

sealed interface EditViewEvent {
    // Support both raw string and resource ID for maximum flexibility
    data class SnackBar(
        val message: String? = null,
        @param:StringRes val messageResId: Int? = null
    ) : EditViewEvent

    data object Saved : EditViewEvent
}
