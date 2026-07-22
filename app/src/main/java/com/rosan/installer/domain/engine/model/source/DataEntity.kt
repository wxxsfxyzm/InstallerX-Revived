package com.rosan.installer.domain.engine.model.source

import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.Closeable
import java.io.File
import java.io.FileDescriptor
import java.io.FilterInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.channels.SeekableByteChannel
import java.nio.file.StandardOpenOption
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.CRC32
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import kotlin.math.min

sealed class DataEntity(open var source: DataEntity? = null) {
    abstract fun getInputStream(): InputStream?

    abstract fun getSize(): Long

    fun getInputStreamWhileNotEmpty(): InputStream? = getInputStream() ?: source?.getInputStream()

    fun getSourceTop(): DataEntity = source?.getSourceTop() ?: this

    open class FileEntity(val path: String) : DataEntity() {
        open fun openChannel(): SeekableByteChannel =
            FileChannel.open(File(path).toPath(), StandardOpenOption.READ)

        override fun getInputStream(): InputStream = File(path).inputStream()

        override fun getSize(): Long = File(path).length()

        override fun toString() = path
    }

    class FileDescriptorEntity(
        path: String,
        val startOffset: Long,
        val length: Long,
        private val channelFactory: () -> SeekableByteChannel,
        private val descriptorFactory: () -> OwnedFileDescriptor
    ) : FileEntity(path) {
        init {
            require(startOffset >= 0L) { "startOffset must be non-negative" }
            require(length > 0L) { "length must be positive" }
        }

        override fun openChannel(): SeekableByteChannel = channelFactory()

        override fun getInputStream(): InputStream = Channels.newInputStream(openChannel())

        override fun getSize(): Long = length

        fun <R> withFileDescriptor(block: (FileDescriptor) -> R): R =
            descriptorFactory().use { descriptor -> block(descriptor.fileDescriptor) }

        /**
         * Creates a view over a byte range of this descriptor without opening the source path.
         * The descriptor factory is intentionally shared so platform consumers can receive the
         * original fd together with the adjusted absolute range.
         */
        fun subrange(relativeOffset: Long, subrangeLength: Long): FileDescriptorEntity {
            require(relativeOffset >= 0L) { "relativeOffset must be non-negative" }
            require(subrangeLength > 0L) { "subrangeLength must be positive" }
            require(relativeOffset <= length && subrangeLength <= length - relativeOffset) {
                "Requested subrange exceeds descriptor bounds"
            }
            require(relativeOffset <= Long.MAX_VALUE - startOffset) {
                "Descriptor offset overflow"
            }

            val parent = this
            return FileDescriptorEntity(
                path = path,
                startOffset = startOffset + relativeOffset,
                length = subrangeLength,
                channelFactory = {
                    BoundedSeekableByteChannel(
                        delegate = parent.openChannel(),
                        startOffset = relativeOffset,
                        rangeLength = subrangeLength
                    )
                },
                descriptorFactory = descriptorFactory
            ).apply {
                source = parent.source
            }
        }
    }

    class OwnedFileDescriptor(
        val fileDescriptor: FileDescriptor,
        private val closeAction: () -> Unit
    ) : Closeable {
        private val closed = AtomicBoolean(false)

        override fun close() {
            if (closed.compareAndSet(false, true)) closeAction()
        }
    }

    class ZipFileEntity(val name: String, val parent: FileEntity) : DataEntity() {
        override fun getInputStream(): InputStream? {
            val zipFile = openCommonsZipFile(parent)
            val entry = zipFile.getEntry(name)
            if (entry == null) {
                zipFile.close()
                return null
            }
            return try {
                requireSupportedZipCompressionMethod(entry.method, entry.name)
                ZipFileClosingInputStream(zipFile.getInputStream(entry), zipFile)
            } catch (error: Exception) {
                zipFile.close()
                throw error
            }
        }

        private val cachedSize: Long by lazy {
            try {
                // Open the parent file (e.g., .xapk or .apks archive)
                openCommonsZipFile(parent).use { zip ->
                    // Get the entry with the specified name (e.g., split_config.arm64_v8a.apk)
                    val entry = zip.getEntry(name)
                    // entry.size returns -1 when unknown, so handle that case
                    val size = entry?.size ?: 0L

                    // If the uncompressed size is unknown (rare), fall back to compressed size or return 0
                    if (size == -1L) entry?.compressedSize ?: 0L else size
                }
            } catch (e: Exception) {
                0L
            }
        }

