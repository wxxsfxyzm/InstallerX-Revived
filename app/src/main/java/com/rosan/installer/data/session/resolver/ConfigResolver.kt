// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.session.resolver

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentHidden
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManagerHidden
import android.content.pm.ProviderInfo
import android.os.Build
import android.os.Process
import com.rosan.installer.core.app.ActivityContracts
import com.rosan.installer.core.bitmask.addFlag
import com.rosan.installer.domain.engine.model.install.InstallOption
import com.rosan.installer.domain.session.model.InstallSourceConfidence
import com.rosan.installer.domain.settings.model.config.ConfigModel
import com.rosan.installer.domain.settings.usecase.config.GetResolvedConfigUseCase
import com.rosan.installer.util.platformLaunchReferrer
import timber.log.Timber

class ConfigResolver(
    private val getResolvedConfigUseCase: GetResolvedConfigUseCase
) {
    private companion object {
        const val TAG = "InstallSource"
        const val DOWNLOADS_AUTHORITY = "downloads"
    }

    suspend fun resolve(activity: Activity): ConfigModel {
        Timber.tag(TAG).d("resolveConfig: Starting.")

        if (activity::class.java.name == ActivityContracts.UNINSTALLER_ACTIVITY) {
            Timber.tag(TAG).d("Activity is UninstallerActivity. Returning default config immediately.")
            return getConfigForSource(InstallSource.unknown(), trustedNotUnknownSource = false)
        }

        val source = activity.resolveInstallSource()
        val trustedNotUnknownSource = activity.isTrustedNotUnknownSource(source)
        Timber.tag(TAG).d(
            "Resolved install source: package=%s, uid=%s, candidates=%s, confidence=%s, viaProxy=%s, trustedNotUnknownSource=%s",
            source.packageName,
            source.uid,
            source.packageCandidates,
            source.confidence,
            source.viaProxyPackage,
            trustedNotUnknownSource
        )
        return getConfigForSource(source, trustedNotUnknownSource)
    }

    suspend fun resolveForPackage(packageName: String?): ConfigModel =
        getConfigForSource(
            InstallSource(
                packageName = packageName,
                uid = null,
                confidence = if (packageName == null) {
                    InstallSourceConfidence.UNKNOWN
                } else {
                    InstallSourceConfidence.EXACT_CALLER
                }
            ),
            trustedNotUnknownSource = false
        )

    private fun Activity.resolveInstallSource(): InstallSource {
        val callingPackage = callingPackage
        Timber.tag(TAG).d("activity.callingPackage: $callingPackage")
        if (callingPackage != null) {
            if (isInstallSourceProxyPackage(callingPackage)) {
                Timber.tag(TAG).d("Calling package $callingPackage is a trusted install source proxy.")
                return resolveTrustedProxySource(callingPackage)
            }

            val uid = packageUid(callingPackage)
            Timber.tag(TAG).d("Using exact calling package source: package=$callingPackage, uid=$uid")
            return InstallSource(
                packageName = callingPackage,
                uid = uid,
                packageCandidates = uid?.let { packageNamesForUid(it) }.orEmpty(),
                confidence = InstallSourceConfidence.EXACT_CALLER
            )
        }

        getLaunchedFromUidCompat()?.let { launchedUid ->
            val candidates = packageNamesForUid(launchedUid)
            Timber.tag(TAG).d(
                "Using launched-from uid source: uid=$launchedUid, candidates=$candidates"
            )
            return InstallSource(
                packageName = selectPackageForUid(candidates),
                uid = launchedUid,
                packageCandidates = candidates,
                confidence = InstallSourceConfidence.LAUNCHED_FROM_UID
            )
        }

        // Check referrer before URI authority: browsers such as Chrome may hand us a media or
        // downloads provider URI, while the install requester is still the activity referrer.
        val referrer = platformLaunchReferrer()
        Timber.tag(TAG).d("activity.referrer: $referrer")
        val referrerHost = referrer?.host
        if (referrer?.scheme == "android-app" && referrerHost != null) {
            val uid = packageUid(referrerHost)
            Timber.tag(TAG).d("Using referrer heuristic source: package=$referrerHost, uid=$uid")
            return InstallSource(
                packageName = referrerHost,
                uid = uid,
                packageCandidates = uid?.let { packageNamesForUid(it) }.orEmpty(),
                confidence = InstallSourceConfidence.REFERRER_HEURISTIC
            )
        } else if (referrer != null) {
            Timber.tag(TAG).w("Ignoring referrer with non-app scheme: ${referrer.scheme}")
        }

        val intent = intent
        // Provider owner is only a low-confidence fallback. It identifies where the APK bytes
        // are served from, not necessarily who requested the install.
        val authority = intent.data?.authority
            ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri?.authority
        Timber.tag(TAG).d("URI authority: $authority")
        if (authority != null) {
            val providerPackage = packageNameFromAuthority(authority)
            val providerUid = providerPackage?.let { packageUid(it) }
            Timber.tag(TAG).d(
                "Using provider-owner source: authority=$authority, package=$providerPackage, uid=$providerUid"
            )
            return InstallSource(
                packageName = providerPackage,
                uid = providerUid,
                packageCandidates = providerUid?.let { packageNamesForUid(it) }.orEmpty(),
                confidence = InstallSourceConfidence.PROVIDER_OWNER
            )
        }

        Timber.tag(TAG).w("Could not determine calling package. Using default config.")
        return InstallSource.unknown()
    }

    private fun Activity.resolveTrustedProxySource(proxyPackageName: String): InstallSource {
        val originatingUid =
            intent.getIntExtra(IntentHidden.EXTRA_ORIGINATING_UID, Process.INVALID_UID)
        if (originatingUid == Process.INVALID_UID) {
            Timber.tag(TAG).d(
                "Proxy install source $proxyPackageName did not provide a valid originating uid. " +
                        "Using default config without source AppOps checks."
            )
            return InstallSource.unknown(viaProxyPackage = proxyPackageName)
        }

        val candidates = packageNamesForUid(originatingUid)
        val originatingPackage = selectPackageForUid(candidates)
        Timber.tag(TAG).d(
            "Proxy install source $proxyPackageName resolved originating uid " +
                    "$originatingUid to package $originatingPackage"
        )

        return InstallSource(
            packageName = originatingPackage,
            uid = if (candidates.isEmpty()) null else originatingUid,
            packageCandidates = candidates,
            confidence = if (candidates.isEmpty()) {
                InstallSourceConfidence.UNKNOWN
            } else {
                InstallSourceConfidence.TRUSTED_PROXY_ORIGINATING_UID
            },
            viaProxyPackage = proxyPackageName
        )
    }

    @Suppress("DEPRECATION")
    private fun Activity.isTrustedNotUnknownSource(source: InstallSource): Boolean {
        val rawNotUnknownSource = intent.getBooleanExtra(
            PackageManagerHidden.EXTRA_NOT_UNKNOWN_SOURCE,
            false
        )

        val trusted = rawNotUnknownSource &&
                intent.action == Intent.ACTION_INSTALL_PACKAGE &&
                source.confidence.isTrustedForPlatformPolicy()
        Timber.tag(TAG).d(
            "Evaluated not-unknown-source extra: raw=$rawNotUnknownSource, " +
                    "action=${intent.action}, confidence=${source.confidence}, trusted=$trusted"
        )
        return trusted
    }

    private suspend fun getConfigForSource(
        source: InstallSource,
        trustedNotUnknownSource: Boolean
    ): ConfigModel {
        var config = getResolvedConfigUseCase(source.packageName)

        val initialInstallFlags = listOfNotNull(
            config.allowTestOnly.takeIf { it }?.let { InstallOption.AllowTest.value },
            config.allowDowngrade.takeIf { it }?.let { InstallOption.AllowDowngrade.value },
            config.forAllUser.takeIf { it }?.let { InstallOption.AllUsers.value },
            config.bypassLowTargetSdk.takeIf { it }?.let { InstallOption.BypassLowTargetSdkBlock.value },
            config.allowAllRequestedPermissions.takeIf { it }?.let { InstallOption.GrantAllRequestedPermissions.value },
            config.requestUpdateOwnership.takeIf { it }?.let { InstallOption.RequestUpdateOwnerShip.value },
        ).fold(0) { acc, flag -> acc or flag }

        config = config.copy(
            installFlags = config.installFlags.addFlag(initialInstallFlags),
            installSourceUid = source.uid,
            installSourcePackageCandidates = source.packageCandidates,
            installSourceConfidence = source.confidence,
            notUnknownSource = trustedNotUnknownSource
        )

        Timber.tag(TAG).d("Resolved config for '${source.packageName ?: "default"}': $config")
        return config
    }

    @Suppress("DEPRECATION")
    private fun Activity.getLaunchedFromUidCompat(): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return null
        return launchedFromUid.takeIf { it != Process.INVALID_UID }
    }

    @Suppress("DEPRECATION")
    private fun Activity.packageUid(packageName: String): Int? =
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageUid(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                packageManager.getPackageUid(packageName, 0)
            }
        }.getOrNull()

    private fun Activity.packageNamesForUid(uid: Int): List<String> =
        packageManager.getPackagesForUid(uid)?.filterNotNull().orEmpty()

    private fun Activity.selectPackageForUid(candidates: List<String>): String? =
        candidates.firstOrNull {
            Manifest.permission.REQUEST_INSTALL_PACKAGES in packageRequestedPermissions(it)
        } ?: candidates.firstOrNull()

    private fun Activity.isInstallSourceProxyPackage(packageName: String): Boolean {
        val hasManageDocuments = packageManager.checkPermission(
            Manifest.permission.MANAGE_DOCUMENTS,
            packageName
        ) == PackageManager.PERMISSION_GRANTED

        return (hasManageDocuments && isSystemOrUpdatedSystemPackage(packageName)) ||
                isSystemDownloadsProviderPackage(packageName)
    }

    private fun Activity.isSystemDownloadsProviderPackage(packageName: String): Boolean {
        val appInfo = resolveProviderInfoCompat(DOWNLOADS_AUTHORITY)?.applicationInfo ?: return false
        return appInfo.isSystemOrUpdatedSystemApp() && appInfo.packageName == packageName
    }

    private fun Activity.packageNameFromAuthority(authority: String): String? =
        resolveProviderInfoCompat(authority)?.packageName

    private fun Activity.resolveProviderInfoCompat(authority: String): ProviderInfo? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.resolveContentProvider(
                authority,
                PackageManager.ComponentInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.resolveContentProvider(authority, 0)
        }

    private fun Activity.isSystemOrUpdatedSystemPackage(packageName: String): Boolean {
        val appInfo = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, 0)
            }
        }.getOrNull() ?: return false

        return appInfo.isSystemOrUpdatedSystemApp()
    }

    private fun ApplicationInfo.isSystemOrUpdatedSystemApp(): Boolean =
        flags and ApplicationInfo.FLAG_SYSTEM != 0 ||
                flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0

    private fun Activity.packageRequestedPermissions(packageName: String): List<String> =
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            }.requestedPermissions.orEmpty()
        }.getOrElse { emptyArray() }
            .toList()

    private data class InstallSource(
        val packageName: String?,
        val uid: Int?,
        val packageCandidates: List<String> = emptyList(),
        val confidence: InstallSourceConfidence,
        val viaProxyPackage: String? = null
    ) {
        companion object {
            fun unknown(viaProxyPackage: String? = null) = InstallSource(
                packageName = null,
                uid = null,
                confidence = InstallSourceConfidence.UNKNOWN,
                viaProxyPackage = viaProxyPackage
            )
        }
    }
}
