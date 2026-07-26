// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.parser

import com.rosan.installer.domain.engine.model.source.DataEntity
import timber.log.Timber
import java.io.File
import java.io.IOException

/**
 * Produces a real local module file when analysis is backed by a retained content descriptor.
 *
 * Root module implementations accept a filesystem path and cannot consume a content URI or fd.
 * Ordinary APK installation remains descriptor-backed and does not use this materialization path.
 */
internal class ModuleSourceMaterializer {
    fun materializeForInstall(data: DataEntity): DataEntity.FileEntity = when (data) {
        is DataEntity.DeferredFileMaterializationEntity ->
            materializeForInstall(data.file, data.cacheDirectory)

        is DataEntity.FileEntity -> data
        else -> throw IOException("Module source is not file-backed: $data")
    }

    fun materializeForInstall(
        data: DataEntity.FileEntity,
        cacheDirectory: String
    ): DataEntity.FileEntity {
        if (data !is DataEntity.FileDescriptorEntity) return data

        val directory = File(cacheDirectory)
        if ((!directory.exists() && !directory.mkdirs()) || !directory.isDirectory) {
            throw IOException("Unable to create module cache directory: $cacheDirectory")
        }

        val localFile = File.createTempFile("module-", ".zip", directory)
        return try {
            val copiedSize = data.getInputStream().use { input ->
                localFile.outputStream().use(input::copyTo)
            }
            if (copiedSize != data.getSize()) {
                throw IOException(
                    "Incomplete module materialization: expected=${data.getSize()}, actual=$copiedSize"
                )
            }

            Timber.d(
                "Materialized descriptor-backed module for path installation: " +
                        "source=${data.getSourceTop()}, path=${localFile.absolutePath}, size=$copiedSize"
            )
            DataEntity.FileEntity(localFile.absolutePath).apply {
                // Preserve source identity for grouping and cleanup. Module installation deliberately
                // uses this entity's current path instead of resolving the top source.
                source = data.getSourceTop()
            }
        } catch (error: Exception) {
            localFile.delete()
            throw error
        }
    }
}
