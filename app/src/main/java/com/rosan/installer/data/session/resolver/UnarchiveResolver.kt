// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.session.resolver

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.rosan.installer.domain.archive.model.UnarchiveErrorAction
import com.rosan.installer.domain.session.model.UnarchiveErrorInfo
import com.rosan.installer.domain.session.model.UnarchiveInfo
import timber.log.Timber

class UnarchiveResolver(
    private val context: Context
) {
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun resolve(
        activity: Activity,
        sessionId: String,
        packageName: String,
        intentSender: IntentSender
    ): UnarchiveInfo {
        validateCaller(activity)

        val pm = activity.packageManager
        val appInfo = pm.getApplicationInfo(
            packageName,
            PackageManager.ApplicationInfoFlags.of(PackageManager.MATCH_ARCHIVED_PACKAGES)
        )
        val installSource = pm.getInstallSourceInfo(packageName)
        val responsibleInstaller = installSource.updateOwnerPackageName
            ?: installSource.installingPackageName
            ?: activity.packageName

        val installerLabel = runCatching {
            pm.getApplicationInfo(responsibleInstaller, 0).loadLabel(pm)
        }.getOrElse {
            Timber.w(it, "[id=$sessionId] resolveUnarchive: Failed to resolve installer label for $responsibleInstaller.")
            responsibleInstaller
        }

        return UnarchiveInfo(
            packageName = packageName,
            appLabel = appInfo.loadLabel(pm),
            installerLabel = installerLabel,
            intentSender = intentSender
        )
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun start(info: UnarchiveInfo) {
        context.packageManager.packageInstaller.requestUnarchive(info.packageName, info.intentSender)
    }

    fun openErrorAction(info: UnarchiveErrorInfo) {
        when (info.status.primaryAction) {
            UnarchiveErrorAction.CONTINUE,
            UnarchiveErrorAction.CLEAR_STORAGE -> {
                val sender = info.pendingIntent?.intentSender
                if (sender != null) {
                    context.startIntentSender(
                        sender,
                        null,
                        0,
                        Intent.FLAG_ACTIVITY_NEW_TASK,
                        0
                    )
                } else if (info.status.primaryAction == UnarchiveErrorAction.CLEAR_STORAGE) {
                    context.startActivity(
                        Intent("android.intent.action.MANAGE_PACKAGE_STORAGE")
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }

            UnarchiveErrorAction.OPEN_INSTALLER_SETTINGS -> {
                val packageName = info.installerPackageName ?: return
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.fromParts("package", packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }

            UnarchiveErrorAction.CLOSE -> Unit
        }
    }

    private fun validateCaller(activity: Activity) {
        val callingUid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.launchedFromUid
        } else {
            -1
        }
        if (callingUid == Process.INVALID_UID) {
            throw SecurityException("Could not determine unarchive caller uid")
        }

        val callingPackage = activity.packageManager.getPackagesForUid(callingUid)?.firstOrNull()
            ?: throw SecurityException("Package not found for unarchive caller uid $callingUid")

        val requestedPermissions = runCatching {
            @Suppress("DEPRECATION")
            activity.packageManager.getPackageInfo(
                callingPackage,
                PackageManager.GET_PERMISSIONS
            ).requestedPermissions.orEmpty()
        }.getOrElse { emptyArray() }

        val hasRequestInstallPermission = Manifest.permission.REQUEST_INSTALL_PACKAGES in requestedPermissions
        val hasInstallPermission = activity.checkPermission(
            Manifest.permission.INSTALL_PACKAGES,
            0,
            callingUid
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasRequestInstallPermission && !hasInstallPermission) {
            throw SecurityException(
                "Uid $callingUid does not have ${Manifest.permission.REQUEST_INSTALL_PACKAGES} or ${Manifest.permission.INSTALL_PACKAGES}"
            )
        }
    }
}
