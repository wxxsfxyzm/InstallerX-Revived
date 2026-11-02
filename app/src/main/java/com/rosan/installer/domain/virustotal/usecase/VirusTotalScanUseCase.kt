// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.virustotal.usecase

import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.virustotal.model.VirusTotalAnalysisStatus
import com.rosan.installer.domain.virustotal.model.VirusTotalErrorKind
import com.rosan.installer.domain.virustotal.model.VirusTotalException
import com.rosan.installer.domain.virustotal.model.VirusTotalFileReport
import com.rosan.installer.domain.virustotal.model.VirusTotalScanState
import com.rosan.installer.domain.virustotal.repository.VirusTotalRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class VirusTotalScanUseCase(private val repository: VirusTotalRepository) {

    /** Normal mode, step 1: hash the file locally and ask VirusTotal whether the hash is known. */
    fun lookupByHash(apiKey: String, source: DataEntity): Flow<VirusTotalScanState> = flow {
        var sha256: String? = null
        try {
            emit(VirusTotalScanState.Hashing)
            val hash = repository.computeSha256(source)
            sha256 = hash.sha256
            emit(VirusTotalScanState.CheckingHash(hash.sha256))
            val report = repository.lookupBySha256(apiKey, hash.sha256)
            if (report != null) {
                emit(VirusTotalScanState.Completed(report, fromUpload = false))
            } else {
                emit(VirusTotalScanState.NotFound(hash.sha256, hash.size))
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: VirusTotalException) {
            emit(VirusTotalScanState.Failed(error.kind, sha256, error.message))
        } catch (error: Exception) {
            emit(VirusTotalScanState.Failed(VirusTotalErrorKind.UNKNOWN, sha256, error.message))
        }
    }

    /**
     * Uploads the file and polls until the analysis completes.
     * Used by the optional upload button in Normal mode and automatically in Strict mode.
     */
    fun uploadAndWait(
        apiKey: String,
        source: DataEntity,
        fileName: String,
        knownSha256: String? = null,
        knownSize: Long? = null,
    ): Flow<VirusTotalScanState> = flow {
        var sha256 = knownSha256
        try {
            val size = if (knownSha256 != null && knownSize != null && knownSize >= 0L) {
                knownSize
            } else {
                emit(VirusTotalScanState.Hashing)
                val hash = repository.computeSha256(source)
                sha256 = hash.sha256
                hash.size
            }
            emit(VirusTotalScanState.Uploading(sha256))
            val queued = repository.upload(apiKey, source, fileName, size)
            var analysis = queued
            var attempts = 0
            while (analysis.status != VirusTotalAnalysisStatus.COMPLETED) {
                if (attempts >= MAX_POLL_ATTEMPTS) {
                    emit(VirusTotalScanState.Failed(VirusTotalErrorKind.ANALYSIS_TIMEOUT, sha256 ?: analysis.fileSha256))
                    return@flow
                }
                emit(VirusTotalScanState.WaitingForAnalysis(sha256 ?: analysis.fileSha256, analysis.status))
                delay(POLL_INTERVAL_MS)
                analysis = repository.getAnalysis(apiKey, analysis.id)
                attempts++
            }
            val reportSha256 = analysis.fileSha256 ?: sha256
            val report = reportSha256?.let { hash ->
                repository.lookupBySha256(apiKey, hash)
                    ?: analysis.stats?.let { stats -> VirusTotalFileReport(hash, stats) }
            } ?: analysis.stats?.let { stats -> sha256?.let { hash -> VirusTotalFileReport(hash, stats) } }
            if (report != null) {
                emit(VirusTotalScanState.Completed(report, fromUpload = true))
            } else {
                emit(VirusTotalScanState.Failed(VirusTotalErrorKind.UNKNOWN, sha256))
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: VirusTotalException) {
            emit(VirusTotalScanState.Failed(error.kind, sha256, error.message))
        } catch (error: Exception) {
            emit(VirusTotalScanState.Failed(VirusTotalErrorKind.UNKNOWN, sha256, error.message))
        }
    }

    private companion object {
        const val POLL_INTERVAL_MS = 15_000L

        // 40 * 15s = 10 minutes of polling after the upload has been accepted.
        const val MAX_POLL_ATTEMPTS = 40
    }
}
