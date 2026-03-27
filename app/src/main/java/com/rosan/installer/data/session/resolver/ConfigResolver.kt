// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.session.resolver

import android.app.Activity
import android.content.Intent
import com.rosan.installer.domain.engine.model.InstallOption
import com.rosan.installer.domain.settings.model.ConfigModel
import com.rosan.installer.domain.settings.usecase.config.GetResolvedConfigUseCase
import com.rosan.installer.ui.activity.UninstallerActivity
import com.rosan.installer.util.addFlag
import timber.log.Timber

class ConfigResolver(
    private val getResolvedConfigUseCase: GetResolvedConfigUseCase
) {
    companion object {
        private const val TAG = "InstallSource"
    }

    // Authorities that definitely belong to the system but don't follow the "com.android.providers" naming convention.
    private val EXPLICIT_SYSTEM_AUTHORITIES = setOf(
        "media",
        "com.android.externalstorage.documents"
    )

    suspend fun resolve(activity: Activity): ConfigModel {
        Timber.tag(TAG).d("resolveConfig: Starting.")
        // 0. Special Check: Is this UninstallerActivity?
        if (activity is UninstallerActivity) {
            Timber.tag(TAG).d("Activity is UninstallerActivity. Returning default config immediately.")
            return getConfigForPackage(null)
        }

        // 1. Check Calling Package
        val callingPackage = activity.callingPackage
        Timber.tag(TAG).d("activity.callingPackage: $callingPackage")
        if (callingPackage != null) {
            return getConfigForPackage(callingPackage)
        }

        // 2. Check Referrer
        // Remove custom referer first
        val intent = activity.intent
        intent.removeExtra(Intent.EXTRA_REFERRER_NAME)
        intent.removeExtra(Intent.EXTRA_REFERRER)
        // Now that the custom referrers are removed, it should return the real referrer.
        val referrer = activity.referrer
        Timber.tag(TAG).d("activity.referrer: $referrer")
        if (referrer?.scheme == "android-app" && referrer.host != null) {
            val referrerPackage = referrer.host
            Timber.tag(TAG).d("Valid app referrer found: $referrerPackage")
            return getConfigForPackage(referrerPackage)
        } else if (referrer != null) {
            Timber.tag(TAG).w("Ignoring referrer with non-app scheme: ${referrer.scheme}")
        }

        // 3. Check URI Authority
        val authority = activity.intent.data?.authority ?: activity.intent.clipData?.getItemAt(0)?.uri?.authority
        Timber.tag(TAG).d("URI authority: $authority")

        if (authority != null) {
            if (isSystemAuthority(authority)) {
                Timber.tag(TAG).w("Authority '$authority' is identified as a system provider. Using default config.")
                // Load default config from database (passing null to getByPackageName)
                return getConfigForPackage(null)
            } else {
                val authorityPackage = extractPackageFromAuthority(authority)
                Timber.tag(TAG).d("Package extracted from app-specific authority: $authorityPackage")
                return getConfigForPackage(authorityPackage)
            }
        }

        Timber.tag(TAG).w("Could not determine calling package. Using default config.")
        return getConfigForPackage(null)
    }

    /**
     * Checks if the authority belongs to a generic system provider.
     * Matches explicit list or any authority starting with "com.android.providers".
     */
    private fun isSystemAuthority(authority: String): Boolean {
        return authority in EXPLICIT_SYSTEM_AUTHORITIES ||
                authority.startsWith("com.android.providers")
    }

    private suspend fun getConfigForPackage(packageName: String?): ConfigModel {
        var config = getResolvedConfigUseCase(packageName)

        val initialInstallFlags = listOfNotNull(
            config.allowTestOnly.takeIf { it }?.let { InstallOption.AllowTest.value },
            config.allowDowngrade.takeIf { it }?.let { InstallOption.AllowDowngrade.value },
            config.forAllUser.takeIf { it }?.let { InstallOption.AllUsers.value },
            config.bypassLowTargetSdk.takeIf { it }?.let { InstallOption.BypassLowTargetSdkBlock.value },
            config.allowAllRequestedPermissions.takeIf { it }?.let { InstallOption.GrantAllRequestedPermissions.value },
            config.requestUpdateOwnership.takeIf { it }?.let { InstallOption.RequestUpdateOwnerShip.value },
        ).fold(0) { acc, flag -> acc or flag }

        config = config.copy(
            installFlags = config.installFlags.addFlag(initialInstallFlags)
        )

        Timber.tag(TAG).d("Resolved config for '${packageName ?: "default"}': $config")
        return config
    }

    private fun extractPackageFromAuthority(authority: String): String {
        return authority.removeSuffix(".FileProvider")
            .removeSuffix(".provider")
    }
}