package com.rosan.installer.data.app.model.impl.processor

import com.rosan.installer.build.RsConfig
import com.rosan.installer.build.model.entity.Architecture
import com.rosan.installer.build.model.entity.Density
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataType
import com.rosan.installer.data.installer.model.entity.SelectInstallEntity
import com.rosan.installer.util.convertLegacyLanguageCode
import com.rosan.installer.util.isLanguageCode

object SelectionStrategy {

    /**
     * 核心入口：根据容器类型和实体列表，返回带选中状态的列表
     */
    fun select(
        entities: List<AppEntity>,
        containerType: DataType
    ): List<SelectInstallEntity> {
        // 1. 混合模块默认全部不选中 (防止误操作覆盖系统文件)
        if (containerType == DataType.MIXED_MODULE_APK || containerType == DataType.MIXED_MODULE_ZIP) {
            return entities.map { SelectInstallEntity(it, selected = false) }
        }

        // 2. 多应用模式 (Batch Install)：只选最佳 Base
        val isMultiAppMode = containerType == DataType.MULTI_APK || containerType == DataType.MULTI_APK_ZIP
        if (isMultiAppMode) {
            val bestBase = findBestBase(entities.filterIsInstance<AppEntity.BaseEntity>())
            return entities.map { entity ->
                // 只选中被判定为 Best 的 Base，忽略其他 Base 和所有 Split
                val isSelected = (entity == bestBase)
                SelectInstallEntity(entity, selected = isSelected)
            }
        }

        // 3. 单应用模式 (Split Install)：Base + 最佳 Splits
        val optimalSplits = findOptimalSplits(entities.filterIsInstance<AppEntity.SplitEntity>())

        return entities.map { entity ->
            val isSelected = when (entity) {
                is AppEntity.BaseEntity -> true // Base 必选
                is AppEntity.DexMetadataEntity -> true // .dm 文件通常跟随 Base
                is AppEntity.SplitEntity -> entity in optimalSplits
                is AppEntity.CollectionEntity -> false
                is AppEntity.ModuleEntity -> true // 纯模块信息通常默认展示
            }
            SelectInstallEntity(entity, selected = isSelected)
        }
    }

    // --- 内部算法 ---

    /**
     * 在多个 Base APK 中寻找最适合当前设备的版本 (优先 ABI -> 版本号)
     */
    private fun findBestBase(bases: List<AppEntity.BaseEntity>): AppEntity.BaseEntity? {
        if (bases.isEmpty()) return null
        if (bases.size == 1) return bases.first()

        val deviceAbis = RsConfig.supportedArchitectures.map { it.arch }

        return bases.sortedWith(
            compareBy<AppEntity.BaseEntity> { base ->
                // 1. ABI 匹配度 (越小越好，-1 代表不匹配排最后)
                val abiIndex = base.arch?.let { deviceAbis.indexOf(it.arch) } ?: -1
                if (abiIndex == -1) Int.MAX_VALUE else abiIndex
            }
                .thenByDescending { it.versionCode } // 2. 版本号大的优先
                .thenByDescending { it.versionName }
        ).firstOrNull()
    }

    /**
     * 智能筛选 Split APKs
     */
    private fun findOptimalSplits(splits: List<AppEntity.SplitEntity>): Set<AppEntity.SplitEntity> {
        if (splits.isEmpty()) return emptySet()

        // 解析并分类所有 Split
        val parsed = splits.map { parseSplitInfo(it) }
        val grouped = parsed.groupBy { it.category }

        val selected = mutableSetOf<AppEntity.SplitEntity>()

        // 1. ABI 筛选
        selectBestAbi(grouped[SplitCategory.ABI])?.let { selected.addAll(it) }

        // 2. DPI 筛选
        selectBestDensity(grouped[SplitCategory.DENSITY])?.let { selected.addAll(it) }

        // 3. 语言筛选
        selectBestLanguage(grouped[SplitCategory.LANGUAGE])?.let { selected.addAll(it) }

        // 4. Feature Splits (功能模块) 全部保留
        grouped[SplitCategory.FEATURE]?.forEach { selected.add(it.entity) }

        return selected
    }

    // --- 筛选细节逻辑 ---

    private fun selectBestAbi(candidates: List<SplitInfo>?): List<AppEntity.SplitEntity>? {
        if (candidates.isNullOrEmpty()) return null
        // 映射设备支持的 ABI (e.g., ["arm64-v8a", "armeabi-v7a"])
        val deviceAbis = RsConfig.supportedArchitectures.map { it.arch.replace('_', '-') }

        // 按设备优先级查找第一个匹配的 Group
        val bestAbi = deviceAbis.firstOrNull { abi -> candidates.any { it.value == abi } }

        return bestAbi?.let { target ->
            candidates.filter { it.value == target }.map { it.entity }
        }
    }

    private fun selectBestDensity(candidates: List<SplitInfo>?): List<AppEntity.SplitEntity>? {
        if (candidates.isNullOrEmpty()) return null

        // [修复] 这里修改了提取逻辑：从 List<Density> 中映射出 key 字符串列表
        val deviceDensities = RsConfig.supportedDensities.map { it.key }

        val bestDensity = deviceDensities.firstOrNull { density -> candidates.any { it.value == density } }

        return bestDensity?.let { target ->
            candidates.filter { it.value == target }.map { it.entity }
        }
    }

    private fun selectBestLanguage(candidates: List<SplitInfo>?): List<AppEntity.SplitEntity>? {
        if (candidates.isNullOrEmpty()) return null
        val deviceLangs = RsConfig.supportedLocales.map { it.convertLegacyLanguageCode() }

        // 1. 精确匹配 (e.g. zh-CN == zh-CN)
        val exactMatch = deviceLangs.firstOrNull { lang -> candidates.any { it.value == lang } }
        if (exactMatch != null) {
            return candidates.filter { it.value == exactMatch }.map { it.entity }
        }

        // 2. 模糊/前缀匹配 (e.g. zh-CN matches zh)
        val baseLangs = deviceLangs.map { it.substringBefore('-') }
        val fuzzyMatch = candidates.firstOrNull { split ->
            baseLangs.any { split.value.startsWith(it) }
        }

        return fuzzyMatch?.let { target ->
            listOf(target.entity)
        }
    }

    // --- 解析辅助类 ---

    private enum class SplitCategory { ABI, DENSITY, LANGUAGE, FEATURE }

    private data class SplitInfo(
        val category: SplitCategory,
        val value: String,
        val entity: AppEntity.SplitEntity
    )

    private fun parseSplitInfo(split: AppEntity.SplitEntity): SplitInfo {
        // 清洗名称：split_config.arm64_v8a.apk -> config.arm64_v8a
        val rawConfig = split.splitName
            .removeSuffix(".apk")
            .removePrefix("split_")
            .removePrefix("base-") // 某些特殊的命名
            .let { if (it.startsWith("config.")) it.removePrefix("config.") else it }
            .substringAfterLast(".config.") // 处理类似 split_feature.config.xxhdpi

        val normalizedValue = rawConfig.replace('_', '-')

        val category = when {
            Architecture.fromArchString(rawConfig) != Architecture.UNKNOWN -> SplitCategory.ABI
            Density.fromDensityString(rawConfig) != Density.UNKNOWN -> SplitCategory.DENSITY
            isLanguageCode(normalizedValue.substringBefore('-')) -> SplitCategory.LANGUAGE
            else -> SplitCategory.FEATURE
        }

        return SplitInfo(category, normalizedValue, split)
    }
}