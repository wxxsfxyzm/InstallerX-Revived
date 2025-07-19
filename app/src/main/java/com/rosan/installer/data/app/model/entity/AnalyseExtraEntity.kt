package com.rosan.installer.data.app.model.entity

import com.rosan.installer.data.app.util.DataType

/**
 * 包含了分析过程中可能需要的附加信息。
 * @param cacheDirectory 用于存放临时文件的缓存目录路径。
 * @param dataType 【新增】 由顶层分析器确定的文件容器类型。
 */
data class AnalyseExtraEntity(
    val cacheDirectory: String,
    // --- 新增字段 ---
    val dataType: DataType? = null
)