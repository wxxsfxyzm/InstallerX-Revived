// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.about

import android.net.Uri

sealed class AboutEvent {
    data object ShowUpdateLoading : AboutEvent()
    data object HideUpdateLoading : AboutEvent()
    data class ShowInAppUpdateErrorDetail(val title: String, val exception: Throwable) : AboutEvent()
    data class OpenLogShare(val uri: Uri) : AboutEvent()
    data class ShareLogFailed(val error: String) : AboutEvent()
}
