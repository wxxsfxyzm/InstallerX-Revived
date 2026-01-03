package com.rosan.installer.data.app.model.entity

import com.rosan.installer.data.app.model.enums.DataType

/**
 * Includes extra data during analyse
 * @param cacheDirectory Cache directory to store files
 * @param dataType The type of data being analysed.
 * @param isModuleFlashEnabled Whether module flash is enabled.
 */
data class AnalyseExtraEntity(
    val cacheDirectory: String,
    val dataType: DataType? = null,
    val isModuleFlashEnabled: Boolean = false
)