// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.session.resolver

import java.io.IOException

internal data class ContentRange(
    val start: Long,
    val end: Long,
    val total: Long
) {
    fun matches(start: Long, end: Long, total: Long = this.total): Boolean =
        this.start == start && this.end == end && this.total == total

    fun requireMatches(start: Long, end: Long, total: Long) {
        if (!matches(start, end, total)) {
            throw IOException(
                "Unexpected Content-Range: expected bytes $start-$end/$total, " +
                        "actual=bytes ${this.start}-${this.end}/${this.total}"
            )
        }
    }

    companion object {
        private val pattern = Regex("^bytes ([0-9]+)-([0-9]+)/([0-9]+)$", RegexOption.IGNORE_CASE)

        fun parse(value: String?): ContentRange? {
            val match = value?.trim()?.let(pattern::matchEntire) ?: return null
            val start = match.groupValues[1].toLongOrNull() ?: return null
            val end = match.groupValues[2].toLongOrNull() ?: return null
            val total = match.groupValues[3].toLongOrNull() ?: return null
            if (start < 0L || end < start || total <= end) return null
            return ContentRange(start, end, total)
        }
    }
}
