// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.recycle.model.exception

import com.rosan.installer.R
import com.rosan.installer.data.app.model.exception.InstallerException

class ShizukuNotWorkException : InstallerException {
    constructor() : super()

    constructor(message: String?) : super(message)

    constructor(cause: Throwable?) : super(cause)

    constructor(message: String?, cause: Throwable?) : super(message, cause)

    override fun getStringResId(): Int {
        return R.string.exception_shizuku_not_work
    }
}
