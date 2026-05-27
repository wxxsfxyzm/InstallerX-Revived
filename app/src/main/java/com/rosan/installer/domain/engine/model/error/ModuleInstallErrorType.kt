// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.model.error

import androidx.annotation.StringRes
import com.rosan.installer.R

enum class ModuleInstallErrorType(@param:StringRes val stringResId: Int) {
    GENERIC_FAILED(R.string.exception_module_install_failed),
    INCOMPATIBLE_AUTHORIZER(R.string.exception_module_install_failed_incompatible_authorizer),
    CMD_INIT_FAILED(R.string.exception_module_cmd_init_failed),
    EXIT_CODE_NON_ZERO(R.string.exception_module_install_exit_code_non_zero)
}
