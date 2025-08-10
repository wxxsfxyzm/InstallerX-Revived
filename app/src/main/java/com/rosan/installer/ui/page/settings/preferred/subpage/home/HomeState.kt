package com.rosan.installer.ui.page.settings.preferred.subpage.home

import android.os.Build
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import com.rosan.installer.build.Level
import com.rosan.installer.build.RsConfig

/**
 * just constant data, so just use it without ViewModel.
 * */
data class HomeState(
    val level: Level = RsConfig.LEVEL,
    val versionInfo: String = "${RsConfig.VERSION_NAME} (${RsConfig.VERSION_CODE})",
    val systemVersion: String = if (Build.VERSION.PREVIEW_SDK_INT != 0)
        String.format("%1\$s Preview (API %2\$s)", Build.VERSION.CODENAME, Build.VERSION.SDK_INT)
    else
        String.format("%1\$s (API %2\$s)", Build.VERSION.RELEASE, Build.VERSION.SDK_INT),
    val deviceName: String = run {
        var manufacturer = Build.MANUFACTURER.toUpperCase(Locale.current)
        val brand = Build.BRAND.toUpperCase(Locale.current)
        if (brand != manufacturer) {
            manufacturer += " $brand"
        }
        manufacturer += " ${Build.MODEL}"
        manufacturer
    },
    val systemStruct: String = run {
        var struct = System.getProperty("os.arch") ?: "unknown"
        val abis = Build.SUPPORTED_ABIS
        struct += if (abis.isEmpty()) {
            " (Not supported Native ABI)"
        } else {
            " (${abis.joinToString(", ")})"
        }
        struct
    }
)