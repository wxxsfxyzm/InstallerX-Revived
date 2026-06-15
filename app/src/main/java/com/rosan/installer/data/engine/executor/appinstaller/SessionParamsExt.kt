// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.executor.appinstaller

import android.Manifest
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import com.rosan.installer.domain.engine.model.install.InstallEntity
import com.rosan.installer.domain.engine.model.install.InstallMetadata
import timber.log.Timber

internal fun PackageInstaller.SessionParams.applyMetadata(
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
