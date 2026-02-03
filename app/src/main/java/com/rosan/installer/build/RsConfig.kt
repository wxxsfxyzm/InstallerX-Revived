package com.rosan.installer.build

import android.content.res.Resources
import android.os.Build
import android.text.TextUtils
import com.rosan.installer.BuildConfig
import com.rosan.installer.build.model.entity.Architecture
import com.rosan.installer.build.model.entity.Density
import com.rosan.installer.build.model.entity.Level
import com.rosan.installer.build.model.entity.Manufacturer
import com.rosan.installer.util.convertLegacyLanguageCode

object RsConfig {
    // Static configs: Computed once
    val LEVEL: Level = when (BuildConfig.BUILD_LEVEL) {
        1 -> Level.PREVIEW
        2 -> Level.STABLE
        else -> Level.UNSTABLE
    }

    val isDebug: Boolean = BuildConfig.DEBUG
    const val isInternetAccessEnabled: Boolean = BuildConfig.INTERNET_ACCESS_ENABLED
    const val VERSION_NAME: String = BuildConfig.VERSION_NAME
    const val VERSION_CODE: Int = BuildConfig.VERSION_CODE

    val isLogEnabled = LEVEL == Level.PREVIEW || LEVEL == Level.UNSTABLE || isDebug

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

    // Lazy properties: Computed only once upon first access for performance

    /**
     * Cached Manufacturer enum using the centralized lookup logic.
     */
    val currentManufacturer: Manufacturer by lazy {
        Manufacturer.from(Build.MANUFACTURER)
    }

    /**
     * Cached list of supported architectures.
     * Maps Build.SUPPORTED_ABIS to the safe Enum.
     */
    val supportedArchitectures: List<Architecture> by lazy {
        Build.SUPPORTED_ABIS.mapNotNull { abiString ->
            Architecture.fromArchString(abiString)
        }
    }

    /**
     * The primary architecture (most preferred).
     */
    val currentArchitecture: Architecture by lazy {
        supportedArchitectures.firstOrNull() ?: Architecture.UNKNOWN
    }

    val isArm: Boolean by lazy {
        when (currentArchitecture) {
            Architecture.ARM64, Architecture.ARM, Architecture.ARMEABI -> true
            else -> false
        }
    }

    val isX86: Boolean by lazy {
        when (currentArchitecture) {
            Architecture.X86_64, Architecture.X86 -> true
            else -> false
        }
    }

    /**
     * Cached supported densities using the specific prioritization logic
     * defined in the Density enum companion object.
     */
    val supportedDensities: List<Density> by lazy {
        Density.getPrioritizedList()
    }

    /**
     * Supported Locales.
     * Dynamic Getter: Users might change system language at runtime.
     */
    val supportedLocales: List<String>
        get() {
            val locales = Resources.getSystem().configuration.locales
            if (locales.isEmpty) return listOf("base")

            val languages = mutableSetOf<String>()
            for (i in 0 until locales.size()) {
                val locale = locales.get(i)
                // Extract base language (e.g. "en" from "en-US")
                val langCode = locale.toLanguageTag().substringBefore('-')
                languages.add(langCode.convertLegacyLanguageCode())
            }
            languages.add("base")
            return languages.toList()
        }
}