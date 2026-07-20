// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.parser

import com.rosan.installer.domain.engine.model.AnalyseExtraEntity
import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.engine.model.source.DataType
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class UnifiedZipFileProviderTest {
    private lateinit var tempDirectory: File
    private lateinit var provider: UnifiedZipFileProvider

    @BeforeTest
    fun setUp() {
        tempDirectory = Files.createTempDirectory("unified-zip-test").toFile()
        provider = UnifiedZipFileProvider(CommonsZipFileProvider(), SeekableZipReader())
    }

    @AfterTest
    fun tearDown() {
        tempDirectory.deleteRecursively()
    }

    @Test
    fun `uses local headers when a nested central directory hides outer entries`() {
        val basePayload = validInnerApk("base")
        val splitPayload = validInnerApk("split")
        val file = writeLocalOnlyArchive(
            "broken.apks",
            listOf(
                TestEntry("base.apk", basePayload),
                TestEntry("split_config.en.apk", splitPayload)
            )
        )

        provider.open(file).use { archive ->
            assertEquals(UnifiedZipBackend.LOCAL_FILE_HEADERS, archive.backend)
            assertEquals(listOf("base.apk", "split_config.en.apk"), archive.entries.map { it.name })

            val baseEntry = requireNotNull(archive.getEntry("base.apk"))
            assertContentEquals(basePayload, archive.openEntry(baseEntry).use { it.readBytes() })
            assertIs<DataEntity.SeekableZipEntryEntity>(
                archive.toDataEntity(baseEntry, DataEntity.FileEntity(file.path))
            )
        }
    }

    @Test
    fun `keeps the central backend when metadata views agree`() {
        val payload = "manifest".toByteArray()
        val file = File(tempDirectory, "normal.apk")
        ZipOutputStream(file.outputStream()).use { output ->
            output.writeStoredEntry("AndroidManifest.xml", payload)
        }

        provider.open(file).use { archive ->
            assertEquals(UnifiedZipBackend.COMMONS_CENTRAL_DIRECTORY, archive.backend)
            val entry = requireNotNull(archive.getEntry("AndroidManifest.xml"))
            assertContentEquals(payload, archive.openEntry(entry).use { it.readBytes() })
        }
    }

    @Test
    fun `can disable local fallback for module archives`() {
        val file = writeLocalOnlyArchive(
            "module-view.zip",
            listOf(TestEntry("base.apk", validInnerApk("base")))
        )

        provider.open(file, allowLocalHeaderFallback = false).use { archive ->
            assertEquals(UnifiedZipBackend.COMMONS_CENTRAL_DIRECTORY, archive.backend)
            assertNull(archive.getEntry("base.apk"))
            assertEquals(listOf("AndroidManifest.xml"), archive.entries.map { it.name })
        }
    }

    @Test
    fun `detects every non-module Android package type through the same fallback`() {
        val detector = FileTypeDetector(Json.Default, provider)
        val cases = listOf(
            DetectionCase(
                fileName = "single.apk",
                entries = listOf(TestEntry("AndroidManifest.xml", "manifest".toByteArray())),
                expectedType = DataType.APK
            ),
            DetectionCase(
                fileName = "splits.apks",
                entries = listOf(
                    TestEntry("base.apk", "base".toByteArray()),
                    TestEntry("split_config.en.apk", "split".toByteArray())
                ),
                expectedType = DataType.APKS
            ),
            DetectionCase(
                fileName = "bundle.apkm",
                entries = listOf(
                    TestEntry("info.json", "{\"pname\":\"pkg\",\"versioncode\":\"1\"}".toByteArray()),
                    TestEntry("base.apk", "base".toByteArray())
                ),
                expectedType = DataType.APKM
            ),
            DetectionCase(
                fileName = "bundle.xapk",
                entries = listOf(
                    TestEntry(
                        "manifest.json",
                        "{\"package_name\":\"pkg\",\"version_code\":1,\"split_apks\":[]}".toByteArray()
                    ),
                    TestEntry("base.apk", "base".toByteArray())
                ),
                expectedType = DataType.XAPK
            ),
            DetectionCase(
                fileName = "multiple.zip",
                entries = listOf(
                    TestEntry("first.apk", "first".toByteArray()),
                    TestEntry("second.apk", "second".toByteArray())
                ),
                expectedType = DataType.MULTI_APK_ZIP
            )
        )

        cases.forEach { case ->
            val file = writeLocalOnlyArchive(case.fileName, case.entries)
            val result = detector.detect(
                DataEntity.FileEntity(file.path),
                AnalyseExtraEntity(
                    cacheDirectory = tempDirectory.path,
                    isModuleFlashEnabled = false,
                    checkAppSignature = false
                )
            )

            assertEquals(case.expectedType, result, case.fileName)
        }
    }

    @Test
    fun `module detection remains central-directory only`() {
        val detector = FileTypeDetector(Json.Default, provider)
        val extra = AnalyseExtraEntity(
            cacheDirectory = tempDirectory.path,
            isModuleFlashEnabled = true,
            checkAppSignature = false
        )
        val moduleProperties = "id=test\nname=Test".toByteArray()
        val normalModule = File(tempDirectory, "normal-module.zip")
        ZipOutputStream(normalModule.outputStream()).use { output ->
            output.writeStoredEntry("module.prop", moduleProperties)
        }
        val localOnlyModule = writeLocalOnlyArchive(
            "local-only-module.zip",
            listOf(TestEntry("module.prop", moduleProperties))
        )

        assertEquals(
            DataType.MODULE_ZIP,
            detector.detect(DataEntity.FileEntity(normalModule.path), extra)
        )
        assertEquals(
            DataType.NONE,
            detector.detect(DataEntity.FileEntity(localOnlyModule.path), extra)
        )
    }

    private fun validInnerApk(marker: String): ByteArray = ByteArrayOutputStream().also { bytes ->
        ZipOutputStream(bytes).use { output ->
            output.putNextEntry(ZipEntry("AndroidManifest.xml"))
            output.write(marker.toByteArray())
            output.closeEntry()
        }
    }.toByteArray()

    private fun writeLocalOnlyArchive(fileName: String, entries: List<TestEntry>): File {
        val output = ByteArrayOutputStream()
        entries.forEach { entry -> output.writeStoredLocalEntry(entry.name, entry.payload) }
        return File(tempDirectory, fileName).apply { writeBytes(output.toByteArray()) }
    }

    private fun ByteArrayOutputStream.writeStoredLocalEntry(name: String, payload: ByteArray) {
        val nameBytes = name.toByteArray(StandardCharsets.UTF_8)
        val crc = CRC32().apply { update(payload) }.value
        writeIntLittleEndian(LOCAL_FILE_HEADER_SIGNATURE)
        writeShortLittleEndian(10)
        writeShortLittleEndian(UTF8_FLAG)
        writeShortLittleEndian(ZipEntry.STORED)
        writeShortLittleEndian(0)
        writeShortLittleEndian(0)
        writeIntLittleEndian(crc)
        writeIntLittleEndian(payload.size.toLong())
        writeIntLittleEndian(payload.size.toLong())
        writeShortLittleEndian(nameBytes.size)
        writeShortLittleEndian(0)
        write(nameBytes)
        write(payload)
    }

    private fun ZipOutputStream.writeStoredEntry(name: String, payload: ByteArray) {
        val crc = CRC32().apply { update(payload) }.value
        putNextEntry(
            ZipEntry(name).apply {
                method = ZipEntry.STORED
                size = payload.size.toLong()
                compressedSize = payload.size.toLong()
                this.crc = crc
            }
        )
        write(payload)
        closeEntry()
    }

    private fun ByteArrayOutputStream.writeShortLittleEndian(value: Int) {
        repeat(Short.SIZE_BYTES) { index -> write(value ushr (index * Byte.SIZE_BITS)) }
    }

    private fun ByteArrayOutputStream.writeIntLittleEndian(value: Long) {
        repeat(Int.SIZE_BYTES) { index -> write((value ushr (index * Byte.SIZE_BITS)).toInt()) }
    }

    private data class TestEntry(val name: String, val payload: ByteArray)

    private data class DetectionCase(
        val fileName: String,
        val entries: List<TestEntry>,
        val expectedType: DataType
    )

    private companion object {
        const val LOCAL_FILE_HEADER_SIGNATURE = 0x04034B50L
        const val UTF8_FLAG = 1 shl 11
    }
}
