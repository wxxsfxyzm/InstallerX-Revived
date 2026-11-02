// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.virustotal.repository

import com.rosan.installer.domain.virustotal.model.VirusTotalAnalysisStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json

class VirusTotalResponseParserTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `file report parses sha256 and last analysis stats`() {
        val body = """
            {
              "data": {
                "id": "abc123",
                "type": "file",
                "attributes": {
                  "sha256": "abc123",
                  "last_analysis_stats": {
                    "malicious": 2,
                    "suspicious": 1,
                    "harmless": 60,
                    "undetected": 10,
                    "timeout": 1,
                    "confirmed-timeout": 1,
                    "failure": 0,
                    "type-unsupported": 3
                  }
                }
              }
            }
        """.trimIndent()

        val report = VirusTotalResponseParser.parseFileReport(json, body, "fallback")

        assertEquals("abc123", report.sha256)
        assertEquals("https://www.virustotal.com/gui/file/abc123", report.guiUrl)
        assertEquals(2, report.stats.malicious)
        assertEquals(1, report.stats.suspicious)
        assertEquals(60, report.stats.harmless)
        assertEquals(10, report.stats.undetected)
        assertEquals(2, report.stats.timeout)
        assertEquals(3, report.stats.detections)
        assertEquals(75, report.stats.total)
    }

    @Test
    fun `analysis parses status stats and file hash from meta`() {
        val body = """
            {
              "meta": { "file_info": { "sha256": "deadbeef" } },
              "data": {
                "id": "analysis-id",
                "type": "analysis",
                "attributes": {
                  "status": "completed",
                  "stats": { "malicious": 0, "suspicious": 0, "harmless": 65, "undetected": 8 }
                }
              }
            }
        """.trimIndent()

        val analysis = VirusTotalResponseParser.parseAnalysis(json, body, "fallback-id")

        assertEquals("analysis-id", analysis.id)
        assertEquals(VirusTotalAnalysisStatus.COMPLETED, analysis.status)
        assertEquals("deadbeef", analysis.fileSha256)
        assertEquals(0, analysis.stats?.detections)
        assertEquals(73, analysis.stats?.total)
    }

    @Test
    fun `queued analysis without stats stays in progress`() {
        val body = """
            {
              "data": {
                "id": "queued-id",
                "attributes": { "status": "queued" },
                "links": { "item": "https://www.virustotal.com/api/v3/files/cafef00d" }
              }
            }
        """.trimIndent()

        val analysis = VirusTotalResponseParser.parseAnalysis(json, body, "fallback-id")

        assertEquals(VirusTotalAnalysisStatus.QUEUED, analysis.status)
        assertEquals("cafef00d", analysis.fileSha256)
        assertNull(analysis.stats)
    }

    @Test
    fun `upload responses expose analysis id and upload url`() {
        assertEquals(
            "xyz",
            VirusTotalResponseParser.parseUploadAnalysisId(json, """{ "data": { "id": "xyz", "type": "analysis" } }"""),
        )
        assertEquals(
            "https://upload.example.test/abc",
            VirusTotalResponseParser.parseUploadUrl(json, """{ "data": "https://upload.example.test/abc" }"""),
        )
    }
}
