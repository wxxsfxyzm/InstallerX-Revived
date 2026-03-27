// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.model

import com.rosan.installer.core.env.DeviceConfig
import com.rosan.installer.domain.device.model.Manufacturer

/**
 * Represents the complete business object for a configuration
 */
data class ConfigModel(
    val id: Long = 0L,
    val name: String = "Default",
    val description: String,
    val authorizer: Authorizer,
    val customizeAuthorizer: String,
    val installMode: InstallMode,
    val showToast: Boolean = false,
    val enableCustomizeInstallReason: Boolean = false,
    val installReason: InstallReason = InstallReason.UNKNOWN,
    val enableCustomizePackageSource: Boolean = false,
    val packageSource: PackageSource = PackageSource.OTHER,
    val installRequester: String? = null,
    val installer: String?,
    val enableCustomizeUser: Boolean = false,
    val targetUserId: Int = 0,
    val enableManualDexopt: Boolean = false,
    val forceDexopt: Boolean = false,
    val dexoptMode: DexoptMode = DexoptMode.SpeedProfile,
    val autoDelete: Boolean = false,
    val autoDeleteZip: Boolean = false,
    val displaySize: Boolean = false,
    val displaySdk: Boolean = false,
    val forAllUser: Boolean = false,
    val allowTestOnly: Boolean = false,
    val allowDowngrade: Boolean = false,
    val bypassLowTargetSdk: Boolean = false,
    val allowAllRequestedPermissions: Boolean = false,
    val requestUpdateOwnership: Boolean = false,
    val splitChooseAll: Boolean = false,
    val apkChooseAll: Boolean = false,

    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),

    // Runtime fields that are not saved in the database but needed for business logic
    var installFlags: Int = 0,
    var bypassBlacklistInstallSetByUser: Boolean = false,
    var uninstallFlags: Int = 0,
    var callingFromUid: Int? = null
) {
    companion object {
        var default = ConfigModel(
            description = "",
            authorizer = Authorizer.Global,
            customizeAuthorizer = "",
            installMode = InstallMode.Global,
            showToast = false,
            enableCustomizeInstallReason = false,
            installReason = InstallReason.UNKNOWN,
            enableCustomizePackageSource = false,
            packageSource = PackageSource.OTHER,
            installer = null,
            enableCustomizeUser = false,
            targetUserId = 0,
            enableManualDexopt = false,
            forceDexopt = false,
            dexoptMode = DexoptMode.SpeedProfile,
            autoDelete = false,
            autoDeleteZip = false,
            displaySize = false,
            displaySdk = false,
            forAllUser = false,
            allowTestOnly = false,
            allowDowngrade = false,
            bypassLowTargetSdk = false,
            allowAllRequestedPermissions = false,
            requestUpdateOwnership = false,
            splitChooseAll = false,
            apkChooseAll = false
        )

        val XiaomiDefault = ConfigModel(
            description = "",
            authorizer = Authorizer.Global,
            customizeAuthorizer = "",
            installMode = InstallMode.Global,
            showToast = false,
            enableCustomizeInstallReason = false,
            installReason = InstallReason.UNKNOWN,
            enableCustomizePackageSource = false,
            packageSource = PackageSource.OTHER,
            installer = "com.android.shell",
            enableCustomizeUser = false,
            targetUserId = 0,
            enableManualDexopt = false,
            forceDexopt = false,
            dexoptMode = DexoptMode.SpeedProfile,
            autoDelete = false,
            autoDeleteZip = false,
            displaySize = false,
            displaySdk = false,
            forAllUser = false,
            allowTestOnly = false,
            allowDowngrade = false,
            bypassLowTargetSdk = false,
            allowAllRequestedPermissions = false,
            requestUpdateOwnership = false,
            splitChooseAll = false,
            apkChooseAll = false
        )

        fun generateOptimalDefault(): ConfigModel =
            when (DeviceConfig.currentManufacturer) {
                Manufacturer.XIAOMI -> XiaomiDefault
                else -> default
            }
    }
}