        override fun getSize(): Long = cachedSize

        override var source: DataEntity? = parent.source?.let { ZipInputStreamEntity(name, it) }

        override fun toString() = "$parent!$name"
    }

    class SeekableZipEntryEntity(
        val name: String,
        val parent: FileEntity,
        val dataOffset: Long,
        val compressedSize: Long,
        private val uncompressedSize: Long,
        val compressionMethod: Int,
        val crc: Long
    ) : DataEntity() {
        init {
            require(dataOffset >= 0) { "dataOffset must be non-negative" }
            require(compressedSize >= 0) { "compressedSize must be non-negative" }
            require(uncompressedSize >= 0) { "uncompressedSize must be non-negative" }
            requireSupportedZipCompressionMethod(compressionMethod, name)
            require(compressionMethod != ZipEntry.STORED || compressedSize == uncompressedSize) {
                "Stored ZIP entry must have matching compressed and uncompressed sizes"
            }
        }

        override fun getInputStream(): InputStream {
            val slice = FileSliceInputStream(parent, dataOffset, compressedSize)
            val decoded = when (compressionMethod) {
                ZipEntry.STORED -> slice
                ZipEntry.DEFLATED -> RawDeflateInputStream(slice)
                else -> error("Unsupported ZIP compression method: $compressionMethod")
            }
            return CrcVerifyingInputStream(decoded, uncompressedSize, crc, name)
        }

        override fun getSize(): Long = uncompressedSize

        override var source: DataEntity? = parent.source?.let { ZipInputStreamEntity(name, it) }

        override fun toString(): String = "$parent!$name"
    }

    class ZipInputStreamEntity(val name: String, val parent: DataEntity) : DataEntity() {
        override fun getInputStream(): InputStream? {
            val inputStream = parent.getInputStream() ?: return null
            val zip = ZipInputStream(inputStream)
            var result: InputStream? = null
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name != name) continue
                try {
                    requireSupportedZipCompressionMethod(entry.method, entry.name)
                } catch (error: Exception) {
                    zip.close()
                    throw error
                }
                result = zip
                break
            }
            return result
        }

        override fun getSize(): Long = -1L

        override var source: DataEntity? = parent.source?.let { ZipInputStreamEntity(name, it) }

        override fun toString(): String = "$parent!$name"
    }

    class StreamDataEntity(
        private val stream: InputStream,
        private val length: Long
    ) : DataEntity() {
        override fun getInputStream(): InputStream = stream

        // Return the Content-Length from the network
        override fun getSize(): Long = length

        override fun toString(): String = "NetworkStream(size=$length)"
    }
}

private fun openCommonsZipFile(source: DataEntity.FileEntity): ZipFile {
    val channel = source.openChannel()
    return try {
        ZipFile.builder()
            .setSeekableByteChannel(channel)
            .setIgnoreLocalFileHeader(true)
            .get()
    } catch (error: Exception) {
        try {
            channel.close()
        } catch (closeError: Exception) {
            error.addSuppressed(closeError)
        }
        throw error
    }
}

private class ZipFileClosingInputStream(
    input: InputStream,
    private val zipFile: ZipFile
) : FilterInputStream(input) {
    override fun close() {
        try {
            super.close()
        } finally {
            zipFile.close()
        }
    }
}

