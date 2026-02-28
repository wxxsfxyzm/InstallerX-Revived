// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.app.model.exception

import com.rosan.installer.R

class AnalyseFailedCorruptedArchiveException : InstallerException {
    // Basic constructor
    constructor() : super()

    // Constructor with message only
    constructor(message: String?) : super(message)

    // Constructor with message and cause
    constructor(message: String?, cause: Throwable?) : super(message, cause)

    // Constructor with cause only
    constructor(cause: Throwable?) : super(cause)

    override fun getStringResId(): Int {
        return R.string.installer_analyse_failed_corrupted_archive
    }
}