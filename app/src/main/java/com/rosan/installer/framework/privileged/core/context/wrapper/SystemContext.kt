// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.framework.privileged.core.context.wrapper

import android.content.AttributionSource
import android.content.Context
import android.content.ContextWrapper
import android.os.Build

class SystemContext(base: Context) : ContextWrapper(base) {
    override fun getOpPackageName() = "android"

    override fun getAttributionSource(): AttributionSource {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val builder = AttributionSource.Builder(1000)
                .setPackageName("android")
            return builder.build()
        }
        return super.getAttributionSource()
    }
}
