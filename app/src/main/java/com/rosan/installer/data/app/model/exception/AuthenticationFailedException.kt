// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.app.model.exception

import com.rosan.installer.R

class AuthenticationFailedException : InstallerException {
    constructor() : super()

    constructor(message: String?) : super(message)

    override fun getStringResId(): Int {
        return R.string.exception_authentication_failed
    }
}