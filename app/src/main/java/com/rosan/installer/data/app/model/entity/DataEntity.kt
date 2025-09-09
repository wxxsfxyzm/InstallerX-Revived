package com.rosan.installer.data.app.model.entity

import android.annotation.SuppressLint
import android.system.Os
import android.system.OsConstants
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.InputStream
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

sealed class DataEntity(open var source: DataEntity? = null) {
    abstract fun getInputStream(): InputStream?

    fun getInputStreamWhileNotEmpty(): InputStream? = getInputStream() ?: source?.getInputStream()

    fun getSourceTop(): DataEntity = source?.getSourceTop() ?: this

    class FileEntity(val path: String) : DataEntity() {
        override fun getInputStream() = File(path).inputStream()

        override fun toString() = path
    }

    class ZipFileEntity(val name: String, val parent: FileEntity) : DataEntity() {
        override fun getInputStream(): InputStream? = ZipFile(parent.path).let {
            val entry = it.getEntry(name) ?: return@let null
            it.getInputStream(entry)
        }

        override var source: DataEntity? = parent.source?.let { ZipInputStreamEntity(name, it) }

        override fun toString() = "$parent!$name"
    }

    class FileDescriptorEntity(private val pid: Int, private val descriptor: Int) : DataEntity() {
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
    }

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

        override var source: DataEntity? = parent.source?.let { ZipInputStreamEntity(name, it) }

        override fun toString(): String = "$parent!$name"
    }
}