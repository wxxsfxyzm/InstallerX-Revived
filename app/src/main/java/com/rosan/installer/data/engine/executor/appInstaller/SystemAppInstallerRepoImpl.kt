// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.executor.appInstaller

import android.content.Context
import android.os.IBinder
import com.rosan.installer.core.reflection.ReflectionProvider

import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.privileged.provider.PostInstallTaskProvider

class SystemAppInstallerRepoImpl(
    context: Context, reflect: ReflectionProvider, capabilityProvider: DeviceCapabilityProvider, postInstallTaskProvider: PostInstallTaskProvider
) : IBinderAppInstallerRepoImpl(context, reflect, capabilityProvider, postInstallTaskProvider) {
    override suspend fun iBinderWrapper(iBinder: IBinder): IBinder = iBinder
}
