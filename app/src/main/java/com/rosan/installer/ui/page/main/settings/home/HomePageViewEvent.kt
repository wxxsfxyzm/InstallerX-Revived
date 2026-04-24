// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.home

import androidx.annotation.StringRes

sealed interface HomePageViewEvent {
    data class ShowDefaultInstallerResult(@param:StringRes val messageResId: Int) : HomePageViewEvent
    data class ShowDefaultInstallerErrorDetail(
        @param:StringRes val titleResId: Int,
        val exception: Throwable,
        val retryAction: HomePageViewAction
    ) : HomePageViewEvent
}
