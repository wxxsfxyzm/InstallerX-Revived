// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.core.env

import android.content.res.Resources
import android.os.Build
import android.text.TextUtils
import com.rosan.installer.domain.device.model.Architecture
import com.rosan.installer.domain.device.model.Density
import com.rosan.installer.domain.device.model.Manufacturer
import com.rosan.installer.util.convertLegacyLanguageCode

object DeviceConfig {
    val systemVersion: String = if (Build.VERSION.PREVIEW_SDK_INT != 0)
        "%s Preview (API %s)".format(Build.VERSION.CODENAME, Build.VERSION.SDK_INT)
    else
        "%s (API %s)".format(Build.VERSION.RELEASE, Build.VERSION.SDK_INT)

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
