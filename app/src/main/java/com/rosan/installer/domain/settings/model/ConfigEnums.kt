// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.model

import androidx.annotation.StringRes
import com.rosan.installer.R

/**
 * Define Authorizers used by InstallerX
 */
enum class Authorizer(
    val value: String,
    @field:StringRes val displayNameRes: Int,
) {
    Global("global", R.string.config_authorizer_global),
    None("none", R.string.config_authorizer_none),
    Root("root", R.string.config_authorizer_root),
    Shizuku("shizuku", R.string.config_authorizer_shizuku),
    Dhizuku("dhizuku", R.string.config_authorizer_dhizuku),
    Customize("customize", R.string.config_authorizer_customize);

    companion object {
        fun fromValueOrDefault(value: String) =
            entries.find { it.value == value } ?: Global
    }
}

/**
 * Define Install Modes used by InstallerX
 */
enum class InstallMode(val value: String) {
    Dialog("dialog"),
    AutoDialog("auto_dialog"),
    Notification("notification"),
    AutoNotification("auto_notification"),
    Ignore("ignore");
}

/**
 * Define Installer Modes used by InstallerX
 */
enum class InstallerMode(val value: Int) {
    Self(0),
    Initiator(1),
    Custom(2)
}

/**
 * Define Dexopt Modes used by InstallerX
 * Sync with Android's Dexopt Mode
 */
enum class DexoptMode(val value: String) {
    Verify("verify"),
    SpeedProfile("speed-profile"),
    Speed("speed"),
    Everything("everything");
}

/**
 * Define Install Reasons,
 * Sync with Android's Install Reason
 *
 * @see android.content.pm.PackageInstaller.SessionParams.setInstallReason
 */
enum class InstallReason(val value: Int) {
    /**
     * Code indicating that the reason for installing this package is unknown.
     * @see android.content.pm.PackageManager.INSTALL_REASON_UNKNOWN
     */
    UNKNOWN(0),

    /**
     * Code indicating that this package was installed due to enterprise policy.
     * @see android.content.pm.PackageManager.INSTALL_REASON_POLICY
     */
    POLICY(1),

    /**
     * Code indicating that this package was installed as part of restoring from another device.
     * @see android.content.pm.PackageManager.INSTALL_REASON_DEVICE_RESTORE
     */
    DEVICE_RESTORE(2),

    /**
     * Code indicating that this package was installed as part of device setup.
     * @see android.content.pm.PackageManager.INSTALL_REASON_DEVICE_SETUP
     */
    DEVICE_SETUP(3),

    /**
     * Code indicating that the package installation was initiated by the user.
     * NOTE: this will cause launcher release desktop icon
     * @see android.content.pm.PackageManager.INSTALL_REASON_USER
     */
    USER(4);

    companion object {
        fun fromInt(value: Int) = entries.find { it.value == value } ?: UNKNOWN
    }
}

/**
 * Define Package Sources,
 * Sync with Android's Package Source
 * @see android.content.pm.PackageInstaller.SessionParams.setPackageSource
 */
enum class PackageSource(val value: Int) {
    UNSPECIFIED(0),     // Corresponds to PACKAGE_SOURCE_UNSPECIFIED
    OTHER(1),           // Corresponds to PACKAGE_SOURCE_OTHER
    STORE(2),           // Corresponds to PACKAGE_SOURCE_STORE
    LOCAL_FILE(3),      // Corresponds to PACKAGE_SOURCE_LOCAL_FILE
    DOWNLOADED_FILE(4); // Corresponds to PACKAGE_SOURCE_DOWNLOADED_FILE

    companion object {
        fun fromInt(value: Int) = entries.find { it.value == value } ?: OTHER
    }
}

/**
 * Define GitHub Update Channels used by InstallerX
 */
enum class GithubUpdateChannel {
    OFFICIAL,
    PROXY_7ED,
    CUSTOM;

    companion object {
        fun fromValueOrDefault(value: String) = entries.find { it.name == value } ?: OFFICIAL
    }
}

/**
 * Define Biometric Auth Modes used by InstallerX
 */
enum class BiometricAuthMode(val value: String) {
    Disable("disable"),
    Enable("enable"),
    FollowConfig("follow_config");

    companion object {
        fun fromValueOrDefault(value: String) = entries.find { it.value == value } ?: FollowConfig
    }
}
