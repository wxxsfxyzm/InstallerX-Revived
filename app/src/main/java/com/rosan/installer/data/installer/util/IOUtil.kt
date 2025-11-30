package com.rosan.installer.data.installer.util

import android.os.SystemClock
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

// Buffer optimized for modern Android filesystem block sizes
private const val COPY_BUFFER_SIZE = 1 * 1024 * 1024 // 1MB

// Balance between sendfile() syscall efficiency and UI responsiveness
private const val CHUNK_SIZE = 16 * 1024 * 1024L // 16MB


/**
 * Copies data from InputStream to OutputStream with progress reporting.
 *
 * @param output Target output stream
 * @param totalSize Total expected bytes (-1 if unknown)
 * @param progressFlow Flow to emit progress updates
 */
suspend fun InputStream.copyToWithProgress(
    output: OutputStream,
    totalSize: Long,
    progressFlow: MutableSharedFlow<ProgressEntity>
) {
    var bytesCopied = 0L
    val buf = ByteArray(COPY_BUFFER_SIZE)
    val step = if (totalSize > 0) (totalSize * 0.01f).toLong().coerceAtLeast(128 * 1024) else Long.MAX_VALUE
    var nextEmit = step
    var lastEmitTime = 0L

    if (totalSize > 0) progressFlow.emit(ProgressEntity.InstallPreparing(0f))

    var read = this.read(buf)
    while (read >= 0) {
        if (!currentCoroutineContext().isActive) throw CancellationException()
        output.write(buf, 0, read)
        bytesCopied += read

        val now = SystemClock.uptimeMillis()
        if (totalSize > 0 && bytesCopied >= nextEmit && now - lastEmitTime >= 200) {
            progressFlow.tryEmit(ProgressEntity.InstallPreparing(bytesCopied.toFloat() / totalSize))
            lastEmitTime = now
            nextEmit = (bytesCopied / step + 1) * step
        }
        read = this.read(buf)
    }
    if (totalSize > 0) progressFlow.emit(ProgressEntity.InstallPreparing(1f))
}

/**
 * Transfers data using NIO FileChannel (Zero-Copy / Kernel-Space copy).
 * Significantly faster for local file operations.
 */
suspend fun transferWithProgress(
    sourceFd: FileDescriptor,
    sourceOffset: Long,
    destFile: File,
    totalSize: Long,
    progressFlow: MutableSharedFlow<ProgressEntity>
) {
    val sourceChannel = FileInputStream(sourceFd).channel
    val destChannel = FileOutputStream(destFile).channel

    sourceChannel.use { src ->
        destChannel.use { dst ->
            var position = 0L
            // Use 16MB chunks. This balances the benefits of the `sendfile` syscall
            // with the need to update UI progress frequently.
            val chunkSize = CHUNK_SIZE
            var nextEmit = chunkSize / 2
            var lastEmitTime = 0L

            if (totalSize > 0) progressFlow.emit(ProgressEntity.InstallPreparing(0f))

            while (position < totalSize) {
                if (!currentCoroutineContext().isActive) throw CancellationException()

                // Calculate remaining bytes to avoid over-reading (especially for assets with offsets)
                val count = (totalSize - position).coerceAtMost(chunkSize)

                // Perform the transfer.
                // Note: We must add sourceOffset to handle assets embedded in other files (e.g., APKs).
                val transferred = src.transferTo(sourceOffset + position, count, dst)

                if (transferred == 0L) break

                position += transferred

                // Progress reporting logic
                val now = SystemClock.uptimeMillis()
                if (position >= nextEmit && now - lastEmitTime >= 200) {
                    progressFlow.tryEmit(ProgressEntity.InstallPreparing(position.toFloat() / totalSize))
                    lastEmitTime = now
                    nextEmit = position + (chunkSize / 2)
                }
            }
            if (totalSize > 0) progressFlow.emit(ProgressEntity.InstallPreparing(1f))
        }
    }
}