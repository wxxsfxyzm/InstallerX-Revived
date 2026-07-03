// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.framework.privileged.core.context.wrapper

import android.content.AttributionSource
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import rikka.shizuku.Shizuku

class ShizukuContext(base: Context) : ContextWrapper(base) {
    override fun getOpPackageName() = "com.android.shell"

    override fun getAttributionSource(): AttributionSource {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val shellUid = Shizuku.getUid()
            val builder = AttributionSource.Builder(shellUid)
                .setPackageName("com.android.shell")

            if (Build.VERSION.SDK_INT >= 34) {
                builder.setPid(android.os.Process.INVALID_PID)
            }

            return builder.build()
        }
        return super.getAttributionSource()
    }
}
