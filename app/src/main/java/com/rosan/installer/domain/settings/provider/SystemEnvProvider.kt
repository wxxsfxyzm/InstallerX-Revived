// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.provider

import kotlinx.coroutines.flow.Flow

interface SystemEnvProvider {
    suspend fun getPackageUid(packageName: String): Int?
    fun isIgnoringBatteryOptimizationsFlow(): Flow<Boolean>
    fun isAdbVerifyEnabledFlow(): Flow<Boolean>
    fun requestIgnoreBatteryOptimization()

    suspend fun authenticateBiometric(isInstaller: Boolean): Boolean
    fun setLauncherAliasEnabled(enabled: Boolean)
    suspend fun getLatestLogUri(): String?
    fun getWallpaperColorsFlow(): Flow<List<Int>?>
}