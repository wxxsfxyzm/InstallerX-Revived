// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.data.engine.executor.appinstaller

import android.content.Context
import android.os.IBinder
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.installer.core.reflection.ReflectionProvider
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.privileged.provider.PostInstallTaskProvider
import com.rosan.installer.framework.privileged.core.execution.authorization.requireDhizukuPermissionGranted

class DhizukuAppInstallerRepoImpl(
    context: Context, reflect: ReflectionProvider, capabilityProvider: DeviceCapabilityProvider, postInstallTaskProvider: PostInstallTaskProvider
) : IBinderAppInstallerRepoImpl(context, reflect, capabilityProvider, postInstallTaskProvider) {
    override suspend fun iBinderWrapper(iBinder: IBinder): IBinder =
        requireDhizukuPermissionGranted {
            Dhizuku.binderWrapper(iBinder)
        }
}
