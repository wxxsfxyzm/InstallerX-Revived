// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.virustotal.repository

import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.virustotal.model.VirusTotalAnalysis
import com.rosan.installer.domain.virustotal.model.VirusTotalFileReport
import com.rosan.installer.domain.virustotal.model.VirusTotalHash

interface VirusTotalRepository {
    /** Streams [source] once and returns its SHA-256 and byte size. The source must be re-readable. */
    suspend fun computeSha256(source: DataEntity): VirusTotalHash

    /** Returns the existing VirusTotal report for [sha256], or null when the hash is unknown (HTTP 404). */
    suspend fun lookupBySha256(apiKey: String, sha256: String): VirusTotalFileReport?

    /**
     * Uploads [source] for analysis and returns the queued analysis.
     * Files larger than [MAX_UPLOAD_SIZE_BYTES] are rejected before any upload happens.
     */
    suspend fun upload(apiKey: String, source: DataEntity, fileName: String, size: Long): VirusTotalAnalysis

    suspend fun getAnalysis(apiKey: String, analysisId: String): VirusTotalAnalysis

    companion object {
        /** VirusTotal accepts direct uploads up to 32 MB; larger files need a dedicated upload URL. */
        const val DIRECT_UPLOAD_LIMIT_BYTES: Long = 32L * 1024L * 1024L

        /** Maximum file size accepted by VirusTotal through the dedicated upload URL. */
        const val MAX_UPLOAD_SIZE_BYTES: Long = 650L * 1024L * 1024L
    }
}
