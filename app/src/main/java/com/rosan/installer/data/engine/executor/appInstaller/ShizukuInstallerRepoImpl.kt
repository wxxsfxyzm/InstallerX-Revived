// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.data.engine.executor.appInstaller

import android.os.IBinder
import com.rosan.installer.data.privileged.util.requireShizukuPermissionGranted
import rikka.shizuku.ShizukuBinderWrapper

object ShizukuInstallerRepoImpl : IBinderInstallerRepoImpl() {
    override suspend fun iBinderWrapper(iBinder: IBinder): IBinder =
        requireShizukuPermissionGranted {
            ShizukuBinderWrapper(iBinder)
        }
}