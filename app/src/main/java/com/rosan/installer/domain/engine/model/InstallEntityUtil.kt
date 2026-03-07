package com.rosan.installer.domain.engine.model

fun List<InstallEntity>.sourcePath(): Array<String> = mapNotNull {
    it.data.sourcePath()
}.distinct().toTypedArray()

fun DataEntity.sourcePath(): String? = when (val source = this.getSourceTop()) {
    is DataEntity.FileEntity -> source.path
    is DataEntity.ZipFileEntity -> source.parent.path
    is DataEntity.ZipInputStreamEntity -> source.parent.sourcePath()
    else -> null
}