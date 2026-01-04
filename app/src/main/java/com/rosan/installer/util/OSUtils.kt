package com.rosan.installer.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import com.rosan.installer.data.reflect.repo.ReflectRepo
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject

object OSUtils : KoinComponent {
    private val reflect = get<ReflectRepo>()
    private val context by inject<Context>()

    // MIUI legacy key (exists in both MIUI and HyperOS)
    private const val KEY_MIUI_VERSION_NAME = "ro.miui.ui.version.name"

    // HyperOS real key (most reliable)
    private const val KEY_MI_OS_VERSION_NAME = "ro.mi.os.version.name"

    // Keys for OPPO OSdkVersion
    private const val KEY_OPLUS_API = "ro.build.version.oplus.api"
    private const val KEY_OPLUS_SUB_API = "ro.build.version.oplus.sub_api"

    /**
     * Checks if the app is installed as a System App.
     * This includes apps in /system/app and /system/priv-app.
     */
    val isSystemApp: Boolean by lazy {
        try {
            (context.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks if the device is running HyperOS.
     */
    fun isHyperOS(): Boolean {
        val osName = getSystemProperty(KEY_MI_OS_VERSION_NAME)
        return !osName.isNullOrEmpty() && osName.startsWith("OS")
    }

    /**
     * Checks if the device is running MIUI.
     */
    fun isMIUI(): Boolean {
        val miuiName = getSystemProperty(KEY_MIUI_VERSION_NAME)
        return !miuiName.isNullOrEmpty() && !isHyperOS()
    }

    /**
     * Get OPPO OSdkVersion.
     * The value is composed of "ro.build.version.oplus.api" and "ro.build.version.oplus.sub_api" joined by a dot.
     * e.g., if api is "30" and sub_api is "1", returns "30.1".
     */
    fun getOplusOSdkVersion(): String? {
        val api = getSystemProperty(KEY_OPLUS_API)
        // If the main API version is missing, it's likely not an applicable device or the property doesn't exist.
        if (api.isNullOrEmpty()) {
            return null
        }

        val subApi = getSystemProperty(KEY_OPLUS_SUB_API)

        // Concatenate with a dot if subApi exists, otherwise return just the api version.
        return if (!subApi.isNullOrEmpty()) {
            "$api.$subApi"
        } else {
            api
        }
    }

    /**
     * Get a system property value using the ReflectRepo
     */
    @SuppressLint("PrivateApi")
    private fun getSystemProperty(key: String): String? {
        try {
            val clz = Class.forName("android.os.SystemProperties")
            // Try public get(key, def) first
            val method = reflect.getMethod(clz, "get", String::class.java, String::class.java)
                ?: reflect.getDeclaredMethod(clz, "get", String::class.java, String::class.java)
                ?: return null

            // It's a static method: pass null as the instance. Use empty string as default.
            val value = method.invoke(null, key, "") as? String
            return value?.takeIf { it.isNotEmpty() }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        return null
    }
}
