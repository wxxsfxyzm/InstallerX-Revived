// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.signature

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import com.rosan.installer.domain.engine.model.packageinfo.AppSignatureInfo
import timber.log.Timber

/**
 * Signature reader for installed packages.
 */
class InstalledPackageSignatureReader(
    private val context: Context,
    private val certificateFormatter: CertificateFormatter
) {
    fun hasSigningCertificate(packageName: String, certificateSha256: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        val certificateBytes = certificateSha256.hexToByteArrayOrNull() ?: return false
        return runCatching {
            context.packageManager.hasSigningCertificate(
                packageName,
                certificateBytes,
                PackageManager.CERT_INPUT_SHA256
            )
        }.getOrDefault(false)
    }

    /**
     * PackageManager SigningInfo is the source of truth for installed app signer certificates.
     */
    fun read(packageName: String): AppSignatureInfo? {
        return try {
            val packageInfo = getInstalledPackageInfo(packageName)
            val signatures = getInstalledSignatures(packageInfo)
            if (signatures.current.isEmpty()) return null

            val certificates = signatures.current.map { signature ->
                certificateFormatter.format(signature)
            }
            val signingCertificateHistory = signatures.history.map { signature ->
                certificateFormatter.format(signature)
            }
            AppSignatureInfo(
                verified = true,
                signerSha256Set = certificates.mapTo(linkedSetOf()) { it.sha256 },
                certificates = certificates,
                signingCertificateHistory = signingCertificateHistory,
                hasMultipleSigners = signatures.hasMultipleSigners
            )
        } catch (_: PackageManager.NameNotFoundException) {
            Timber.d("Package not found, can't get signature: $packageName")
            null
        } catch (e: Exception) {
            Timber.e(e, "Failed to get signature hash for installed package: $packageName")
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun getInstalledPackageInfo(packageName: String): PackageInfo {
        val packageManager = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES or PackageManager.MATCH_UNINSTALLED_PACKAGES
        } else {
            PackageManager.GET_SIGNATURES or PackageManager.MATCH_UNINSTALLED_PACKAGES
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            packageManager.getPackageInfo(packageName, flags)
        }
    }

    private fun getInstalledSignatures(packageInfo: PackageInfo?): InstalledSignatures {
        packageInfo ?: return InstalledSignatures()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = packageInfo.signingInfo
            when {
                signingInfo == null -> InstalledSignatures()
                signingInfo.hasMultipleSigners() -> InstalledSignatures(
                    current = signingInfo.apkContentsSigners?.toList().orEmpty(),
                    hasMultipleSigners = true
                )

                else -> {
                    val history = signingInfo.signingCertificateHistory?.toList().orEmpty()
                    val current = history.lastOrNull()?.let(::listOf)
                        ?: signingInfo.apkContentsSigners?.toList().orEmpty()
                    InstalledSignatures(
                        current = current,
                        history = history,
                        hasMultipleSigners = false
                    )
                }
            }
        } else {
            @Suppress("DEPRECATION")
            val signatures = packageInfo.signatures?.toList().orEmpty()
            InstalledSignatures(
                current = signatures,
                hasMultipleSigners = signatures.size > 1
            )
        }
    }

    private fun String.hexToByteArrayOrNull(): ByteArray? {
        if (length % 2 != 0) return null
        val bytes = ByteArray(length / 2)
        for (index in bytes.indices) {
            val high = Character.digit(this[index * 2], 16)
            val low = Character.digit(this[index * 2 + 1], 16)
            if (high < 0 || low < 0) return null
            bytes[index] = ((high shl 4) + low).toByte()
        }
        return bytes
    }

    private data class InstalledSignatures(
        val current: List<Signature> = emptyList(),
        val history: List<Signature> = emptyList(),
        val hasMultipleSigners: Boolean = false
    )
}
