// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.model

/**
 * Define Authorizers used by InstallerX
 */
enum class Authorizer(val value: String) {
    Global("global"),
    None("none"),
    Root("root"),
    Shizuku("shizuku"),
    Dhizuku("dhizuku"),
    Customize("customize");
}

/**
 * Define Install Modes used by InstallerX
 */
enum class InstallMode(val value: String) {
    Global("global"),
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
