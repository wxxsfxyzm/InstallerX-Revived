// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.installer.model.exception

import com.rosan.installer.R
import com.rosan.installer.data.app.model.exception.InstallerException

class ResolvedFailedNoInternetAccessException : InstallerException {
    constructor() : super()

    constructor(message: String?) : super(message)

    override fun getStringResId(): Int {
        return R.string.exception_resolve_failed_no_internet_access
    }
}