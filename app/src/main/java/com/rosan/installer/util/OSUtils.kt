package com.rosan.installer.util

import android.annotation.SuppressLint
import com.rosan.installer.data.reflect.repo.ReflectRepo
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

object OSUtils : KoinComponent {
    private val reflect = get<ReflectRepo>()

    // Key for MIUI version name
    private const val KEY_MIUI_VERSION_NAME = "ro.miui.ui.version.name"

    // Key for HyperOS version name
    private const val KEY_HYPEROS_VERSION_NAME = "ro.hyperos.version.name"

    /**
     * Checks if the device is running HyperOS.
     */
    fun isHyperOS(): Boolean {
        return !getSystemProperty(KEY_HYPEROS_VERSION_NAME).isNullOrEmpty()
    }

    /**
     * Checks if the device is running MIUI.
     */
    fun isMIUI(): Boolean {
        return !getSystemProperty(KEY_MIUI_VERSION_NAME).isNullOrEmpty()
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
