package com.rosan.installer.domain.engine.model.install

import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.engine.model.source.DataType
import com.rosan.installer.domain.settings.model.config.ConfigModel

fun List<InstallEntity>.sourcePath(): Array<String> = mapNotNull {
    it.data.sourcePath()
}.distinct().toTypedArray()

/**
 * Resolves the original source identity rather than the current analysis backing source.
 *
 * A provider-derived value may be a virtual path that is unsuitable for direct filesystem access.
 * Consumers that require a real local path must validate or materialize their current data.
 */
fun DataEntity.sourcePath(): String? = when (val source = this.getSourceTop()) {
    is DataEntity.FileEntity -> source.path
    is DataEntity.ZipFileEntity -> source.parent.path
    is DataEntity.SeekableZipEntryEntity -> source.parent.path
    is DataEntity.ZipInputStreamEntity -> source.parent.sourcePath()
    else -> null
}

fun ConfigModel.shouldAutoDeleteSource(sourceType: DataType?): Boolean {
    val requiresArchiveOptIn = when (sourceType) {
        DataType.MULTI_APK_ZIP,
        DataType.MIXED_MODULE_APK,
        DataType.MIXED_MODULE_ZIP -> true

        else -> false
    }
    return autoDelete && (!requiresArchiveOptIn || autoDeleteZip)
}
