// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.executor.appinstaller

import com.rosan.installer.domain.session.model.ProgressEntity
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.EOFException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class InstallProgressWriterTest {
    @Test
    fun `reports aggregate byte progress across entries`() = runTest {
        val first = ByteArray(2 * 1024 * 1024) { (it % 251).toByte() }
        val second = ByteArray(1024 * 1024) { (it % 239).toByte() }
        val progress = mutableListOf<Pair<Long, Long>>()
        var clock = 0L
        val writer = InstallProgressWriter(
            totalBytes = first.size.toLong() + second.size,
            onProgress = { progress += it.bytesWritten to it.totalBytes },
            nanoTime = {
                clock += 250_000_000L
                clock
            },
        )

        writer.start()
        val firstOutput = ByteArrayOutputStream()
        writer.copy(
            input = ByteArrayInputStream(first),
            output = firstOutput,
            expectedBytes = first.size.toLong(),
        )
        val secondOutput = ByteArrayOutputStream()
        writer.copy(
            input = ByteArrayInputStream(second),
            output = secondOutput,
            expectedBytes = second.size.toLong(),
        )

        assertContentEquals(first, firstOutput.toByteArray())
        assertContentEquals(second, secondOutput.toByteArray())
        assertEquals(0L to (first.size + second.size).toLong(), progress.first())
        assertEquals((first.size + second.size).toLong() to (first.size + second.size).toLong(), progress.last())
        assertTrue(progress.zipWithNext().all { (previous, next) -> next.first >= previous.first })
        assertTrue(progress.any { it.first == first.size.toLong() })
    }

    @Test
    fun `rejects a truncated install source`() = runTest {
        val writer = InstallProgressWriter(totalBytes = 10L, onProgress = {})
        writer.start()

        assertFailsWith<EOFException> {
            writer.copy(
                input = ByteArrayInputStream(ByteArray(9)),
                output = ByteArrayOutputStream(),
                expectedBytes = 10L,
            )
        }
    }

    @Test
    fun `rejects a source larger than its declared size before writing excess bytes`() = runTest {
        val output = ByteArrayOutputStream()
        val writer = InstallProgressWriter(totalBytes = 10L, onProgress = {})
        writer.start()

        assertFailsWith<EOFException> {
            writer.copy(
                input = ByteArrayInputStream(ByteArray(11)),
                output = output,
                expectedBytes = 10L,
            )
        }

        assertEquals(10, output.size())
    }

    @Test
    fun `combines current item byte progress with batch position`() {
        val progress = ProgressEntity.Installing(
            current = 2,
            total = 4,
            writeProgress = 0.5f,
        )

        assertEquals(0.375f, progress.overallProgress())
        assertEquals(null, progress.copy(writeProgress = null).overallProgress())
    }

    @Test
    fun `keeps progress indeterminate when any source size is unknown`() {
        assertNull(InstallProgressWriter.totalBytesOrNull(listOf(1024L, -1L)))
        assertNull(InstallProgressWriter.totalBytesOrNull(listOf(Long.MAX_VALUE, 1L)))
    }
}
