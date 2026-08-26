// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.model.preferences

import com.rosan.installer.domain.settings.model.config.Authorizer

data class SmartAuthorizerCandidate(val authorizer: Authorizer, val enabled: Boolean)

object SmartAuthorizerPreferences {
    private val supportedAuthorizers = listOf(
        Authorizer.Root,
        Authorizer.Shizuku,
        Authorizer.Dhizuku,
        Authorizer.None,
    )

    fun defaultCandidates(isSessionInstallSupported: Boolean): List<SmartAuthorizerCandidate> = supportedAuthorizers
        .filter { it != Authorizer.None || isSessionInstallSupported }
        .map { SmartAuthorizerCandidate(authorizer = it, enabled = true) }

    fun decode(value: String, isSessionInstallSupported: Boolean): List<SmartAuthorizerCandidate> {
        if (value.isBlank()) return defaultCandidates(isSessionInstallSupported)

        val parsed = value
            .split(',')
            .mapNotNull { token ->
                val parts = token.split(':', limit = 2)
                val authorizer = Authorizer.fromValueOrDefault(parts.firstOrNull().orEmpty())
                if (authorizer !in supportedAuthorizers) return@mapNotNull null

                SmartAuthorizerCandidate(
                    authorizer = authorizer,
                    enabled = parts.getOrNull(1) != "0",
                )
            }
            .distinctBy { it.authorizer }
            .filter { it.authorizer != Authorizer.None || isSessionInstallSupported }

        if (parsed.isEmpty()) return defaultCandidates(isSessionInstallSupported)

        val missing = defaultCandidates(isSessionInstallSupported)
            .filterNot { candidate -> parsed.any { it.authorizer == candidate.authorizer } }

        return parsed + missing
    }

    fun encode(candidates: List<SmartAuthorizerCandidate>): String = candidates
        .filter { it.authorizer in supportedAuthorizers }
        .distinctBy { it.authorizer }
        .joinToString(",") { candidate ->
            "${candidate.authorizer.value}:${if (candidate.enabled) "1" else "0"}"
        }
}
