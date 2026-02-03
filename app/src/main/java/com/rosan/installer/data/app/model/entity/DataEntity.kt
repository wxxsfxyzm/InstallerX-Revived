package com.rosan.installer.data.app.model.entity

import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

sealed class DataEntity(open var source: DataEntity? = null) {
    abstract fun getInputStream(): InputStream?

    abstract fun getSize(): Long

    fun getInputStreamWhileNotEmpty(): InputStream? = getInputStream() ?: source?.getInputStream()

    fun getSourceTop(): DataEntity = source?.getSourceTop() ?: this

    class FileEntity(val path: String) : DataEntity() {
        override fun getInputStream() = File(path).inputStream()

        override fun getSize(): Long = File(path).length()

        override fun toString() = path
    }

    class ZipFileEntity(val name: String, val parent: FileEntity) : DataEntity() {
        override fun getInputStream(): InputStream? = ZipFile(parent.path).let {
            val entry = it.getEntry(name) ?: return@let null
            it.getInputStream(entry)
        }

        private val cachedSize: Long by lazy {
            try {
                // Open the parent file (e.g., .xapk or .apks archive)
                ZipFile(parent.path).use { zip ->
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

    /*    class FileDescriptorEntity(private val pid: Int, private val descriptor: Int) : DataEntity() {
            @SuppressLint("DiscouragedPrivateApi")
            fun getFileDescriptor(): FileDescriptor? {
                if (Os.getpid() != pid) return null
                val fileDescriptor = FileDescriptor()
                kotlin.runCatching { FileDescriptor::class.java.getDeclaredField("descriptor") }
                    .onSuccess {
                        it.isAccessible = true
                        it.set(fileDescriptor, descriptor)
                    }.onFailure {
                        it.printStackTrace()
                    }
                if (!fileDescriptor.valid()) return null
                Os.lseek(fileDescriptor, 0, OsConstants.SEEK_SET)
                return fileDescriptor
            }

            override fun getInputStream(): InputStream {
                val fileDescriptor = getFileDescriptor()
                if (fileDescriptor != null) {
                    return FileInputStream(fileDescriptor)
                }
                return File("/proc/$pid/fd/$descriptor").inputStream()
            }

            override fun toString() = "/proc/$pid/fd/$descriptor"
        }*/

    class ZipInputStreamEntity(val name: String, val parent: DataEntity) : DataEntity() {
        override fun getInputStream(): InputStream? {
            val inputStream = parent.getInputStream() ?: return null
            val zip = ZipInputStream(inputStream)
            var result: InputStream? = null
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name != name) continue
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