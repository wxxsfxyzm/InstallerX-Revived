// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.session.resolver

import android.app.Activity
import android.content.Context
import android.content.pm.PackageInstallerHidden
import android.content.pm.PackageManager
import android.os.Build
import com.rosan.installer.domain.engine.model.install.UninstallFlags
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
    private val getAppColor: GetAppIconColorUseCase
) {
    data class Result(
        val config: ConfigModel,
        val uninstallInfo: UninstallInfo
    )

    suspend fun resolve(
        activity: Activity,
        sessionId: String,
        packageName: String
    ): Result {
        val resolvedConfig = configResolver.resolve(activity)
        val pm = context.packageManager
        var isArchived = false
        val appInfo = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
        }.getOrElse {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) throw it

            pm.getApplicationInfo(
                packageName,
                PackageManager.ApplicationInfoFlags.of(PackageManager.MATCH_ARCHIVED_PACKAGES)
            ).also { archivedInfo ->
                if (!archivedInfo.isArchived) throw it
                isArchived = true
            }
        }
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(
                    if (isArchived && Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM)
                        PackageManager.MATCH_ARCHIVED_PACKAGES
                    else
                        0
                )
            )
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, 0)
        }

        val requestedUninstallFlags = resolveRequestedUninstallFlags(activity)
        val config = if (requestedUninstallFlags != 0) {
            resolvedConfig.copy(
                uninstallFlags = resolvedConfig.uninstallFlags or requestedUninstallFlags
            )
        } else {
            resolvedConfig
        }

        val color = if (appSettingsRepo.getBoolean(BooleanSetting.UiDynColorFollowPkgIcon, false).first()) {
            getAppColor(
                sessionId = sessionId,
                packageName = packageName,
                preferSystemIcon = true
            )
        } else {
            null
        }

        return Result(
            config = config,
            uninstallInfo = UninstallInfo(
                packageName = packageName,
                appLabel = pm.getApplicationLabel(appInfo).toString(),
                versionName = packageInfo.versionName,
                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                },
                isArchived = isArchived,
                seedColor = color
            )
        )
    }

    private fun resolveRequestedUninstallFlags(activity: Activity): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return 0

        val requestDeleteFlags = activity.intent.getIntExtra(PackageInstallerHidden.EXTRA_DELETE_FLAGS, 0)
        val allowedRequestFlags = UninstallFlags.DELETE_ARCHIVE or UninstallFlags.DELETE_KEEP_DATA
        return requestDeleteFlags and allowedRequestFlags
    }
}
