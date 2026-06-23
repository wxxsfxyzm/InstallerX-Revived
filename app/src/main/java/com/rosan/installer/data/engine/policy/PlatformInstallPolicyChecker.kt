// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.policy

import android.Manifest
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserManager
import com.rosan.installer.core.reflection.ReflectionProvider
import com.rosan.installer.core.reflection.getValue
import com.rosan.installer.domain.engine.exception.InstallException
import com.rosan.installer.domain.engine.model.error.InstallErrorType
import com.rosan.installer.domain.settings.model.config.ConfigModel
import com.rosan.installer.util.pm.UnknownSourcePermissionUtil

class PlatformInstallPolicyChecker(
    private val context: Context,
    private val reflect: ReflectionProvider
) {
    private companion object {
        const val DOWNLOADS_AUTHORITY = "downloads"
        const val PRIVATE_FLAG_PRIVILEGED = 1 shl 3
    }

    suspend fun check(config: ConfigModel) {
        val callerPackage = config.initiatorPackageName
        val callerUid = config.installSourceUid ?: callerPackage?.let { packageUid(it) }
        val appOpPackage = callerPackage ?: callerUid?.let { packageNameForUid(it) }
        val trustedSource = (
                callerUid != null && hasPermission(Manifest.permission.INSTALL_PACKAGES, callerUid)
                ) || (
                callerPackage != null && config.notUnknownSource && isPrivilegedPackage(callerPackage)
                )
        val exemptFromRequestPermission = callerUid != null && (
                hasPermission(Manifest.permission.MANAGE_DOCUMENTS, callerUid) ||
                        isSystemDownloadsProvider(callerUid)
                )

        if (callerUid != null &&
            appOpPackage != context.packageName &&
            !trustedSource &&
            !exemptFromRequestPermission
        ) {
            checkInstallPermissionRequested(callerUid)
        }
        if (callerUid != null && appOpPackage != null && appOpPackage != context.packageName && !trustedSource) {
            checkUnknownSourceAllowed(config, callerUid, appOpPackage)
        }

        checkUserRestrictions(trustedSource)
    }

    private fun hasPermission(permission: String, uid: Int): Boolean =
        context.checkPermission(permission, -1, uid) == PackageManager.PERMISSION_GRANTED

    private fun isPrivilegedPackage(packageName: String): Boolean {
        val appInfo = applicationInfo(packageName) ?: return false
        val privateFlags = reflect.getValue<Int>(appInfo, "privateFlags") ?: 0
        return privateFlags and PRIVATE_FLAG_PRIVILEGED != 0
    }

    private fun checkInstallPermissionRequested(uid: Int) {
        val targetSdkVersion = maxTargetSdkVersionForUid(uid)
            ?: throw missingPermission("Cannot resolve target sdk for install requester uid $uid")

        if (targetSdkVersion >= Build.VERSION_CODES.O && !isUidRequestingPermission(
                uid,
                Manifest.permission.REQUEST_INSTALL_PACKAGES
            )
        ) {
            throw missingPermission(
                "Uid $uid does not declare ${Manifest.permission.REQUEST_INSTALL_PACKAGES}"
            )
        }
    }

    private suspend fun checkUnknownSourceAllowed(config: ConfigModel, uid: Int, packageName: String) {
        if (!UnknownSourcePermissionUtil.checkAndPrepare(context, config, uid, packageName)) {
            throw missingPermission(
                "$packageName is not allowed for ${Manifest.permission.REQUEST_INSTALL_PACKAGES}"
            )
        }
    }

    private fun checkUserRestrictions(trustedSource: Boolean) {
        val userManager = context.getSystemService(UserManager::class.java) ?: return
        val restrictions = if (trustedSource) {
            listOf(UserManager.DISALLOW_INSTALL_APPS)
        } else {
            buildList {
                add(UserManager.DISALLOW_INSTALL_APPS)
                add(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY)
                }
            }
        }

        val restriction = restrictions.firstOrNull { userManager.hasUserRestriction(it) } ?: return
        throw InstallException(
            InstallErrorType.USER_RESTRICTED,
            "Install blocked by user restriction: $restriction"
        )
    }

    private fun packageUid(packageName: String): Int? =
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageUid(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageUid(packageName, 0)
            }
        }.getOrNull()

    private fun packageNameForUid(uid: Int): String? =
        context.packageManager.getPackagesForUid(uid)?.firstOrNull()

    private fun packageRequestedPermissions(packageName: String): List<String> =
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            }.requestedPermissions.orEmpty()
        }.getOrElse { emptyArray() }
            .toList()

    private fun maxTargetSdkVersionForUid(uid: Int): Int? {
        val packages = context.packageManager.getPackagesForUid(uid)?.filterNotNull().orEmpty()
        if (packages.isEmpty()) return null
        return packages.mapNotNull { packageName ->
            applicationInfo(packageName)?.targetSdkVersion
        }.maxOrNull()
    }

    private fun isUidRequestingPermission(uid: Int, permission: String): Boolean {
        val packages = context.packageManager.getPackagesForUid(uid)?.filterNotNull().orEmpty()
        return packages.any { packageName -> permission in packageRequestedPermissions(packageName) }
    }

    private fun isSystemDownloadsProvider(uid: Int): Boolean {
        val providerInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.resolveContentProvider(
                DOWNLOADS_AUTHORITY,
                PackageManager.ComponentInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.resolveContentProvider(DOWNLOADS_AUTHORITY, 0)
        } ?: return false

        val appInfo = providerInfo.applicationInfo ?: return false
        return appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0 && uid == appInfo.uid
    }

    private fun applicationInfo(packageName: String): ApplicationInfo? =
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getApplicationInfo(packageName, 0)
            }
        }.getOrNull()

    private fun missingPermission(message: String): InstallException =
        InstallException(InstallErrorType.MISSING_INSTALL_PERMISSION, message)
}
