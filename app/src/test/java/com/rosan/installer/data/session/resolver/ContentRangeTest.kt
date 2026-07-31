// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.session.resolver

import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ContentRangeTest {
    @Test
    fun `parses a valid byte range`() {
        assertEquals(ContentRange(0L, 3L, 100L), ContentRange.parse("bytes 0-3/100"))
    }

    @Test
    fun `rejects a wrong start offset`() {
        val range = requireNotNull(ContentRange.parse("bytes 1-3/100"))

        assertFailsWith<IOException> { range.requireMatches(0L, 3L, 100L) }
    }

    @Test
    fun `rejects a wrong end offset`() {
        val range = requireNotNull(ContentRange.parse("bytes 0-4/100"))

        assertFailsWith<IOException> { range.requireMatches(0L, 3L, 100L) }
    }

    @Test
    fun `rejects a changed total length`() {
        val range = requireNotNull(ContentRange.parse("bytes 0-3/101"))

        assertFailsWith<IOException> { range.requireMatches(0L, 3L, 100L) }
    }

    @Test
    fun `rejects a missing content range`() {
        assertNull(ContentRange.parse(null))
    }

    @Test
    fun `rejects malformed or unsatisfied ranges`() {
        assertTrue(
            listOf(
                "0-3/100",
                "bytes 3-0/100",
                "bytes 0-100/100",
                "bytes 0-3/*",
                "bytes */100"
            ).all { ContentRange.parse(it) == null }
        )
    }
}
