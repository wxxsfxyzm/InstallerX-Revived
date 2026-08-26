// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.session.resolver

import com.rosan.installer.domain.engine.exception.AnalyseException
import com.rosan.installer.domain.engine.model.error.AnalyseErrorType
import com.rosan.installer.domain.engine.model.source.AnalysisMaterializationKey
import com.rosan.installer.domain.engine.model.source.AnalysisMaterializationPolicy
import com.rosan.installer.domain.engine.model.source.DataEntity
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.io.path.deleteIfExists
import kotlin.io.path.readBytes
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

class AnalysisSourceMaterializerTest {
    @Test
    fun `materialization replaces top-level source and uses install stream`() = runTest {
        val analysisBytes = byteArrayOf(1, 2, 3, 4)
        val installBytes = byteArrayOf(5, 6, 7, 8)
        val analysisFile = Files.createTempFile("installerx-analysis", ".apk")
        val cacheDirectory = Files.createTempDirectory("installerx-materialized")
        Files.write(analysisFile, analysisBytes)

        try {
            val key = AnalysisMaterializationKey()
            val remote = descriptorEntity(
                analysisFile = analysisFile.toFile(),
                installBytes = installBytes,
                policy = AnalysisMaterializationPolicy.RETAINED_SOURCE_REPLACEMENT,
                key = key,
            ).apply { source = DataEntity.FileEntity("https://example.com/app.apk") }
            val requestedSubrange = remote.subrange(0L, 2L)
            val unrelated = DataEntity.FileEntity("unrelated.apk")

            val result = materializeAnalysisSource(
                data = listOf(unrelated, remote),
                requestedSource = requestedSubrange,
                cacheDirectory = cacheDirectory.toFile(),
            ) { input, output, _ -> input.copyTo(output) }

            assertEquals(unrelated, result[0])
            val retained = assertIs<DataEntity.FileEntity>(result[1])
            assertContentEquals(installBytes, retained.getInputStream().use { it.readBytes() })
            assertEquals("https://example.com/app.apk", retained.getSourceTop().toString())
            assertContentEquals(
                installBytes,
                requireNotNull(retained.getInstallInputStream()).use { it.readBytes() },
            )
        } finally {
            analysisFile.deleteIfExists()
            cacheDirectory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `disallowed source is never materialized`() = runTest {
        val sourceFile = Files.createTempFile("installerx-low-storage", ".apk")
        val cacheDirectory = Files.createTempDirectory("installerx-materialized")
        Files.write(sourceFile, byteArrayOf(1))

        try {
            val source = descriptorEntity(
                analysisFile = sourceFile.toFile(),
                installBytes = byteArrayOf(1),
                policy = AnalysisMaterializationPolicy.DISALLOW,
                key = null,
            )

            val error = assertFailsWith<AnalyseException> {
                materializeAnalysisSource(
                    data = listOf(source),
                    requestedSource = source,
                    cacheDirectory = cacheDirectory.toFile(),
                ) { input, output, _ -> input.copyTo(output) }
            }

            assertEquals(AnalyseErrorType.SOURCE_MATERIALIZATION_FAILED, error.errorType)
            assertEquals(0, cacheDirectory.toFile().listFiles()?.size ?: 0)
        } finally {
            sourceFile.deleteIfExists()
            cacheDirectory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `failed materialization removes the partial file`() = runTest {
        val sourceFile = Files.createTempFile("installerx-analysis", ".apk")
        val cacheDirectory = Files.createTempDirectory("installerx-materialized")
        Files.write(sourceFile, byteArrayOf(1, 2))

        try {
            val source = descriptorEntity(
                analysisFile = sourceFile.toFile(),
                installBytes = byteArrayOf(1, 2),
                policy = AnalysisMaterializationPolicy.RETAINED_SOURCE_REPLACEMENT,
                key = AnalysisMaterializationKey(),
            )

            val error = assertFailsWith<AnalyseException> {
                materializeAnalysisSource(
                    data = listOf(source),
                    requestedSource = source,
                    cacheDirectory = cacheDirectory.toFile(),
                ) { input, output, _ ->
                    output.write(input.read())
                    error("copy failed")
                }
            }

            assertEquals(AnalyseErrorType.SOURCE_MATERIALIZATION_FAILED, error.errorType)
            assertEquals(0, cacheDirectory.toFile().listFiles()?.size ?: 0)
        } finally {
            sourceFile.deleteIfExists()
            cacheDirectory.toFile().deleteRecursively()
        }
    }

    private fun descriptorEntity(
        analysisFile: java.io.File,
        installBytes: ByteArray,
        policy: AnalysisMaterializationPolicy,
        key: AnalysisMaterializationKey?,
    ) = DataEntity.FileDescriptorEntity(
        path = "remote.apk",
        startOffset = 0L,
        length = installBytes.size.toLong(),
        channelFactory = {
            FileChannel.open(analysisFile.toPath(), StandardOpenOption.READ)
        },
        descriptorFactory = { error("descriptor is unused") },
        inputStreamFactory = { installBytes.inputStream() },
        analysisMaterializationPolicy = policy,
        analysisMaterializationKey = key,
    )
}
