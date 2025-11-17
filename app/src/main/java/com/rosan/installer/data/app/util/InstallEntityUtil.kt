package com.rosan.installer.data.app.util

import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.model.entity.InstallEntity

fun List<InstallEntity>.sourcePath(): Array<String> = mapNotNull {
    it.data.sourcePath()
}.distinct().toTypedArray()

fun DataEntity.sourcePath(): String? = when (val source = this.getSourceTop()) {
    is DataEntity.FileEntity -> source.path
    is DataEntity.ZipFileEntity -> source.parent.path
    is DataEntity.ZipInputStreamEntity -> source.parent.sourcePath()
}