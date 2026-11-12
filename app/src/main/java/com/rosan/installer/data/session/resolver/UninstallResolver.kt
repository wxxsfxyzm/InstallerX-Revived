// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.session.resolver

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstallerHidden
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.os.UserHandle
import com.rosan.installer.domain.engine.model.install.UninstallFlags
import com.rosan.installer.domain.engine.repository.AppInstallerRepository
import com.rosan.installer.domain.engine.usecase.GetAppIconColorUseCase
import com.rosan.installer.domain.session.model.UninstallInfo
import com.rosan.installer.domain.settings.model.config.ConfigModel
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.settings.repository.BooleanSetting
import kotlinx.coroutines.flow.first

class UninstallResolver(
    private val context: Context,
    private val configResolver: ConfigResolver,
    private val appSettingsRepo: AppSettingsRepository,
    private val getAppColor: GetAppIconColorUseCase,
    private val appInstaller: AppInstallerRepository,
) {
    data class Result(val config: ConfigModel, val uninstallInfo: UninstallInfo)

    suspend fun resolve(activity: Activity, sessionId: String, packageName: String): Result {
        val userId = resolveUninstallUser(activity.intent)
        val resolvedConfig = configResolver.resolve(activity)
        val pm = context.packageManager
        val packageInfo = appInstaller.getUninstallPackageInfo(
            config = resolvedConfig,
            packageName = packageName,
            userId = userId,
            flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                PackageManager.MATCH_ARCHIVED_PACKAGES
            } else {
                0L
            },
        )
        val appInfo = checkNotNull(packageInfo.applicationInfo) { "Missing application info for $packageName" }
        val isArchived = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM && appInfo.isArchived

        val requestedUninstallFlags = resolveRequestedUninstallFlags(activity)
        val config = if (requestedUninstallFlags != 0) {
            resolvedConfig.copy(
                uninstallFlags = resolvedConfig.uninstallFlags or requestedUninstallFlags,
            )
        } else {
            resolvedConfig
        }

        val color = if (appSettingsRepo.getBoolean(BooleanSetting.UiDynColorFollowPkgIcon, false).first()) {
            getAppColor(
                sessionId = sessionId,
                packageName = packageName,
                preferSystemIcon = true,
            )
        } else {
            null
        }

        return Result(
            config = config,
            uninstallInfo = UninstallInfo(
                packageName = packageName,
                userId = userId,
                appLabel = pm.getApplicationLabel(appInfo).toString(),
                versionName = packageInfo.versionName,
                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                },
                isArchived = isArchived,
                seedColor = color,
            ),
        )
    }

    private fun resolveRequestedUninstallFlags(activity: Activity): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return 0

        val requestDeleteFlags = activity.intent.getIntExtra(PackageInstallerHidden.EXTRA_DELETE_FLAGS, 0)
        val allowedRequestFlags = UninstallFlags.DELETE_ARCHIVE or UninstallFlags.DELETE_KEEP_DATA
        return requestDeleteFlags and allowedRequestFlags
    }
}

/** The requested user takes precedence over the process user, never the installation profile. */
internal fun resolveUninstallUser(intent: Intent): Int {
    val hasUserExtra = intent.hasExtra(Intent.EXTRA_USER)

    val user = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getParcelableExtra(Intent.EXTRA_USER, UserHandle::class.java)
    } else {
        @Suppress("DEPRECATION")
        intent.getParcelableExtra<UserHandle>(Intent.EXTRA_USER)
    }
    // UserHandle.hashCode() returns its user ID, including on older platform versions.
    return resolveUninstallUserId(Process.myUid() / 100000, hasUserExtra, user?.hashCode())
}

internal fun resolveUninstallUserId(currentUserId: Int, hasUserExtra: Boolean, requestedUserId: Int?): Int {
    val userId = if (hasUserExtra) requireNotNull(requestedUserId) { "Invalid uninstall user extra" } else currentUserId
    require(userId >= 0) { "Invalid uninstall user: $userId" }
    return userId
}
