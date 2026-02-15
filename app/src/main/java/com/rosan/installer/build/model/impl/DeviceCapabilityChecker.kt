package com.rosan.installer.build.model.impl

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.rosan.installer.build.RsConfig
import com.rosan.installer.build.model.entity.Manufacturer
import com.rosan.installer.util.OSUtils
import timber.log.Timber

/**
 * Service responsible for checking device-specific capabilities and system limitations.
 * Managed by Koin as a Singleton.
 */
class DeviceCapabilityChecker(private val context: Context) {
    companion object {
        private const val MIUI_PACKAGE_INSTALLER = "com.miui.packageinstaller"
        private const val MIN_SUPPORTED_MIUI_VERSION_CODE = 54100L
    }

    /**
     * Checks if session-based installation is supported on this device.
     *
     * Value is cached using `lazy`, ensuring the expensive PackageManager check
     * runs only once per app lifecycle in a thread-safe manner.
     */
    val isSessionInstallSupported: Boolean by lazy {
        calculateSessionInstallSupport() || OSUtils.isSystemApp
    }

    /**
     * Checks if the MIUI package installer is installed on this device.
     *
     * Value is cached using `lazy`, ensuring the expensive PackageManager check
     * runs only once per app lifecycle in a thread-safe manner.
     */
    val hasMiPackageInstaller: Boolean by lazy {
        getMiuiPackageInstallerVersion() != null
    }

    private fun calculateSessionInstallSupport(): Boolean {
        // Reuse the static Manufacturer enum from RsConfig
        val isMi = RsConfig.currentManufacturer == Manufacturer.XIAOMI
        Timber.d("isSessionInstallSupported: isMi=$isMi")

        if (!isMi) {
            // Non-Xiaomi devices are supported by default.
            return true
        }

        val miPackageManagerVersion = getMiuiPackageInstallerVersion()
        Timber.d("isSessionInstallSupported: miPackageManagerVersion=$miPackageManagerVersion")

        return if (miPackageManagerVersion != null) {
            Timber.d("isSessionInstallSupported: version=${miPackageManagerVersion.second}")
            miPackageManagerVersion.second >= MIN_SUPPORTED_MIUI_VERSION_CODE
        } else {
            // if miPackageManagerVersion is null, assume supported
            true
        }
    }

    private fun getMiuiPackageInstallerVersion(): Pair<String, Long>? =
        try {
            val pm = context.packageManager
            val info = pm.getPackageInfo(MIUI_PACKAGE_INSTALLER, 0)

            val versionName = info.versionName ?: ""
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }

            versionName to versionCode
        } catch (_: PackageManager.NameNotFoundException) {
            // Expected on non-MIUI ROMs or if stripped
            null
        } catch (e: Throwable) {
            Timber.e(e, "Failed to retrieve MIUI package installer version")
            null
        }
}