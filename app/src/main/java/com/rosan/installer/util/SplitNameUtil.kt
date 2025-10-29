package com.rosan.installer.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import com.rosan.installer.build.Architecture
import java.util.Locale

const val BASE_PREFIX = "base-"
const val SPLIT_PREFIX = "split-"
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
    // Extract the actual configuration qualifier by stripping all known prefixes.
    val qualifier = this
        .removePrefix(BASE_PREFIX)
        .removePrefix(SPLIT_CONFIG_PREFIX)
        .removePrefix(CONFIG_PREFIX)

    // --- Try to parse as CPU Architecture (ABI) ---
    val architecture = Architecture.fromArchString(qualifier)
    if (architecture != Architecture.UNKNOWN) {
        return stringResource(R.string.split_name_architecture, architecture.displayName)
    }

    // --- Try to parse as Screen Density (DPI) ---
    val dpiResId = getDpiStringResourceId(qualifier)
    if (dpiResId != null) {
        return stringResource(R.string.split_name_density, stringResource(dpiResId))
    }
    // Handle numeric DPI formats like "480dpi"
    if (qualifier.endsWith("dpi") && qualifier.removeSuffix("dpi").all { it.isDigit() }) {
        return stringResource(R.string.split_name_density, qualifier)
    }

    // --- Try to parse as Language (less specific, so checked last) ---
    val languageDisplayName = getLanguageDisplayName(qualifier)
    if (languageDisplayName != null) {
        return stringResource(R.string.split_name_language, languageDisplayName)
    }

    // If it's none of the above, it's likely a feature split or unknown.
    // Returning the original, full splitName is a sensible fallback.
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

/**
 * Defines the category of an application split.
 */
enum class SplitType {
    ARCHITECTURE,
    LANGUAGE,
    DENSITY,
    FEATURE
}

/**
 * Analyzes the split name and returns its [SplitType].
 *
 * @return The categorized [SplitType].
 */
fun String.getSplitType(): SplitType {
    val qualifier = this
        .removePrefix(BASE_PREFIX)
        .removePrefix(SPLIT_CONFIG_PREFIX)
        .removePrefix(CONFIG_PREFIX)

    // Check Arch
    if (Architecture.fromArchString(qualifier) != Architecture.UNKNOWN) {
        return SplitType.ARCHITECTURE
    }

    // Check DPI
    if (getDpiStringResourceId(qualifier) != null) {
        return SplitType.DENSITY
    }
    if (qualifier.endsWith("dpi") && qualifier.removeSuffix("dpi").all { it.isDigit() }) {
        return SplitType.DENSITY
    }

    // Check Language
    if (getLanguageDisplayName(qualifier) != null) {
        return SplitType.LANGUAGE
    }

    return SplitType.FEATURE
}

/**
 * Returns the localized display name for a [SplitType] group.
 */
@Composable
fun SplitType.getDisplayName(): String {
    return when (this) {
        SplitType.ARCHITECTURE -> stringResource(R.string.split_name_architecture_group_title)
        SplitType.LANGUAGE -> stringResource(R.string.split_name_language_group_title)
        SplitType.DENSITY -> stringResource(R.string.split_name_density_group_title)
        SplitType.FEATURE -> stringResource(R.string.split_name_feature_group_title)
    }
}