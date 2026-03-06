// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.device.provider

interface DeviceCapabilityProvider {
    val isSessionInstallSupported: Boolean
    val hasMiPackageInstaller: Boolean

    val isSystemApp: Boolean
    val isHyperOS: Boolean
    val isMIUI: Boolean
    val isSupportMiIsland: Boolean
    val oplusOSdkVersion: String?
}
