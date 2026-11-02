// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.virustotal.repository

import com.rosan.installer.domain.virustotal.model.VirusTotalAnalysis
import com.rosan.installer.domain.virustotal.model.VirusTotalAnalysisStatus
import com.rosan.installer.domain.virustotal.model.VirusTotalFileReport
import com.rosan.installer.domain.virustotal.model.VirusTotalStats
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Pure JSON parsing for the VirusTotal v3 API, kept separate from HTTP so it can be unit tested. */
object VirusTotalResponseParser {

    fun parseFileReport(json: Json, body: String, fallbackSha256: String): VirusTotalFileReport {
        val data = json.parseToJsonElement(body).jsonObject.getValue("data").jsonObject
        val attributes = data["attributes"]?.jsonObject ?: JsonObject(emptyMap())
        return VirusTotalFileReport(
            sha256 = attributes["sha256"]?.jsonPrimitive?.contentOrNull
                ?: data["id"]?.jsonPrimitive?.contentOrNull
                ?: fallbackSha256,
            stats = parseStats(attributes["last_analysis_stats"]?.jsonObject),
        )
    }

    fun parseAnalysis(json: Json, body: String, fallbackId: String): VirusTotalAnalysis {
        val root = json.parseToJsonElement(body).jsonObject
        val data = root.getValue("data").jsonObject
        val attributes = data["attributes"]?.jsonObject ?: JsonObject(emptyMap())
        val metaFileInfo = root["meta"]?.jsonObject?.get("file_info")?.jsonObject
        val itemLink = data["links"]?.jsonObject?.get("item")?.jsonPrimitive?.contentOrNull
        return VirusTotalAnalysis(
            id = data["id"]?.jsonPrimitive?.contentOrNull ?: fallbackId,
            status = VirusTotalAnalysisStatus.fromValue(attributes["status"]?.jsonPrimitive?.contentOrNull),
            stats = attributes["stats"]?.jsonObject?.let { parseStats(it) },
            fileSha256 = metaFileInfo?.get("sha256")?.jsonPrimitive?.contentOrNull
                ?: itemLink?.substringAfterLast('/'),
        )
    }

    fun parseUploadAnalysisId(json: Json, body: String): String? = json.parseToJsonElement(body).jsonObject["data"]?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull

    fun parseUploadUrl(json: Json, body: String): String? = json.parseToJsonElement(body).jsonObject["data"]?.jsonPrimitive?.contentOrNull

    fun parseStats(stats: JsonObject?): VirusTotalStats {
        if (stats == null) return VirusTotalStats()
        fun count(key: String): Int = stats[key]?.jsonPrimitive?.intOrNull ?: 0
        return VirusTotalStats(
            malicious = count("malicious"),
            suspicious = count("suspicious"),
            harmless = count("harmless"),
            undetected = count("undetected"),
            timeout = count("timeout") + count("confirmed-timeout"),
        )
    }
}
