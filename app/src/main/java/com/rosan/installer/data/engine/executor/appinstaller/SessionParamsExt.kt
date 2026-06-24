// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.executor.appinstaller

import android.Manifest
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import com.rosan.installer.domain.engine.model.install.InstallEntity
import com.rosan.installer.domain.engine.model.install.InstallMetadata
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.config.ConfigModel
import com.rosan.installer.domain.settings.model.config.PackageSource
import timber.log.Timber

internal fun PackageInstaller.SessionParams.applySessionContext(
    config: ConfigModel,
    metadata: InstallMetadata,
    entities: List<InstallEntity>,
    installerPackageName: String?,
    respectPlatformInstallPolicy: Boolean
) {
    applyMetadata(
        metadata = metadata,
        entities = entities,
        installerPackageName = installerPackageName,
        respectPlatformInstallPolicy = respectPlatformInstallPolicy
    )
    applyInstallReasonAndPackageSource(
        config = config,
        metadata = metadata,
        respectPlatformInstallPolicy = respectPlatformInstallPolicy
    )
}

private fun PackageInstaller.SessionParams.applyMetadata(
    metadata: InstallMetadata,
    entities: List<InstallEntity>,
    installerPackageName: String?,
    respectPlatformInstallPolicy: Boolean
) {
    installerPackageName
        ?.takeIf { it.isNotBlank() }
        ?.let { installer ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                setInstallerPackageName(installer)
            }
        }

    metadata.sourceUris.firstNotNullOfOrNull { it.toUriOrNull() }
        ?.let(::setOriginatingUri)

    metadata.referrerUri?.toUriOrNull()
        ?.let(::setReferrerUri)

    entities.totalInstallSize()
        ?.let(::setSize)

    entities.firstNotNullOfOrNull { it.installLocation }
        ?.let(::setInstallLocation)

    if (
        respectPlatformInstallPolicy &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    ) {
        setPermissionState(
            Manifest.permission.USE_FULL_SCREEN_INTENT,
            PackageInstaller.SessionParams.PERMISSION_STATE_DENIED
        )
    }
}

private fun PackageInstaller.SessionParams.applyInstallReasonAndPackageSource(
    config: ConfigModel,
    metadata: InstallMetadata,
    respectPlatformInstallPolicy: Boolean
) {
    val installReason = when {
        respectPlatformInstallPolicy -> PackageManager.INSTALL_REASON_USER
        config.enableCustomizeInstallReason -> config.installReason.value
        else -> PackageManager.INSTALL_REASON_UNKNOWN
    }
    Timber.d("Setting installReason to $installReason")
    setInstallReason(installReason)

    // Only available on Android 13+.
    // TODO Dhizuku needs test.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && config.authorizer != Authorizer.Dhizuku) {
        val packageSource = when {
            respectPlatformInstallPolicy -> metadata.defaultPackageSource()
            config.enableCustomizePackageSource -> config.packageSource
            else -> PackageSource.UNSPECIFIED
        }
        Timber.d("Setting packageSource to ${packageSource.name} (${packageSource.value})")
        setPackageSource(packageSource.value)
    }
}

private fun String.toUriOrNull(): Uri? =
    runCatching { toUri() }
        .getOrElse { error ->
            Timber.w(error, "Failed to parse install metadata URI: $this")
            null
        }
        ?.takeIf { !it.scheme.isNullOrBlank() }

private fun List<InstallEntity>.totalInstallSize(): Long? {
    if (isEmpty()) return null
    val sizes = map { it.data.getSize() }
    if (sizes.any { it <= 0L }) return null
    return sizes.sum()
}

private fun InstallMetadata.defaultPackageSource(): PackageSource =
    if (referrerUri.isNullOrBlank()) PackageSource.LOCAL_FILE else PackageSource.DOWNLOADED_FILE
