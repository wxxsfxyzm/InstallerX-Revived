package com.rosan.installer.domain.engine.model

import com.rosan.installer.domain.engine.model.source.DataType

/**
 * Includes extra data during analyse
 * @param cacheDirectory Cache directory to store files
 * @param dataType The type of data being analysed.
 * @param isModuleFlashEnabled Whether module flash is enabled.
 * @param checkAppSignature Whether APK signatures should be analysed.
 * @param checkSplitPackageSignatures Whether APK signatures inside APKS, APKM, and XAPK should be analysed.
 */
data class AnalyseExtraEntity(
    val cacheDirectory: String,
    val dataType: DataType? = null,
    val isModuleFlashEnabled: Boolean = false,
    val checkAppSignature: Boolean = true,
    val checkSplitPackageSignatures: Boolean = false,
) {
    fun shouldCheckAppSignatures(type: DataType? = dataType): Boolean = checkAppSignature && (checkSplitPackageSignatures || type !in SPLIT_PACKAGE_TYPES)

    private companion object {
        val SPLIT_PACKAGE_TYPES = setOf(DataType.APKS, DataType.APKM, DataType.XAPK)
    }
}
