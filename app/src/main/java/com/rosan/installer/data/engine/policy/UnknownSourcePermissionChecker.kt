// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.policy

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.rosan.installer.domain.privileged.provider.AppOpsProvider
import com.rosan.installer.domain.settings.model.config.ConfigModel
import timber.log.Timber

class UnknownSourcePermissionChecker(
    private val context: Context,
    private val appOpsProvider: AppOpsProvider
) {
    fun isAllowed(packageName: String): Boolean {
        if (packageName == context.packageName) {
            val allowed = context.packageManager.canRequestPackageInstalls()
            Timber.tag(TAG).d("Self unknown-source permission allowed=$allowed")
            return allowed
        }

        val uid = context.packageUid(packageName)
        if (uid == null) {
            Timber.tag(TAG).w("Unable to resolve uid for $packageName while checking unknown-source permission")
            return false
        }

        val mode = context.requestInstallPackagesMode(uid, packageName)
        if (mode == null) {
            Timber.tag(TAG).w("Unable to resolve AppOps mode for $packageName/$uid")
            return false
        }
        Timber.tag(TAG).d("Unknown-source permission state for $packageName/$uid: mode=$mode")
        return mode == AppOpsManager.MODE_ALLOWED
    }

    suspend fun prepareSettingsToggle(packageName: String, config: ConfigModel) {
        val uid = context.packageUid(packageName) ?: run {
            Timber.tag(TAG).w("Unable to resolve uid for $packageName before launching settings")
            return
        }
        Timber.tag(TAG).d("Preparing settings toggle for $packageName/$uid")
        prepareRequestInstallPackagesOp(config, uid, packageName)
    }

    suspend fun checkAndPrepare(config: ConfigModel, uid: Int, packageName: String): Boolean {
        val mode = prepareRequestInstallPackagesOp(config, uid, packageName)
            ?: context.requestInstallPackagesMode(uid, packageName)
            ?: run {
                Timber.tag(TAG).w("No AppOps mode available for $packageName/$uid; allowing by compatibility fallback")
                return true
            }
        Timber.tag(TAG).d("Prepared unknown-source AppOps for $packageName/$uid: mode=$mode")
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private suspend fun prepareRequestInstallPackagesOp(
        config: ConfigModel,
        uid: Int,
        packageName: String
    ): Int? = if (packageName == context.packageName) {
        val mode = context.requestInstallPackagesMode(uid, packageName)
        Timber.tag(TAG).d("Read self request-install AppOps mode for $packageName/$uid: mode=$mode")
        mode
    } else {
        Timber.tag(TAG).d(
            "Preparing request-install AppOps via authorizer=${config.authorizer} " +
                    "for $packageName/$uid"
        )
        val mode = appOpsProvider.prepareUnknownSourceAppOp(
            authorizer = config.authorizer,
            customizeAuthorizer = config.customizeAuthorizer,
            uid = uid,
            packageName = packageName
        )
        Timber.tag(TAG).d("Privileged AppOps preparation result for $packageName/$uid: mode=$mode")
        mode
    }

    @Suppress("DEPRECATION")
    private fun Context.requestInstallPackagesMode(uid: Int, packageName: String): Int? {
        val appOps = getSystemService(AppOpsManager::class.java) ?: return null
        val op = AppOpsManager.permissionToOp(Manifest.permission.REQUEST_INSTALL_PACKAGES)
            ?: return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(op, uid, packageName)
        } else {
            appOps.checkOpNoThrow(op, uid, packageName)
        }
    }

    @Suppress("DEPRECATION")
    private fun Context.packageUid(packageName: String): Int? =
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageUid(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                packageManager.getPackageUid(packageName, 0)
            }
        }.getOrNull()

    private companion object {
        const val TAG = "UnknownSourcePermission"
    }
}
