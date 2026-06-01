// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.device.provider

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import androidx.core.net.toUri
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.installer.core.bitmask.hasFlag
import com.rosan.installer.core.device.model.Manufacturer
import com.rosan.installer.core.env.DeviceConfig
import com.rosan.installer.core.reflection.ReflectionProvider
import com.rosan.installer.core.reflection.invoke
import com.rosan.installer.core.reflection.invokeStatic
import com.rosan.installer.domain.device.model.ShizukuMode
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.settings.model.preferences.RootMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import timber.log.Timber
import java.io.File

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

        private const val SAMSUNG_FLOATING_FEATURE_CLASS =
            "com.samsung.android.feature.SemFloatingFeature"
        private const val KEY_SAMSUNG_PRODUCT_NAME =
            "SEC_FLOATING_FEATURE_SETTINGS_CONFIG_BRAND_NAME"

        private val SAMSUNG_FLOATING_FEATURE_PATHS = listOf(
            "/vendor/etc/floating_feature.xml",
            "/system/etc/floating_feature.xml",
            "/system/system/etc/floating_feature.xml",
            "/product/etc/floating_feature.xml",
            "/odm/etc/floating_feature.xml"
        )

        private val OEM_MARKET_NAME_PROPERTY_KEYS = listOf(
            "ro.product.marketname",        // Xiaomi
            "ro.vendor.oplus.market.name",  // Oppo
            "ro.vivo.market.name",          // Vivo
            "ro.config.marketing_name"      // Huawei/Honor
        )
    }

    private val systemPropertiesClass by lazy {
        @SuppressLint("PrivateApi")
        Class.forName("android.os.SystemProperties")
    }

    override val isSessionInstallSupported: Boolean by lazy {
        calculateSessionInstallSupport() || isSystemApp
    }

    override val hasMiPackageInstaller: Boolean by lazy {
        getMiuiPackageInstallerVersion() != null
    }

    override val isDefaultInstaller: Boolean
        get() {
            if (isLSPosedActive) return true
            val intent = Intent(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_DEFAULT)
                .setDataAndType(
                    "content://storage/emulated/0/test.apk".toUri(),
                    "application/vnd.android.package-archive"
                )
            val resolveInfo = context.packageManager.resolveActivity(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            Timber.d("ResolveInfo: $resolveInfo")
            return resolveInfo?.activityInfo?.packageName == context.packageName
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

    override val deviceName: String by lazy {
        getSamsungProductName()
            ?: OEM_MARKET_NAME_PROPERTY_KEYS.firstNotNullOfOrNull { key ->
                getSystemProperty(key, Build.UNKNOWN).takeIfValidDeviceName()
            }
            ?: DeviceConfig.deviceName
    }

    override var isLSPosedActive: Boolean = false

    // --- Shizuku Dynamic Flow ---
    private val _shizukuModeFlow = MutableStateFlow(ShizukuMode.NONE)
    override val shizukuModeFlow: StateFlow<ShizukuMode> = _shizukuModeFlow.asStateFlow()

    private val _shizukuAuthorizedFlow = MutableStateFlow(false)
    override val shizukuAuthorizedFlow: StateFlow<Boolean> = _shizukuAuthorizedFlow.asStateFlow()

    // --- Dhizuku Dynamic Flow ---
    private val _dhizukuAvailableFlow = MutableStateFlow(false)
    override val dhizukuAvailableFlow: StateFlow<Boolean> = _dhizukuAvailableFlow.asStateFlow()

    private val _dhizukuAuthorizedFlow = MutableStateFlow(false)
    override val dhizukuAuthorizedFlow: StateFlow<Boolean> = _dhizukuAuthorizedFlow.asStateFlow()

    // --- Root Dynamic Flow ---
    private val _rootModeFlow = MutableStateFlow(RootMode.None)
    override val rootModeFlow: StateFlow<RootMode> = _rootModeFlow.asStateFlow()

    private val _defaultInstallerFlow = MutableStateFlow("Unknown")
    override val defaultInstallerFlow: StateFlow<String> = _defaultInstallerFlow.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        // Register Shizuku listeners to drive state automatically
        Shizuku.addBinderReceivedListenerSticky {
            updateShizukuModeInternal()
        }

        Shizuku.addBinderDeadListener {
            _shizukuModeFlow.value = ShizukuMode.NONE
            _shizukuAuthorizedFlow.value = false
            Timber.d("Shizuku binder is dead. Status updated to NONE.")
        }

        refreshPrivilegeStatus()
    }

    override fun refreshPrivilegeStatus() {
        updateShizukuModeInternal()
        updateDefaultInstallerInternal()
        scope.launch(Dispatchers.IO) {
            _rootModeFlow.value = detectRootMode()
            updateDhizukuStatusInternal()
        }
    }

    private fun updateDefaultInstallerInternal() {
        val intent = Intent(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_DEFAULT)
            .setDataAndType(
                "content://storage/emulated/0/test.apk".toUri(),
                "application/vnd.android.package-archive"
            )
        val resolveInfo =
            context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        _defaultInstallerFlow.value =
            resolveInfo?.activityInfo?.let { "${it.packageName}/${it.name}" } ?: "Unknown"
    }

    private fun updateShizukuModeInternal() {
        // Run IPC methods in IO thread to prevent UI stutter
        scope.launch(Dispatchers.IO) {
            try {
                if (Shizuku.pingBinder()) {
                    val mode = ShizukuMode.fromUid(Shizuku.getUid())
                    _shizukuModeFlow.value = mode
                    _shizukuAuthorizedFlow.value =
                        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                    Timber.d("Shizuku binder received. Status updated to: $mode")
                } else {
                    _shizukuModeFlow.value = ShizukuMode.NONE
                    _shizukuAuthorizedFlow.value = false
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse Shizuku UID or check permission.")
                _shizukuModeFlow.value = ShizukuMode.NONE
                _shizukuAuthorizedFlow.value = false
            }
        }
    }

    private suspend fun updateDhizukuStatusInternal() = withContext(Dispatchers.IO) {
        try {
            // Dhizuku.init() performs IPC and ContentResolver query, must be called on IO thread
            val available = Dhizuku.init(context)
            _dhizukuAvailableFlow.value = available

            _dhizukuAuthorizedFlow.value = if (available) {
                Dhizuku.isPermissionGranted()
            } else {
                false
            }
            Timber.d("Dhizuku status updated. Available: $available")
        } catch (e: Exception) {
            Timber.e(e, "Failed to check Dhizuku status.")
            _dhizukuAvailableFlow.value = false
            _dhizukuAuthorizedFlow.value = false
        }
    }

    private suspend fun detectRootMode(): RootMode = withContext(Dispatchers.IO) {
        if (checkBinaryViaSu("ksud -V")) return@withContext RootMode.KernelSU
        if (checkBinaryViaSu("magisk -v")) return@withContext RootMode.Magisk
        if (checkBinaryViaSu("apd -V")) return@withContext RootMode.APatch

        RootMode.None
    }

    private fun checkBinaryViaSu(command: String) =
        try {
            // Execute the specific binary check within the su environment
            val process = ProcessBuilder("su", "-c", command)
                .redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor()
            if (exitCode == 0) {
                Timber.d("RootDetection: Successfully executed -> su -c '$command'")
                true
            } else {
                Timber.d("RootDetection: Command 'su -c '$command'' failed with exit code: $exitCode")
                false
            }
        } catch (e: CancellationException) {
            // Rethrow to maintain structured concurrency
            throw e
        } catch (e: Exception) {
            // Catch IOExceptions or other runtime errors
            Timber.d("RootDetection: Execution failed for 'su -c '$command'': ${e.message}")
            false
        }

    private fun calculateSessionInstallSupport(): Boolean {
        val isMi = DeviceConfig.currentManufacturer == Manufacturer.XIAOMI
        if (!isMi) return true

        val miPackageManagerVersion = getMiuiPackageInstallerVersion()
        return if (miPackageManagerVersion != null) {
            miPackageManagerVersion.second >= MIN_SUPPORTED_MIUI_VERSION_CODE
        } else {
            true
        }
    }

    private fun getMiuiPackageInstallerVersion(): Pair<String, Long>? =
        try {
            val info = context.packageManager.getPackageInfo(MIUI_PACKAGE_INSTALLER, 0)
            val versionName = info.versionName ?: ""
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
            versionName to versionCode
        } catch (_: PackageManager.NameNotFoundException) {
            null
        } catch (e: Throwable) {
            Timber.e(e, "Failed to retrieve MIUI package installer version")
            null
        }

    private fun getSamsungProductName(): String? {
        if (DeviceConfig.currentManufacturer != Manufacturer.SAMSUNG) return null

        val productName = getSamsungProductNameFromFloatingFeature()
            ?: getSamsungProductNameFromFloatingFeatureFile()

        return productName.withManufacturerPrefix(Manufacturer.SAMSUNG)
    }

    private fun getSamsungProductNameFromFloatingFeature(): String? =
        try {
            val clazz = Class.forName(SAMSUNG_FLOATING_FEATURE_CLASS)
            val instance = reflect.invokeStatic<Any>("getInstance", clazz) ?: return null
            val value = reflect.invoke<String>(
                instance,
                "getString",
                clazz,
                arrayOf(String::class.java),
                KEY_SAMSUNG_PRODUCT_NAME
            )

            value.takeIfValidDeviceName()
        } catch (e: Throwable) {
            Timber.d(e, "Failed to read Samsung product name from SemFloatingFeature")
            null
        }

    private fun getSamsungProductNameFromFloatingFeatureFile(): String? {
        val pattern = Regex(
            "<$KEY_SAMSUNG_PRODUCT_NAME>\\s*(.*?)\\s*</$KEY_SAMSUNG_PRODUCT_NAME>",
            RegexOption.DOT_MATCHES_ALL
        )

        return SAMSUNG_FLOATING_FEATURE_PATHS.firstNotNullOfOrNull { path ->
            try {
                pattern.find(File(path).readText())
                    ?.groupValues
                    ?.getOrNull(1)
                    .takeIfValidDeviceName()
            } catch (_: Throwable) {
                null
            }
        }
    }

    private fun String?.takeIfValidDeviceName(): String? =
        this?.trim()
            ?.takeIf {
                it.isNotEmpty() &&
                        !TextUtils.equals(it, Build.UNKNOWN) &&
                        !TextUtils.equals(it, "unknown") &&
                        !TextUtils.equals(it, "null")
            }

    private fun String?.withManufacturerPrefix(manufacturer: Manufacturer): String? {
        val name = this.takeIfValidDeviceName() ?: return null
        val displayName = manufacturer.displayName

        if (manufacturer == Manufacturer.UNKNOWN) return name

        return if (name.startsWith(displayName, ignoreCase = true)) {
            name
        } else {
            "$displayName $name"
        }
    }

    private fun getSystemProperty(key: String, defaultValue: String = ""): String? =
        reflect.invokeStatic<String>(
            "get",
            systemPropertiesClass,
            arrayOf(String::class.java, String::class.java),
            key,
            defaultValue
        )?.takeIf { it.isNotEmpty() }
}
