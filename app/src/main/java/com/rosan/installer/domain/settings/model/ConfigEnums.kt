// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.model

// Extracted from Data layer to keep Domain layer pure
enum class Authorizer(val value: String) {
    Global("global"),
    None("none"),
    Root("root"),
    Shizuku("shizuku"),
    Dhizuku("dhizuku"),
    Customize("customize");
}

enum class InstallMode(val value: String) {
    Global("global"),
    Dialog("dialog"),
    AutoDialog("auto_dialog"),
    Notification("notification"),
    AutoNotification("auto_notification"),
    Ignore("ignore");
}

enum class DexoptMode(val value: String) {
    Verify("verify"),
    SpeedProfile("speed-profile"),
    Speed("speed"),
    Everything("everything");
}

enum class InstallReason(val value: Int) {
    UNKNOWN(0),
    POLICY(1),
    DEVICE_RESTORE(2),
    DEVICE_SETUP(3),
    USER(4);

    companion object {
        fun fromInt(value: Int) = entries.find { it.value == value } ?: UNKNOWN
    }
}

enum class PackageSource(val value: Int) {
    UNSPECIFIED(0),
    OTHER(1),
    STORE(2),
    LOCAL_FILE(3),
    DOWNLOADED_FILE(4);

    companion object {
        fun fromInt(value: Int) = entries.find { it.value == value } ?: OTHER
    }
}