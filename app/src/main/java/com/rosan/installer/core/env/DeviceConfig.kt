// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.core.env

import android.content.res.Resources
import android.os.Build
import android.text.TextUtils
import com.rosan.installer.core.device.model.Architecture
import com.rosan.installer.core.device.model.Density
import com.rosan.installer.core.device.model.Manufacturer
import com.rosan.installer.util.convertLegacyLanguageCode

object DeviceConfig {
    /**
     * Resolves the exact API level string, accounting for minor SDK versions introduced in API 36.
     *
     * - For API 35 and below: Returns the standard major integer version (e.g., "35").
     * - For API 36 and above: Appends the precise minor version (e.g., "36.0", "36.1").
     */
    private val exactApiLevel: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            // Extract the minor version using the native modulo operation provided by Android 16+.
            val minorVersion = Build.getMinorSdkVersion(Build.VERSION.SDK_INT_FULL)
            "${Build.VERSION.SDK_INT}.$minorVersion"
        } else {
            Build.VERSION.SDK_INT.toString()
        }

    /**
     * Generates a user-friendly system version string suitable for UI display or logging.
     * Automatically handles both Developer Preview/Beta builds and stable releases.
     *
     * Example outputs:
     * - Stable Android 15: "15 (API 35)"
     * - Stable Android 16 (Initial): "16 (API 36.0)"
     * - Stable Android 16 (QPR Update): "16 (API 36.1)"
     * - Preview Build: "Cinnamon Bun Preview (API 37.0)"
     */
    val systemVersion: String
        get() = if (Build.VERSION.PREVIEW_SDK_INT != 0) {
            "%s Preview (API %s)".format(Build.VERSION.CODENAME, exactApiLevel)
        } else {
            "%s (API %s)".format(Build.VERSION.RELEASE, exactApiLevel)
        }

    val manufacturer: String = Build.MANUFACTURER.uppercase()
    val brand: String = Build.BRAND.uppercase()

    val deviceName: String = if (!TextUtils.equals(brand, manufacturer))
        "$manufacturer $brand ${Build.MODEL}"
    else
        "$manufacturer ${Build.MODEL}"

    val currentManufacturer: Manufacturer by lazy {
        Manufacturer.from(Build.MANUFACTURER)
    }

    val supportedArchitectures: List<Architecture> by lazy {
        Build.SUPPORTED_ABIS.mapNotNull { Architecture.fromArchString(it) }
    }

    val currentArchitecture: Architecture by lazy {
        supportedArchitectures.firstOrNull() ?: Architecture.UNKNOWN
    }

    val isArm: Boolean by lazy {
        currentArchitecture in listOf(Architecture.ARM64, Architecture.ARM, Architecture.ARMEABI)
    }

    val isX86: Boolean by lazy {
        currentArchitecture in listOf(Architecture.X86_64, Architecture.X86)
    }

    val supportedDensities: List<Density> by lazy {
        Density.getPrioritizedList()
    }

    val supportedLocales: List<String>
        get() {
            val locales = Resources.getSystem().configuration.locales
            if (locales.isEmpty) return listOf("base")

            val languages = mutableSetOf<String>()
            for (i in 0 until locales.size()) {
                val langCode = locales.get(i).toLanguageTag().substringBefore('-')
                languages.add(langCode.convertLegacyLanguageCode())
            }
            languages.add("base")
            return languages.toList()
        }
}
