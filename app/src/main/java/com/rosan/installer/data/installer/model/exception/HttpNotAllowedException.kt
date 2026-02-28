// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.installer.model.exception

import com.rosan.installer.R
import com.rosan.installer.data.app.model.exception.InstallerException

class HttpNotAllowedException(message: String) : InstallerException(message) {
    override fun getStringResId(): Int {
        return R.string.exception_http_not_allowed
    }
}