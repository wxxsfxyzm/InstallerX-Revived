// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.settings.provider

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.kieronquinn.monetcompat.core.MonetCompat
import com.rosan.installer.BuildConfig
import com.rosan.installer.R
import com.rosan.installer.domain.settings.provider.SystemEnvProvider
import com.rosan.installer.ui.common.auth.safeBiometricAuth
import com.rosan.installer.util.timber.FileLoggingTree
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

class SystemEnvProviderImpl(private val context: Context) : SystemEnvProvider {
    override val packageName: String = context.packageName

    override suspend fun getPackageUid(packageName: String): Int? {
        return runCatching {
            context.packageManager.getApplicationInfo(packageName, 0).uid
        }.getOrNull()
    }

    override fun isIgnoringBatteryOptimizationsFlow(): Flow<Boolean> = flow {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        emit(pm.isIgnoringBatteryOptimizations(context.packageName))
        // Delay for compatibility (e.g., Xiaomi devices)
        delay(500)
        emit(pm.isIgnoringBatteryOptimizations(context.packageName))
    }.flowOn(Dispatchers.IO)

    override fun isAdbVerifyEnabledFlow(): Flow<Boolean> = flow {
        val enabled = Settings.Global.getInt(
            context.contentResolver,
            "verifier_verify_adb_installs",
            1
        ) != 0
        emit(enabled)
    }.flowOn(Dispatchers.IO)

    @SuppressLint("BatteryLife")
    override fun requestIgnoreBatteryOptimization() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = "package:${context.packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    override suspend fun authenticateBiometric(isInstaller: Boolean): Boolean {
        return context.safeBiometricAuth(
            title = context.getString(R.string.use_biometric_confirm_change_auth_settings),
            subTitle = context.getString(R.string.use_biometric_confirm_change_auth_settings_desc)
        )
    }

    override fun setLauncherAliasEnabled(enabled: Boolean) {
        val componentName = ComponentName(context, "com.rosan.installer.ui.activity.LauncherAlias")
        val newState = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        context.packageManager.setComponentEnabledSetting(componentName, newState, PackageManager.DONT_KILL_APP)
    }

    override suspend fun getLatestLogUri(): String? = withContext(Dispatchers.IO) {
        val logDir = File(context.cacheDir, FileLoggingTree.LOG_DIR_NAME)
        if (!logDir.exists() || !logDir.isDirectory) return@withContext null

        val latestLogFile = logDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(FileLoggingTree.LOG_SUFFIX) }
            ?.maxByOrNull { it.lastModified() }

        if (latestLogFile == null || latestLogFile.length() == 0L) return@withContext null

        val authority = "${BuildConfig.APPLICATION_ID}.fileprovider"
        return@withContext FileProvider.getUriForFile(context, authority, latestLogFile).toString()
    }

    override fun getWallpaperColorsFlow(): Flow<List<Int>?> = flow {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            val colors = try {
                MonetCompat.getInstance().getAvailableWallpaperColors()
            } catch (e: Exception) {
                null
            }
            emit(colors)
        } else emit(null)
    }.flowOn(Dispatchers.IO)
}
