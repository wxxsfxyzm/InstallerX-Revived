// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.executor.appinstaller

import com.rosan.installer.domain.engine.model.install.InstallWriteProgress
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Copies selected APK payloads while reporting aggregate bytes written.
 *
 * Progress callbacks run inline with the caller's suspend operation. This keeps cancellation and
 * delivery owned by the installation flow instead of introducing a separate lossy event stream.
 */
internal class InstallProgressWriter(
    private val totalBytes: Long,
    private val onProgress: suspend (InstallWriteProgress) -> Unit,
    private val nanoTime: () -> Long = System::nanoTime
) {
    private val reportStep = max(totalBytes / 100L, MIN_REPORT_STEP)
    private var completedBytes = 0L
    private var lastReportedBytes = -1L
    private var lastReportTime = 0L

    init {
        require(totalBytes > 0L) { "totalBytes must be positive" }
    }

    suspend fun start() {
        report(bytesWritten = 0L, force = true)
    }

    suspend fun copy(
        input: InputStream,
        output: OutputStream,
        expectedBytes: Long,
        onEntryProgress: (bytesWritten: Long) -> Unit = {}
    ) {
        require(expectedBytes > 0L) { "expectedBytes must be positive" }
        require(expectedBytes <= totalBytes - completedBytes) {
            "Entry size exceeds remaining installation bytes"
        }

        val buffer = ByteArray(COPY_BUFFER_SIZE)
        var entryBytes = 0L

        while (true) {
            currentCoroutineContext().ensureActive()
            val remainingBytes = expectedBytes - entryBytes
            val maxRead = if (remainingBytes == 0L) {
                1
            } else {
                min(buffer.size.toLong(), remainingBytes).toInt()
            }
            val count = input.read(buffer, 0, maxRead)
            if (count < 0) break
            if (count == 0) continue

            entryBytes += count
            if (entryBytes > expectedBytes) {
                throw EOFException(
                    "Install source exceeds expected size: expected=$expectedBytes, actual>$entryBytes"
                )
            }

            output.write(buffer, 0, count)
            val aggregateBytes = completedBytes + entryBytes
            if (shouldReport(aggregateBytes)) {
                onEntryProgress(entryBytes)
                report(aggregateBytes, force = false)
            }
        }

        if (entryBytes != expectedBytes) {
            throw EOFException(
                "Incomplete install source: expected=$expectedBytes, actual=$entryBytes"
            )
        }

        completedBytes += entryBytes
        onEntryProgress(entryBytes)
        report(completedBytes, force = true)
    }

    private fun shouldReport(bytesWritten: Long): Boolean {
        if (bytesWritten - lastReportedBytes < reportStep) return false
        return nanoTime() - lastReportTime >= MIN_REPORT_INTERVAL_NANOS
    }

    private suspend fun report(bytesWritten: Long, force: Boolean) {
        if (!force && bytesWritten == lastReportedBytes) return
        lastReportedBytes = bytesWritten
        lastReportTime = nanoTime()
        onProgress(InstallWriteProgress(bytesWritten, totalBytes))
    }

    companion object {
        internal const val COPY_BUFFER_SIZE = 1024 * 1024
        private const val MIN_REPORT_STEP = 128 * 1024L
        private const val MIN_REPORT_INTERVAL_NANOS = 200_000_000L

        fun totalBytes(sizes: Iterable<Long>): Long = sizes.fold(0L) { total, size ->
            require(size > 0L) { "Install entity size must be positive: $size" }
            Math.addExact(total, size)
        }

        fun totalBytesOrNull(sizes: Iterable<Long>): Long? {
            var total = 0L
            for (size in sizes) {
                if (size <= 0L) return null
                total = try {
                    Math.addExact(total, size)
                } catch (_: ArithmeticException) {
                    return null
                }
            }
            return total.takeIf { it > 0L }
        }
    }
}

internal suspend fun copyInstallSource(input: InputStream, output: OutputStream) {
    val buffer = ByteArray(InstallProgressWriter.COPY_BUFFER_SIZE)
    while (true) {
        currentCoroutineContext().ensureActive()
        val count = input.read(buffer)
        if (count < 0) return
        if (count > 0) output.write(buffer, 0, count)
    }
}
