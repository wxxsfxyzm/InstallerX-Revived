package com.rosan.installer.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import com.rosan.installer.build.Architecture
import java.util.Locale

private const val SPLIT_CONFIG_PREFIX = "split_config."
private const val CONFIG_PREFIX = "config."

/**
 * [公开API - Composable]
 *
 * 将原始的 splitName 转换为用户可读的格式。
 * 例如 "split_config.zh" -> "语言: 中文"
 * "split_config.armeabi-v7a" -> "架构: ARMv7a (32-bit)"
 *
 * @author wxxsfxyzm
 * @return 返回一个在 Composable 中使用的本地化字符串。
 */
@Composable
fun String.asUserReadableSplitName(): String {
    // 移除通用的前缀，提取核心名称，如 "zh", "armeabi-v7a"
    val coreName = this.removePrefix(SPLIT_CONFIG_PREFIX).removePrefix(CONFIG_PREFIX)

    // --- 尝试解析为语言 ---
    val languageDisplayName = getLanguageDisplayName(coreName)
    if (languageDisplayName != null) {
        return stringResource(R.string.split_name_language, languageDisplayName)
    }

    // --- 尝试解析为CPU架构 (ABI) ---
    // Call the static-like helper function from the enum's companion object.
    val architecture = Architecture.fromArchString(coreName)
    // Check if the returned enum is not the default 'UNKNOWN' value.
    if (architecture != Architecture.UNKNOWN) {
        // If the string was recognized, use the 'displayName' property from the enum constant.
        return stringResource(R.string.split_name_architecture, architecture.displayName)
    }

    // --- 尝试解析为屏幕密度 (DPI) ---
    val dpiResId = getDpiStringResourceId(coreName)
    if (dpiResId != null) {
        // 使用格式化字符串，将本地化的DPI描述填入
        return stringResource(R.string.split_name_density, stringResource(dpiResId))
    }
    // 特别处理 "480dpi" 这种数字格式，它们本身具有可读性
    if (coreName.endsWith("dpi") && coreName.removeSuffix("dpi").all { it.isDigit() }) {
        return stringResource(R.string.split_name_density, coreName)
    }

    // 如果无法识别，则返回原始的 splitName
    return this
}

/**
 * 根据语言代码，使用 Java 的 Locale 获取其显示名称
 *
 * @param code 语言代码，例如 "zh", "en"
 * @return 本地化的语言名称，如果无法识别则返回 null
 */
private fun getLanguageDisplayName(code: String): String? {
    val locale = Locale.forLanguageTag(code)
    // 确保这是一个有效的语言代码，并且能生成一个不同于代码本身的显示名称
    return if (locale.language.isNotEmpty() && locale.displayLanguage.lowercase() != code.lowercase()) {
        // 返回在当前设备语言环境下的语言名称 (例如，系统为中文时返回"英语"，系统为英文时返回"English")
        locale.getDisplayName(Locale.getDefault())
    } else {
        null
    }
}

/**
 * 识别常见的 ABI 名称并返回更具描述性的版本
 *
 * @param name ABI 名称
 * @return 描述性的 ABI 名称，如果无法识别则返回 null
 */
private fun getAbiDisplayName(name: String): String? {
    return when (name) {
        "armeabi" -> "ARM (32-bit)"
        "armeabi-v7a" -> "ARMv7a (32-bit)"
        "armeabi_v7a" -> "ARMv7a (32-bit)"
        "arm64-v8a" -> "ARMv8a (64-bit)"
        "arm64_v8a" -> "ARMv8a (64-bit)"
        "x86" -> "x86 (32-bit)"
        "x86_64" -> "x86 (64-bit)"
        "mips" -> "MIPS"
        "mips64" -> "MIPS64"
        else -> null
    }
}

/**
 * 识别常见的屏幕密度(DPI)名称并返回更具描述性的版本。
 *
 * @param name DPI 名称
 * @return 描述性的 DPI 名称，如果无法识别则返回 null。
 */
@StringRes
private fun getDpiStringResourceId(name: String): Int? {
    return when (name) {
        "ldpi" -> R.string.split_dpi_ldpi
        "mdpi" -> R.string.split_dpi_mdpi
        "hdpi" -> R.string.split_dpi_hdpi
        "xhdpi" -> R.string.split_dpi_xhdpi
        "xxhdpi" -> R.string.split_dpi_xxhdpi
        "xxxhdpi" -> R.string.split_dpi_xxxhdpi
        "tvdpi" -> R.string.split_dpi_tvdpi
        "nodpi" -> R.string.split_dpi_nodpi
        "anydpi" -> R.string.split_dpi_anydpi
        else -> null
    }
}