// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred

import androidx.annotation.StringRes

sealed class PreferredViewEvent {
    data class ShowDefaultInstallerResult(
        @param:StringRes val messageResId: Int
    ) : PreferredViewEvent()

    data class ShowDefaultInstallerErrorDetail(
        @param:StringRes val titleResId: Int,
        val exception: Throwable,
        val retryAction: PreferredViewAction
    ) : PreferredViewEvent()
}
