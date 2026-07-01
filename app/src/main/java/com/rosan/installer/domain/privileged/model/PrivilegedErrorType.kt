// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.privileged.model

import androidx.annotation.StringRes
import com.rosan.installer.R

enum class PrivilegedErrorType(@param:StringRes val stringResId: Int) {
    APP_PROCESS_NOT_WORK(R.string.exception_app_process_not_work),
    CUSTOM_AUTHORIZER_EMPTY(R.string.config_error_customize_authorizer),
    DHIZUKU_DEAD_SERVICE(R.string.exception_dhizuku_dead_service),
    DHIZUKU_NOT_WORK(R.string.exception_dhizuku_not_work),
    ROOT_NOT_WORK(R.string.exception_root_not_work),
    SHIZUKU_NOT_WORK(R.string.exception_shizuku_not_work)
}
