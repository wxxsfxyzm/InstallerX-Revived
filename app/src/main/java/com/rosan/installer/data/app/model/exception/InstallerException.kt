// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.app.model.exception

/**
 * The root exception of InstallerX-Revived
 * Every Custom Exception should extend this Exception
 */
abstract class InstallerException : RuntimeException {
    constructor(): super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)

    /**
     * Return the user-friendly error string resource id of the exception
     * @return string resource id
     */
    abstract fun getStringResId() : Int
}