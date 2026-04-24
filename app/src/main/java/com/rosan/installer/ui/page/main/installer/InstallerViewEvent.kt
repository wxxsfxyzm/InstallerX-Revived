// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer

import androidx.annotation.StringRes

sealed interface InstallerViewEvent {
    /**
     * Show a simple toast message using a String.
     */
    data class ShowToast(val message: String) : InstallerViewEvent

    /**
     * Show a simple toast message using a string resource ID.
     */
    data class ShowToastRes(@param:StringRes val messageResId: Int) : InstallerViewEvent

    /**
     * Show an error toast message using a Throwable.
     */
    data class ShowErrorToast(val error: Throwable) : InstallerViewEvent

    /**
     * Share a file using its original content URI.
     */
    data class ShareFile(val uriString: String, val mimeType: String) : InstallerViewEvent
}
