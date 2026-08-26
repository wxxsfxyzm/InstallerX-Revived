// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.session.resolver

import com.rosan.installer.domain.engine.exception.AnalyseException
import com.rosan.installer.domain.engine.model.error.AnalyseErrorType
import com.rosan.installer.domain.engine.model.source.AnalysisMaterializationPolicy
import com.rosan.installer.domain.engine.model.source.DataEntity
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.CancellationException

internal suspend fun materializeAnalysisSource(
    data: List<DataEntity>,
    requestedSource: DataEntity.FileDescriptorEntity,
    cacheDirectory: File,
    copySource: suspend (InputStream, OutputStream, Long) -> Unit,
): List<DataEntity> {
    if (
        requestedSource.analysisMaterializationPolicy !=
        AnalysisMaterializationPolicy.RETAINED_SOURCE_REPLACEMENT
    ) {
        throw materializationFailure(
            "Analysis requested materialization for a source that does not allow it",
        )
    }

    val key = requestedSource.analysisMaterializationKey
        ?: throw materializationFailure("Analysis materialization source has no identity key")
    val sourceIndex = data.indexOfFirst { entity ->
        entity is DataEntity.FileDescriptorEntity &&
            entity.analysisMaterializationKey === key
    }
    val source = data.getOrNull(sourceIndex) as? DataEntity.FileDescriptorEntity
        ?: throw materializationFailure(
            "The requested analysis source is no longer part of the session",
        )

    cacheDirectory.mkdirs()
    val retainedFile = File.createTempFile("materialized-", ".apk", cacheDirectory)
    try {
        source.getInstallInputStream().use { input ->
            retainedFile.outputStream().use { output ->
                copySource(input, output, source.length)
            }
        }
        if (retainedFile.length() != source.length) {
            throw IOException(
                "Materialized source length mismatch: " +
                    "expected=${source.length}, actual=${retainedFile.length()}",
            )
        }

        val replacement = DataEntity.FileEntity(retainedFile.absolutePath).apply {
            // Preserve the original URI identity while the retained file becomes the only
            // analysis and install input for the remainder of the session.
            this.source = source.source ?: DataEntity.FileEntity(source.path)
        }
        return data.toMutableList().apply { this[sourceIndex] = replacement }
    } catch (error: CancellationException) {
        retainedFile.delete()
        throw error
    } catch (error: Exception) {
        retainedFile.delete()
        throw materializationFailure(
            message = "Failed to retain the complete source for analysis",
            cause = error,
        )
    }
}

private fun materializationFailure(message: String, cause: Throwable? = null) = AnalyseException(
    errorType = AnalyseErrorType.SOURCE_MATERIALIZATION_FAILED,
    message = message,
    cause = cause,
)
