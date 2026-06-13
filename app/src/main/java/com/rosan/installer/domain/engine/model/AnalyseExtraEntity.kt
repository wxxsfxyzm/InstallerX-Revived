package com.rosan.installer.domain.engine.model

import com.rosan.installer.domain.engine.model.source.DataType

/**
 * Includes extra data during analyse
 * @param cacheDirectory Cache directory to store files
 * @param dataType The type of data being analysed.
 * @param isModuleFlashEnabled Whether module flash is enabled.
 * @param checkAppSignature Whether APK signatures should be analysed.
 */
data class AnalyseExtraEntity(
    val cacheDirectory: String,
    val dataType: DataType? = null,
    val isModuleFlashEnabled: Boolean = false,
    val checkAppSignature: Boolean = true
)
