// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.model.packageinfo

/** Capabilities granted by a verified proof of rotation; a missing entry means unknown. */
data class SigningCertificateCapabilities(val installedData: Boolean, val rollback: Boolean)

/** A hybrid's two active keys have distinct roles and are not ordinary multiple signers. */
data class SigningIdentity(
    val signerSha256Set: Set<String>,
    val historySha256: List<String> = emptyList(),
    val capabilities: Map<String, SigningCertificateCapabilities> = emptyMap(),
    val isHybrid: Boolean = false,
    val classicalSha256: String? = null,
) {
    val isComplete: Boolean
        get() = signerSha256Set.isNotEmpty() && (
            !isHybrid ||
                (
                    signerSha256Set.size == 1 && classicalSha256 != null &&
                        classicalSha256 != signerSha256Set.single() &&
                        historySha256.takeLast(2) == listOf(classicalSha256, signerSha256Set.single())
                    )
            )

    fun matches(other: SigningIdentity): Boolean = isComplete && other.isComplete &&
        isHybrid == other.isHybrid && signerSha256Set == other.signerSha256Set &&
        classicalSha256 == other.classicalSha256
}

enum class SigningIdentityComparison { MATCH, MISMATCH, UNKNOWN }

/**
 * Compares the same platform branch on both sides. A complete installed hybrid identity confirms
 * V3.2 selection; an installed legacy identity leaves the device's feature flag unknown.
 */
fun AppSignatureInfo.compareSigningIdentity(
    other: AppSignatureInfo,
    installedIdentity: SigningIdentity? = null,
): SigningIdentityComparison {
    val pairs = when {
        installedIdentity?.let { it.isHybrid && it.isComplete } == true ->
            listOf(signingIdentity to other.signingIdentity)

        fallbackIdentity != null && other.fallbackIdentity != null ->
            listOf(signingIdentity to other.signingIdentity, fallbackIdentity to other.fallbackIdentity)

        fallbackIdentity != null && !other.isHybrid ->
            listOf(signingIdentity to other.signingIdentity, fallbackIdentity to other.signingIdentity)

        other.fallbackIdentity != null && !isHybrid ->
            listOf(signingIdentity to other.signingIdentity, signingIdentity to other.fallbackIdentity)

        else -> listOf(signingIdentity to other.signingIdentity)
    }
    val results = pairs.map { (left, right) ->
        when {
            !left.isComplete || !right.isComplete -> SigningIdentityComparison.UNKNOWN
            left.matches(right) -> SigningIdentityComparison.MATCH
            else -> SigningIdentityComparison.MISMATCH
        }
    }.distinct()
    return results.singleOrNull() ?: SigningIdentityComparison.UNKNOWN
}
