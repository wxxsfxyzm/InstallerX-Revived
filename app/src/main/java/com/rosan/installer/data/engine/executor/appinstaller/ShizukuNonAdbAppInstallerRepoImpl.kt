// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
// Based on the safe-mode installation backend in vvb2060/PackageInstaller:
// https://github.com/vvb2060/PackageInstaller
package com.rosan.installer.data.engine.executor.appinstaller

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.Session
import android.content.pm.PackageManagerHidden
import android.os.Build
import android.os.Process
import com.rosan.installer.core.bitmask.removeFlag
import com.rosan.installer.core.reflection.ReflectionProvider
import com.rosan.installer.data.engine.executor.PackageInstallerUtil.installFlags
import com.rosan.installer.data.engine.executor.PackageManagerUtil
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.engine.exception.InstallException
import com.rosan.installer.domain.engine.model.error.InstallErrorType
import com.rosan.installer.domain.privileged.provider.PostInstallTaskProvider
import com.rosan.installer.domain.settings.model.config.ConfigModel
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.framework.privileged.core.execution.authorization.requireShizukuPermissionGranted
import timber.log.Timber

/**
 * Uses the app UID for the entire session lifecycle, as in PackageInstaller's safe mode.
 * Only verification preparation and user approval use Shizuku. Creating the session via
 * shell/root would cause PackageInstallerService to add INSTALL_FROM_ADB again.
 */
class ShizukuNonAdbAppInstallerRepoImpl(
    context: Context,
    reflect: ReflectionProvider,
    capabilityProvider: DeviceCapabilityProvider,
    postInstallTaskProvider: PostInstallTaskProvider,
    appSettingsRepo: AppSettingsRepository,
) : ShizukuAppInstallerRepoImpl(context, reflect, capabilityProvider, postInstallTaskProvider, appSettingsRepo) {
    override suspend fun resolveInstallerPackageName(config: ConfigModel): String = context.packageName

    override suspend fun getPackageInstaller(config: ConfigModel): PackageInstaller {
        validateNonAdbInstallUser(
            currentUserId = Process.myUid() / 100000,
            targetUserId = config.targetUserId.takeIf { config.enableCustomizeUser },
            installFlags = config.installFlags,
        )
        return requireShizukuPermissionGranted { context.packageManager.packageInstaller }
    }

    // Keep open/write/commit/abandon under the app UID; do not wrap mSession in Shizuku.
    override suspend fun setSessionIBinder(session: Session) = Unit

    override suspend fun prepareSessionCreation(
        config: ConfigModel,
        params: PackageInstaller.SessionParams,
        respectPlatformInstallPolicy: Boolean,
    ) {
        params.installFlags = nonAdbInstallFlags(params.installFlags)
        if (!respectPlatformInstallPolicy && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // A one-off request for this app's next session, not a persistent global setting.
            resolvePackageInstallerBinder().disableVerificationForUid(Process.myUid())
        }
        Timber.i("Using app-owned non-ADB installation session with Shizuku approval")
    }

    @SuppressLint("RequestInstallPackagesPolicy")
    override suspend fun commit(config: ConfigModel, sessionId: Int, session: Session) {
        val receiver = LocalIntentReceiver(reflect)
        session.commit(receiver.getIntentSender())
        var approved = false
        PackageManagerUtil.installResultVerify(context, receiver) {
            check(!approved) { "Session $sessionId still requires user action after Shizuku approval" }
            approveSession(config, sessionId, true)
            approved = true
        }
    }
}

// Shizuku approval does not give the app-owned session permission to grant runtime permissions.
internal fun nonAdbInstallFlags(flags: Int): Int = flags
    .removeFlag(PackageManagerHidden.INSTALL_FROM_ADB)
    .removeFlag(PackageManagerHidden.INSTALL_GRANT_ALL_REQUESTED_PERMISSIONS)

internal fun validateNonAdbInstallUser(currentUserId: Int, targetUserId: Int?, installFlags: Int) {
    if ((targetUserId != null && targetUserId != currentUserId) ||
        installFlags and PackageManagerHidden.INSTALL_ALL_USERS != 0
    ) {
        throw InstallException(
            InstallErrorType.NON_ADB_UNSUPPORTED_USER,
            "App-owned installation sessions cannot target another user or all users",
        )
    }
}
