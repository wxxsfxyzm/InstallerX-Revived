// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.util.pm

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.rosan.installer.domain.privileged.provider.AppOpsProvider
import com.rosan.installer.domain.settings.model.config.ConfigModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object UnknownSourcePermissionUtil : KoinComponent {
    private val appOpsProvider by inject<AppOpsProvider>()

    fun isAllowed(context: Context, packageName: String): Boolean {
        if (packageName == context.packageName) {
            return context.packageManager.canRequestPackageInstalls()
        }

        val uid = context.packageUid(packageName) ?: return false
        val mode = context.requestInstallPackagesMode(uid, packageName) ?: return false
        return mode == AppOpsManager.MODE_ALLOWED
    }

    suspend fun prepareSettingsToggle(context: Context, packageName: String, config: ConfigModel) {
        val uid = context.packageUid(packageName) ?: return
        prepareRequestInstallPackagesOp(context, config, uid, packageName)
    }

    suspend fun checkAndPrepare(context: Context, config: ConfigModel, uid: Int, packageName: String): Boolean {
        val mode = prepareRequestInstallPackagesOp(context, config, uid, packageName)
            ?: context.requestInstallPackagesMode(uid, packageName)
            ?: return true
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private suspend fun prepareRequestInstallPackagesOp(
        context: Context,
        config: ConfigModel,
        uid: Int,
        packageName: String
    ): Int? = if (packageName == context.packageName) {
        context.requestInstallPackagesMode(uid, packageName)
    } else {
        appOpsProvider.prepareUnknownSourceAppOp(
            authorizer = config.authorizer,
            customizeAuthorizer = config.customizeAuthorizer,
            uid = uid,
            packageName = packageName
        )
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
}
