// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.device.provider

import com.rosan.installer.domain.device.model.ShizukuMode
import com.rosan.installer.domain.settings.model.RootMode
import kotlinx.coroutines.flow.StateFlow

interface DeviceCapabilityProvider {
    val isSessionInstallSupported: Boolean
    val hasMiPackageInstaller: Boolean
    val isDefaultInstaller: Boolean

    val isSystemApp: Boolean
    val isHyperOS: Boolean
    val isMIUI: Boolean
    val isSupportMiIsland: Boolean
    val oplusOSdkVersion: String?

    var isLSPosedActive: Boolean

    /**
     * Flow emitting the current Shizuku running mode.
     * Can change dynamically during runtime.
     */
    val shizukuModeFlow: StateFlow<ShizukuMode>

    /**
     * Flow emitting whether Shizuku has been granted permission.
     */
    val shizukuAuthorizedFlow: StateFlow<Boolean>

    /**
     * Flow emitting whether Dhizuku is installed, active, and binder is received.
     */
    val dhizukuAvailableFlow: StateFlow<Boolean>

    /**
     * Flow emitting whether Dhizuku has been granted permission.
     */
    val dhizukuAuthorizedFlow: StateFlow<Boolean>

    /**
     * Flow emitting the detected local root implementation.
     * Static during the app's lifecycle but detected asynchronously.
     */
    val rootModeFlow: StateFlow<RootMode>

    /**
     * Flow emitting the current system default installer activity.
     */
    val defaultInstallerFlow: StateFlow<String>

    /**
     * Refreshes both Shizuku, Dhizuku, and Root detection states.
     * This should be called early in the app lifecycle (e.g., Application.onCreate).
     */
    fun refreshPrivilegeStatus()
}