private class FileSliceInputStream(
    source: DataEntity.FileEntity,
    offset: Long,
    length: Long
) : InputStream() {
    private val channel = source.openChannel()
    private val singleByte = ByteBuffer.allocate(1)
    private var remaining = length

    init {
        try {
            val fileSize = channel.size()
            if (offset < 0 || length < 0 || offset > fileSize || length > fileSize - offset) {
                throw ZipException(
                    "ZIP entry slice exceeds parent file: offset=$offset, length=$length, size=$fileSize"
                )
            }
            channel.position(offset)
        } catch (e: Exception) {
            channel.close()
            throw e
        }
    }

    override fun read(): Int {
        if (remaining == 0L) return -1
        singleByte.clear()
        while (true) {
            when (channel.read(singleByte)) {
                -1 -> return -1
                0 -> continue
                else -> {
                    remaining--
                    return singleByte.array()[0].toInt() and 0xFF
                }
            }
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (offset < 0 || length < 0 || length > buffer.size - offset) throw IndexOutOfBoundsException()
        if (length == 0) return 0
        if (remaining == 0L) return -1

        val allowed = min(length.toLong(), remaining).toInt()
        val count = channel.read(ByteBuffer.wrap(buffer, offset, allowed))
        if (count > 0) remaining -= count
        return count
    }

    override fun skip(count: Long): Long {
        if (count <= 0 || remaining == 0L) return 0L
        val skipped = min(count, remaining)
        channel.position(channel.position() + skipped)
        remaining -= skipped
        return skipped
    }

    override fun available(): Int = min(remaining, Int.MAX_VALUE.toLong()).toInt()

    override fun close() = channel.close()
}

private class BoundedSeekableByteChannel(
    private val delegate: SeekableByteChannel,
    private val startOffset: Long,
    private val rangeLength: Long
) : SeekableByteChannel {
    private var position = 0L
    private var closed = false

    override fun read(destination: ByteBuffer): Int {
        ensureOpen()
        if (!destination.hasRemaining()) return 0
        if (position >= rangeLength) return -1

        val bytesToRead = min(destination.remaining().toLong(), rangeLength - position).toInt()
        val originalLimit = destination.limit()
        return try {
            destination.limit(destination.position() + bytesToRead)
            delegate.position(startOffset + position)
            delegate.read(destination).also { count ->
                if (count > 0) position += count
            }
        } finally {
            destination.limit(originalLimit)
        }
    }

    override fun write(source: ByteBuffer): Int = throw java.nio.channels.NonWritableChannelException()

    override fun position(): Long {
        ensureOpen()
        return position
    }

    override fun position(newPosition: Long): SeekableByteChannel {
        ensureOpen()
        require(newPosition >= 0L) { "position must be non-negative" }
        position = newPosition
        return this
    }

    override fun size(): Long {
        ensureOpen()
        return rangeLength
    }

    override fun truncate(size: Long): SeekableByteChannel =
        throw java.nio.channels.NonWritableChannelException()

    override fun isOpen(): Boolean = !closed && delegate.isOpen

    override fun close() {
        if (!closed) {
            closed = true
            delegate.close()
        }
    }

    private fun ensureOpen() {
        if (!isOpen) throw java.nio.channels.ClosedChannelException()
    }
}

internal class RawDeflateInputStream(
    input: InputStream,
    private val ownedInflater: Inflater = Inflater(true)
) : InflaterInputStream(input, ownedInflater) {
    private var resourcesReleased = false

    override fun close() {
        if (resourcesReleased) return
        resourcesReleased = true
        try {
            super.close()
        } finally {
            ownedInflater.end()
        }
    }
}

private class CrcVerifyingInputStream(
    input: InputStream,
    private val expectedSize: Long,
    private val expectedCrc: Long,
    private val entryName: String
) : FilterInputStream(input) {
    private val crc = CRC32()
    private var bytesRead = 0L
    private var verified = false

    override fun read(): Int {
        val value = super.read()
        if (value < 0) {
            verify()
        } else {
            crc.update(value)
            bytesRead++
        }
        return value
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val count = super.read(buffer, offset, length)
        if (count < 0) {
            verify()
        } else if (count > 0) {
            crc.update(buffer, offset, count)
            bytesRead += count
        }
        return count
    }

    override fun skip(count: Long): Long {
        if (count <= 0) return 0L
        val buffer = ByteArray(min(count, SKIP_BUFFER_SIZE.toLong()).toInt())
        var remaining = count
        while (remaining > 0) {
            val read = read(buffer, 0, min(remaining, buffer.size.toLong()).toInt())
            if (read < 0) break
            remaining -= read
        }
        return count - remaining
    }

    private fun verify() {
        if (verified) return
        if (bytesRead != expectedSize) {
            throw ZipException(
                "ZIP entry size mismatch for $entryName: expected=$expectedSize, actual=$bytesRead"
            )
        }
        if (crc.value != expectedCrc) {
            throw ZipException(
                "ZIP entry CRC mismatch for $entryName: expected=$expectedCrc, actual=${crc.value}"
            )
        }
        verified = true
    }

    private companion object {
        const val SKIP_BUFFER_SIZE = 8 * 1024
    }
}
