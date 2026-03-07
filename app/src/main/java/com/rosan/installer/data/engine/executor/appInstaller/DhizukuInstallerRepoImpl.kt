// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.data.engine.executor.appInstaller

import android.content.Context
import android.os.IBinder
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.installer.core.reflection.ReflectionProvider
import com.rosan.installer.data.privileged.util.deletePaths
import com.rosan.installer.data.privileged.util.requireDhizukuPermissionGranted
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.engine.model.InstallEntity
import com.rosan.installer.domain.engine.model.InstallExtraInfoEntity
import com.rosan.installer.domain.engine.model.sourcePath
import com.rosan.installer.domain.privileged.provider.PostInstallTaskProvider
import com.rosan.installer.domain.settings.model.ConfigModel
import timber.log.Timber

class DhizukuInstallerRepoImpl(
    context: Context, reflect: ReflectionProvider, capabilityProvider: DeviceCapabilityProvider, postInstallTaskProvider: PostInstallTaskProvider
) : IBinderInstallerRepoImpl(context, reflect, capabilityProvider, postInstallTaskProvider) {
    override suspend fun iBinderWrapper(iBinder: IBinder): IBinder =
        requireDhizukuPermissionGranted {
            Dhizuku.binderWrapper(iBinder)
        }

    override suspend fun onDeleteWork(
        config: ConfigModel,
        entities: List<InstallEntity>,
        extra: InstallExtraInfoEntity
    ) {
        // Extract the unique source file paths from the install entities.
        val pathsToDelete = entities.sourcePath()
        if (pathsToDelete.isEmpty()) {
            Timber.tag("onDeleteWork").w("Dhizuku: No source paths found to delete.")
            return
        }

        Timber.tag("onDeleteWork").d("Dhizuku: Attempting to delete paths using standard API: ${pathsToDelete.joinToString()}")

        // Use the deletePaths utility function which handles file deletion using standard APIs,
        // making it suitable for the non-privileged context of app process.
        deletePaths(pathsToDelete)
    }
}