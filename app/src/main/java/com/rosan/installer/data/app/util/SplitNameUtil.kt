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
private const val CONFIG_INFIX = ".config." // 用于识别 Feature 中的配置段

/**
 * Split 的主要展示分类 (UI用途)
 */
enum class SplitType {
    ARCHITECTURE,
    LANGUAGE,
    DENSITY,
    FEATURE
}

/**
 * Split 的过滤类型 (选择逻辑用途)
 */
enum class FilterType {
    NONE,       // 无限制，通用的 Feature
    ABI,        // 架构相关
    DENSITY,    // 屏幕密度相关
    LANGUAGE    // 语言相关
}

/**
 * 解析结果元数据
 * @property type 主分类 (用于 UI 分组)
 * @property filterType 过滤类型 (用于安装选择算法)
 * @property configValue 具体的配置值 (如 "arm64-v8a", "zh-CN", "xxhdpi")
 */
data class SplitMetadata(
    val type: SplitType,
    val filterType: FilterType,
    val configValue: String?
)

/**
 * [Public API] 解析入口
 */
fun String.parseSplitMetadata(): SplitMetadata {
    // 1. 预处理：去除扩展名
    val rawName = this.removeSuffix(".apk")

    // 2. 提取核心限定符 (Qualifier) 和是否为 Feature 的线索
    // 逻辑：如果是标准 split_config 开头，它是纯 Config；否则可能是 Feature
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

    // 3. 提取潜在的配置部分 (Config Part)
    // 如果文件名包含 ".config." (如 split_feature_map.config.arm64_v8a)，则取后缀
    // 如果不包含，则整体就是潜在的 config (如 arm64_v8a)
    val potentialConfig = if (qualifier.contains(CONFIG_INFIX)) {
        qualifier.substringAfterLast(CONFIG_INFIX)
    } else {
        qualifier
    }

    // 4. 尝试匹配配置类型 (ABI > Density > Language)

    // 4.1 Check Architecture
    // 标准化: 统一处理 x86_64 和 x86-64。
    // 将下划线转为横杠进行统一匹配，或根据 Architecture 枚举的具体实现调整
    val normalizedArch = potentialConfig.replace('_', '-')
    // 尝试两种情况：原样(可能有下划线) 和 标准化(横杠)
    val arch = Architecture.fromArchString(potentialConfig).takeIf { it != Architecture.UNKNOWN }
        ?: Architecture.fromArchString(normalizedArch).takeIf { it != Architecture.UNKNOWN }

    if (arch != null) {
        // 即使是 Feature，如果带有 Arch 后缀，FilterType 也是 ABI
        val type = if (isLikelyFeature && qualifier.contains(CONFIG_INFIX)) SplitType.FEATURE else SplitType.ARCHITECTURE
        // 返回标准化的 arch 字符串 (通常 Architecture 枚举里有标准写法，这里假设用 arch 字段)
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

    // 5. Fallback: 这是一个没有任何 Config 后缀的纯 Feature
    return SplitMetadata(SplitType.FEATURE, FilterType.NONE, null)
}

/**
 * [Public API] 获取显示名称
 */
@Composable
fun getSplitDisplayName(type: SplitType, configValue: String?, fallbackName: String): String {
    // Feature 类型即使有 configValue (如 arm64)，通常也优先显示原始功能名，
    // 或者你可以根据需求修改这里，显示 "Feature Name (arm64)"
    if (type == SplitType.FEATURE) {
        return fallbackName
    }

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

// --- Helper Methods (保持原样或微调) ---

private fun getLanguageDisplayName(code: String): String? {
    // 严格校验：防止长文件名被误认为语言
    if (code.length > 8 || code.contains(".") || code.contains("_")) return null
    return try {
        val locale = Locale.forLanguageTag(code)
        if (locale.language.isNotEmpty() && locale.displayLanguage.lowercase() != code.lowercase()) {
            locale.getDisplayName(Locale.getDefault())
        } else null
    } catch (e: Exception) {
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