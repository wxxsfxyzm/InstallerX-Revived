package com.rosan.installer.build

import android.os.Build
import android.text.TextUtils
import com.rosan.installer.BuildConfig

object RsConfig {
    val LEVEL: Level = getLevel()
    val isDebug: Boolean = BuildConfig.DEBUG

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
     * 获取当前设备的制造商枚举。
     * 这个属性会读取系统的制造商信息，并将其匹配到预定义的 Manufacturer 枚举上。
     * @return 返回匹配到的 Manufacturer 枚举常量，如果不在列表中则返回 UNKNOWN。
     */
    val currentManufacturer: Manufacturer
        get() {
            // 获取制造商字符串并转换为大写，以便进行不区分大小写的比较
            val manufacturerString = manufacturer
            // 使用 when 表达式将字符串匹配到枚举
            return when (manufacturerString) {
                "GOOGLE" -> Manufacturer.GOOGLE
                "HUAWEI" -> Manufacturer.HUAWEI
                "HONOR" -> Manufacturer.HONOR
                "OPPO" -> Manufacturer.OPPO
                "VIVO" -> Manufacturer.VIVO
                "XIAOMI" -> Manufacturer.XIAOMI
                "ONEPLUS" -> Manufacturer.ONEPLUS
                "REALME" -> Manufacturer.REALME
                "SAMSUNG" -> Manufacturer.SAMSUNG
                "SONY" -> Manufacturer.SONY
                "ASUS" -> Manufacturer.ASUS
                "MOTOROLA" -> Manufacturer.MOTOROLA
                "NOKIA" -> Manufacturer.NOKIA
                "LG" -> Manufacturer.LG
                "ZTE" -> Manufacturer.ZTE
                "LENOVO" -> Manufacturer.LENOVO
                "MEIZU" -> Manufacturer.MEIZU
                "SMARTISAN" -> Manufacturer.SMARTISAN
                "BLACKSHARK" -> Manufacturer.BLACKSHARK
                else -> Manufacturer.UNKNOWN // 如果没有匹配到任何已知厂商，返回 UNKNOWN
            }
        }

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
        get() {
            // Build.SUPPORTED_ABIS lists supported ABIs in order of preference.
            // The first element is the most preferred one for the running system.
            val primaryAbi = Build.SUPPORTED_ABIS.firstOrNull()
            // Use the helper function in the enum to safely convert the string to an enum type.
            return Architecture.fromArchString(primaryAbi)
        }

    // Used for displaying the system architecture in a user-friendly format.
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