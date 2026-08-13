// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.session.resolver

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class SeekableBlockCacheTest {
    private val source = ByteArray(32) { it.toByte() }

    @Test
    fun `small reads in one block share a single load`() {
        val loads = mutableListOf<Pair<Long, Int>>()
        val cache = createCache(loads)

        assertContentEquals(byteArrayOf(1, 2, 3), read(cache, offset = 1, size = 3))
        assertContentEquals(byteArrayOf(4, 5), read(cache, offset = 4, size = 2))

        assertEquals(listOf(0L to 8), loads)
    }

    @Test
    fun `reads spanning blocks are assembled in order`() {
        val loads = mutableListOf<Pair<Long, Int>>()
        val cache = createCache(loads)

        assertContentEquals(source.copyOfRange(6, 18), read(cache, offset = 6, size = 12))

        assertEquals(listOf(0L to 8, 8L to 8, 16L to 8), loads)
    }

    @Test
    fun `least recently used blocks are bounded by memory capacity`() {
        val loads = mutableListOf<Pair<Long, Int>>()
        val cache = createCache(loads, maxCachedBytes = 16)

        read(cache, offset = 0, size = 1)
        read(cache, offset = 8, size = 1)
        read(cache, offset = 16, size = 1)
        read(cache, offset = 0, size = 1)

        assertEquals(listOf(0L to 8, 8L to 8, 16L to 8, 0L to 8), loads)
    }

    @Test
    fun `final block and end of source use exact lengths`() {
        val loads = mutableListOf<Pair<Long, Int>>()
        val cache = createCache(loads)

        assertContentEquals(byteArrayOf(30, 31), read(cache, offset = 30, size = 8))
        assertEquals(0, cache.read(offset = 32, requestedSize = 4, destination = ByteArray(4)))

        assertEquals(listOf(24L to 8), loads)
    }

    @Test
    fun `runtime cache budget uses a safe share of available heap`() {
        assertEquals(
            24 * 1024 * 1024,
            calculateRuntimeRangeCacheBudget(
                maximumBytes = 128 * 1024 * 1024,
                maxHeapBytes = 256 * 1024 * 1024L,
                allocatedHeapBytes = 64 * 1024 * 1024L,
                blockSize = 1024 * 1024
            )
        )
    }

    @Test
    fun `runtime cache budget respects maximum and minimum block bounds`() {
        assertEquals(
            1024 * 1024,
            calculateRuntimeRangeCacheBudget(
                maximumBytes = 1024 * 1024,
                maxHeapBytes = 256 * 1024 * 1024L,
                allocatedHeapBytes = 64 * 1024 * 1024L,
                blockSize = 1024 * 1024
            )
        )
        assertEquals(
            1024 * 1024,
            calculateRuntimeRangeCacheBudget(
                maximumBytes = 128 * 1024 * 1024,
                maxHeapBytes = 256 * 1024 * 1024L,
                allocatedHeapBytes = 255 * 1024 * 1024L,
                blockSize = 1024 * 1024
            )
        )
    }

    private fun createCache(
        loads: MutableList<Pair<Long, Int>>,
        maxCachedBytes: Int = 32
    ) = SeekableBlockCache(
        contentLength = source.size.toLong(),
        blockSize = 8,
        maxCachedBytes = maxCachedBytes
    ) { offset, size ->
        loads += offset to size
        source.copyOfRange(offset.toInt(), offset.toInt() + size)
    }

    private fun read(cache: SeekableBlockCache, offset: Long, size: Int): ByteArray {
        val destination = ByteArray(size)
        val count = cache.read(offset, size, destination)
        return destination.copyOf(count)
    }
}
