package com.rosan.installer.data.app.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import com.rosan.installer.build.model.entity.Architecture
import java.util.Locale

private const val BASE_PREFIX = "base-"
private const val SPLIT_PREFIX = "split-"
private const val SPLIT_CONFIG_PREFIX = "split_config."
private const val CONFIG_PREFIX = "config."
private const val CONFIG_INFIX = ".config." // Identifies config section in features

/**
 * UI categorization for splits.
 */
enum class SplitType {
    ARCHITECTURE,
    LANGUAGE,
    DENSITY,
    FEATURE
}

/**
 * Selection logic for splits.
 */
enum class FilterType {
    NONE,       // Generic feature
    ABI,
    DENSITY,
    LANGUAGE
}

/**
 * Holds parsed split metadata.
 *
 * @property type UI grouping category.
 * @property filterType Selection algorithm category.
 * @property configValue Specific value (e.g., "arm64-v8a", "zh-CN").
 */
data class SplitMetadata(
    val type: SplitType,
    val filterType: FilterType,
    val configValue: String?
)

/**
 * Parses the split filename into metadata.
 */
fun String.parseSplitMetadata(): SplitMetadata {
    // 1. Remove extension
    val rawName = this.removeSuffix(".apk")

    // 2. Extract qualifier
    var qualifier = rawName
    var isLikelyFeature = true

    if (qualifier.startsWith(SPLIT_CONFIG_PREFIX)) {
        qualifier = qualifier.removePrefix(SPLIT_CONFIG_PREFIX)
        isLikelyFeature = false
    } else if (qualifier.startsWith(CONFIG_PREFIX)) {
        qualifier = qualifier.removePrefix(CONFIG_PREFIX)
        isLikelyFeature = false
    } else {
        qualifier = qualifier
            .removePrefix(BASE_PREFIX)
            .removePrefix(SPLIT_PREFIX)
    }

    // 3. Extract potential config
    // If ".config." exists (e.g., split_feature_map.config.arm64_v8a), take the suffix.
    val potentialConfig = if (qualifier.contains(CONFIG_INFIX)) {
        qualifier.substringAfterLast(CONFIG_INFIX)
    } else {
        qualifier
    }

    // 4. Match config type (ABI > Density > Language)

    // 4.1 Check Architecture
    val normalizedArch = potentialConfig.replace('_', '-')
    val arch = Architecture.fromArchString(potentialConfig).takeIf { it != Architecture.UNKNOWN }
        ?: Architecture.fromArchString(normalizedArch).takeIf { it != Architecture.UNKNOWN }

    if (arch != null) {
        val type = if (isLikelyFeature && qualifier.contains(CONFIG_INFIX)) SplitType.FEATURE else SplitType.ARCHITECTURE
        return SplitMetadata(type, FilterType.ABI, arch.arch)
    }

    // 4.2 Check Density
    val dpiResId = getDpiStringResourceId(potentialConfig)
    if (dpiResId != null || (potentialConfig.endsWith("dpi") && potentialConfig.removeSuffix("dpi").all { it.isDigit() })) {
        val type = if (isLikelyFeature && qualifier.contains(CONFIG_INFIX)) SplitType.FEATURE else SplitType.DENSITY
        return SplitMetadata(type, FilterType.DENSITY, potentialConfig)
    }

    // 4.3 Check Language
    val languageDisplayName = getLanguageDisplayName(potentialConfig)
    if (languageDisplayName != null) {
        val type = if (isLikelyFeature && qualifier.contains(CONFIG_INFIX)) SplitType.FEATURE else SplitType.LANGUAGE
        return SplitMetadata(type, FilterType.LANGUAGE, potentialConfig)
    }

    // 5. Fallback to generic feature
    return SplitMetadata(SplitType.FEATURE, FilterType.NONE, null)
}

/**
 * Returns the display name for UI.
 */
@Composable
fun getSplitDisplayName(type: SplitType, configValue: String?, fallbackName: String): String {
    val qualifier = configValue ?: fallbackName
    return when (type) {
        SplitType.ARCHITECTURE -> {
            val arch = Architecture.fromArchString(qualifier.replace('-', '_'))
            stringResource(R.string.split_name_architecture, arch.displayName)
        }

        SplitType.DENSITY -> {
            val dpiResId = getDpiStringResourceId(qualifier)
            if (dpiResId != null) stringResource(R.string.split_name_density, stringResource(dpiResId))
            else stringResource(R.string.split_name_density, qualifier)
        }

        SplitType.LANGUAGE -> {
            getLanguageDisplayName(qualifier)?.let {
                stringResource(R.string.split_name_language, it)
            } ?: stringResource(R.string.split_name_language, qualifier)
        }

        else -> fallbackName
    }
}

// --- Helper Methods ---

private fun getLanguageDisplayName(code: String): String? {
    // Strict validation to avoid false positives (e.g. long filenames)
    if (code.length > 8 || code.contains(".") || code.contains("_")) return null
    return try {
        val locale = Locale.forLanguageTag(code)
        // Check if the locale is valid and recognized
        if (locale.language.isNotEmpty() && locale.displayLanguage.lowercase() != code.lowercase()) {
            locale.getDisplayName(Locale.getDefault())
        } else null
    } catch (_: Exception) {
        null
    }
}

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

@Composable
fun SplitType.getDisplayName(): String {
    return when (this) {
        SplitType.ARCHITECTURE -> stringResource(R.string.split_name_architecture_group_title)
        SplitType.LANGUAGE -> stringResource(R.string.split_name_language_group_title)
        SplitType.DENSITY -> stringResource(R.string.split_name_density_group_title)
        SplitType.FEATURE -> stringResource(R.string.split_name_feature_group_title)
    }
}