package com.rosan.installer.build

import android.content.res.Resources
import android.os.Build
import android.text.TextUtils
import com.rosan.installer.BuildConfig

object RsConfig {
    val LEVEL: Level = getLevel()
    val isDebug: Boolean = BuildConfig.DEBUG
    val isInternetAccessEnabled
        get() = BuildConfig.INTERNET_ACCESS_ENABLED

    private fun getLevel(): Level {
        return when (BuildConfig.BUILD_LEVEL) {
            1 -> Level.PREVIEW
            2 -> Level.STABLE
            else -> Level.UNSTABLE
        }
    }

    const val VERSION_NAME: String = BuildConfig.VERSION_NAME
    const val VERSION_CODE: Int = BuildConfig.VERSION_CODE

    val systemVersion: String
        get() = if (Build.VERSION.PREVIEW_SDK_INT != 0)
            "%s Preview (API %s)".format(Build.VERSION.CODENAME, Build.VERSION.SDK_INT)
        else
            "%s (API %s)".format(Build.VERSION.RELEASE, Build.VERSION.SDK_INT)

    val manufacturer: String
        get() = Build.MANUFACTURER.uppercase()

    /**
     * The current device manufacturer's enum representation.
     * The lookup logic is centralized in the Manufacturer.from() companion object function.
     * @return The matching Manufacturer enum, or UNKNOWN if not found.
     */
    val currentManufacturer: Manufacturer
        get() = Manufacturer.from(Build.MANUFACTURER)

    val brand = Build.BRAND.uppercase()

    val deviceName: String
        get() = if (!TextUtils.equals(brand, manufacturer))
            manufacturer + " " + brand + " " + Build.MODEL
        else
            manufacturer + " " + Build.MODEL

    /**
     * The primary architecture of the device.
     * This property determines the most preferred ABI supported by the device
     * and maps it to a safe enum type.
     * @return The corresponding Architecture enum constant.
     */
    val currentArchitecture: Architecture
        get() = supportedArchitectures.first()

    /**
     * Gets a prioritized list of supported architectures for the device.
     * The list is ordered from the most preferred to the least preferred.
     *
     * @return A List of Architecture enums, sorted by preference.
     */
    val supportedArchitectures: List<Architecture>
        get() =
        // Build.SUPPORTED_ABIS is already ordered by system preference.
            // We just map the string values to our type-safe enum.
            Build.SUPPORTED_ABIS.mapNotNull { abiString ->
                Architecture.fromArchString(abiString)
            }

    /**
     * Checks if the primary device architecture is part of the ARM family.
     * @return True if the architecture is ARM64, ARMv7a, or ARMEABI.
     */
    val isArm: Boolean
        get() = when (currentArchitecture) {
            Architecture.ARM64, Architecture.ARM, Architecture.ARMEABI -> true
            else -> false
        }

    /**
     * Checks if the primary device architecture is part of the x86 family.
     * @return True if the architecture is x86_64 or x86.
     */
    val isX86: Boolean
        get() = when (currentArchitecture) {
            Architecture.X86_64, Architecture.X86 -> true
            else -> false
        }

    /**
     * Gets a prioritized list of screen densities for the device.
     * The logic prefers densities that are equal to or higher than the device's native density,
     * followed by lower densities, to ensure the best possible asset quality is chosen first.
     *
     * @return A List of Density enums, sorted by preference.
     */
    val supportedDensities: List<Density>
        get() = Density.getPrioritizedList()

    /**
     * Gets a prioritized list of languages based on the user's system settings.
     * This version strictly extracts language codes without any modification or conversion.
     *
     * Logic is from the PackageUtil.kt file in the PackageInstaller project.
     * Under Apache License 2.0.
     *
     * @return A List of language code Strings, sorted by user preference, with "base" as a fallback.
     * @see <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache License 2.0</a>
     * @see <a href="https://github.com/vvb2060/PackageInstaller/blob/master/app/src/main/java/io/github/vvb2060/packageinstaller/model/PackageUtil.kt">PackageUtil</a>
     */
    val supportedLocales: List<String>
        get() {
            val res = Resources.getSystem()
            // Use a LinkedHashSet to preserve insertion order while ensuring uniqueness.
            val languageSet = LinkedHashSet<String>()
            val locales = res.configuration.locales

            if (!locales.isEmpty) {
                for (i in 0 until locales.size()) {
                    val locale = locales.get(i)
                    // Get the base language code (e.g., "en" from "en-US") and add it directly.
                    // No conversion logic is applied.
                    val langCode = locale.toLanguageTag().substringBefore('-')
                    languageSet.add(langCode.convertLanguageCode())
                }
            }

            // Always add "base" as the ultimate fallback.
            languageSet.add("base")
            return languageSet.toList()
        }

    /**
     * Converts legacy ISO 639 language codes to their modern equivalents.
     * This ensures compatibility with older and non-standard APK splits.
     *
     * Logic is from the PackageUtil.kt file in the PackageInstaller project.
     * Under Apache License 2.0.
     *
     * @param code The language code to convert.
     * @return The converted, modern language code.
     *
     * @see <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache License 2.0</a>
     * @see <a href="https://github.com/vvb2060/PackageInstaller/blob/master/app/src/main/java/io/github/vvb2060/packageinstaller/model/PackageUtil.kt">PackageUtil</a>
     */
    private fun String.convertLanguageCode(): String =
        when (this) {
            "tl" -> "fil" // Tagalog -> Filipino
            "iw" -> "he"  // Old Hebrew -> Hebrew
            "ji" -> "yi"  // Old Yiddish -> Yiddish
            "in" -> "id"  // Old Indonesian -> Indonesian
            else -> this
        }
}