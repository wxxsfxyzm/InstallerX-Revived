package com.rosan.installer.build

import android.os.Build
import android.text.TextUtils
import com.rosan.installer.BuildConfig

object RsConfig {
    val LEVEL: Level = getLevel()

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

    val deviceName: String
        get() {
            var manufacturer = Build.MANUFACTURER.uppercase()
            val brand = Build.BRAND.uppercase()
            if (!TextUtils.equals(brand, manufacturer)) manufacturer += " $brand"
            manufacturer += " " + Build.MODEL
            return manufacturer
        }

    val systemStruct: String
        get() {
            var struct = System.getProperty("os.arch") ?: "unknown"
            val abis = Build.SUPPORTED_ABIS
            struct += if (abis.isEmpty()) {
                " (Not Supported Native ABI)"
            } else {
                val supportABIs = abis.joinToString(", ")
                " ($supportABIs)"
            }
            return struct
        }
}