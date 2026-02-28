// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.installer.model.exception

import android.net.Uri
import com.rosan.installer.R
import com.rosan.installer.data.app.model.exception.InstallerException

data class ResolveException(
    val action: String?,
    val uris: List<Uri>,
) : InstallerException(
    "action: $action, uri: $uris"
) {
    override fun getStringResId(): Int {
        return R.string.exception_resolve_failed
    }
}