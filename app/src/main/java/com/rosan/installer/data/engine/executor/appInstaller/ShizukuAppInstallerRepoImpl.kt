// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.data.engine.executor.appInstaller

import android.content.Context
import android.os.IBinder
import com.rosan.installer.core.reflection.ReflectionProvider
import com.rosan.installer.data.privileged.util.requireShizukuPermissionGranted

import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.privileged.provider.PostInstallTaskProvider
import rikka.shizuku.ShizukuBinderWrapper

class ShizukuAppInstallerRepoImpl(
    context: Context, reflect: ReflectionProvider, capabilityProvider: DeviceCapabilityProvider, postInstallTaskProvider: PostInstallTaskProvider
) : IBinderAppInstallerRepoImpl(context, reflect, capabilityProvider, postInstallTaskProvider) {
    override suspend fun iBinderWrapper(iBinder: IBinder): IBinder = requireShizukuPermissionGranted { ShizukuBinderWrapper(iBinder) }
}