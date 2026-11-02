// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.virustotal.model

data class VirusTotalStats(
    val malicious: Int = 0,
    val suspicious: Int = 0,
    val harmless: Int = 0,
    val undetected: Int = 0,
    val timeout: Int = 0,
) {
    val detections: Int
        get() = malicious + suspicious

    val total: Int
        get() = malicious + suspicious + harmless + undetected + timeout
}

data class VirusTotalFileReport(
    val sha256: String,
    val stats: VirusTotalStats,
) {
    val guiUrl: String
        get() = "https://www.virustotal.com/gui/file/$sha256"
}

enum class VirusTotalAnalysisStatus {
    QUEUED,
    IN_PROGRESS,
    COMPLETED,
    UNKNOWN,
    ;

    companion object {
        fun fromValue(value: String?): VirusTotalAnalysisStatus = when (value) {
            "queued" -> QUEUED
            "in-progress" -> IN_PROGRESS
            "completed" -> COMPLETED
            else -> UNKNOWN
        }
    }
}

data class VirusTotalAnalysis(
    val id: String,
    val status: VirusTotalAnalysisStatus,
    val stats: VirusTotalStats? = null,
    val fileSha256: String? = null,
)

data class VirusTotalHash(
    val sha256: String,
    val size: Long,
)

enum class VirusTotalErrorKind {
    NO_API_KEY,
    INTERNET_DISABLED,
    UNAUTHORIZED,
    RATE_LIMITED,
    FILE_TOO_LARGE,
    NETWORK,
    HTTP,
    UNSUPPORTED_SOURCE,
    HASH_FAILED,
    ANALYSIS_TIMEOUT,
    UNKNOWN,
}

class VirusTotalException(
    val kind: VirusTotalErrorKind,
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause)
