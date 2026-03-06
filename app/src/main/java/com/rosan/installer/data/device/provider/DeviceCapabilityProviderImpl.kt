// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.device.provider

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import com.rosan.installer.core.env.DeviceConfig
import com.rosan.installer.core.reflection.ReflectionProvider
import com.rosan.installer.core.reflection.invokeStatic
import com.rosan.installer.domain.device.model.Manufacturer
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.util.hasFlag
import timber.log.Timber

class DeviceCapabilityProviderImpl(
    private val context: Context,
    private val reflect: ReflectionProvider
) : DeviceCapabilityProvider {
    companion object {
        private const val MIUI_PACKAGE_INSTALLER = "com.miui.packageinstaller"
        private const val MIN_SUPPORTED_MIUI_VERSION_CODE = 54100L

        private const val KEY_MIUI_VERSION_NAME = "ro.miui.ui.version.name"
        private const val KEY_MI_OS_VERSION_NAME = "ro.mi.os.version.name"
        private const val KEY_OPLUS_API = "ro.build.version.oplus.api"
        private const val KEY_OPLUS_SUB_API = "ro.build.version.oplus.sub_api"
    }

    private val systemPropertiesClass by lazy { @SuppressLint("PrivateApi") Class.forName("android.os.SystemProperties") }

    override val isSessionInstallSupported: Boolean by lazy {
        calculateSessionInstallSupport() || isSystemApp
    }

    override val hasMiPackageInstaller: Boolean by lazy {
        getMiuiPackageInstallerVersion() != null
    }

    override val isSystemApp: Boolean by lazy {
        try {
            context.applicationInfo.flags.hasFlag(ApplicationInfo.FLAG_SYSTEM)
        } catch (_: Exception) {
            false
        }
    }

    override val isHyperOS: Boolean by lazy {
        val osName = getSystemProperty(KEY_MI_OS_VERSION_NAME)
        !osName.isNullOrEmpty() && osName.startsWith("OS")
    }

    override val isMIUI: Boolean by lazy {
        val miuiName = getSystemProperty(KEY_MIUI_VERSION_NAME)
        !miuiName.isNullOrEmpty() && !isHyperOS
    }

    override val isSupportMiIsland: Boolean by lazy {
        try {
            val focusProtocolVersion = Settings.System.getInt(
                context.contentResolver,
                "notification_focus_protocol",
                0
            )
            focusProtocolVersion == 3
        } catch (_: Exception) {
            false
        }
    }

    override val oplusOSdkVersion: String? by lazy {
        val api = getSystemProperty(KEY_OPLUS_API)
        if (api.isNullOrEmpty()) return@lazy null

        val subApi = getSystemProperty(KEY_OPLUS_SUB_API)
        if (!subApi.isNullOrEmpty()) "$api.$subApi" else api
    }

    private fun calculateSessionInstallSupport(): Boolean {
        val isMi = DeviceConfig.currentManufacturer == Manufacturer.XIAOMI
        if (!isMi) return true

        val miPackageManagerVersion = getMiuiPackageInstallerVersion()
        return if (miPackageManagerVersion != null) {
            miPackageManagerVersion.second >= MIN_SUPPORTED_MIUI_VERSION_CODE
        } else true
    }

    private fun getMiuiPackageInstallerVersion(): Pair<String, Long>? =
        try {
            val info = context.packageManager.getPackageInfo(MIUI_PACKAGE_INSTALLER, 0)
            val versionName = info.versionName ?: ""
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION") info.versionCode.toLong()
            }
            versionName to versionCode
        } catch (_: PackageManager.NameNotFoundException) {
            null
        } catch (e: Throwable) {
            Timber.e(e, "Failed to retrieve MIUI package installer version")
            null
        }

    private fun getSystemProperty(key: String): String? =
        reflect.invokeStatic<String>(
            "get",
            systemPropertiesClass,
            arrayOf(String::class.java, String::class.java),
            key, ""
        )?.takeIf { it.isNotEmpty() }
}
