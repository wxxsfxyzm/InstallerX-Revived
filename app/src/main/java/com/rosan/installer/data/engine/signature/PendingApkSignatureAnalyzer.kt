// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.signature

import com.android.apksig.ApkVerifier
import com.rosan.installer.domain.engine.exception.AnalyseException
import com.rosan.installer.domain.engine.model.packageinfo.AppSignatureInfo
import com.rosan.installer.domain.engine.model.source.DataEntity
import timber.log.Timber
import java.io.File
import java.security.cert.X509Certificate
import java.util.UUID

/**
 * APK signature analyzer.
 */
class PendingApkSignatureAnalyzer(
    private val certificateFormatter: CertificateFormatter
) {
    /**
     * apksig is the source of truth for pending APK signer certificates.
     */
    fun analyze(apkPath: String): AppSignatureInfo {
        return analyze(File(apkPath), apkPath)
    }

    fun analyze(data: DataEntity, cacheDirectory: String): AppSignatureInfo? {
        return when (data) {
            is DataEntity.FileEntity -> analyze(File(data.path), data.path)
            else -> analyzeStream(data, cacheDirectory)
        }
    }

    private fun analyze(file: File, displayName: String): AppSignatureInfo {
        return try {
            val result = ApkVerifier.Builder(file).build().verify()
            val certificates = result.signerCertificates.map { certificate ->
                certificateFormatter.format(certificate)
            }
            AppSignatureInfo(
                verified = result.isVerified,
                signerSha256Set = certificates.mapTo(linkedSetOf()) { it.sha256 },
                certificates = certificates,
                signingCertificateHistory = result.signingCertificateLineageCertificates().map { certificate ->
                    certificateFormatter.format(certificate)
                },
                hasMultipleSigners = certificates.size > 1,
                verifiedSchemes = result.verifiedSchemes(),
                warnings = result.warnings.map { it.toString() },
                errors = result.errors.map { it.toString() }
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to get signature hash from APK: $displayName")
            failedSignatureInfo(e.message ?: e::class.java.simpleName)
        }
    }

    private fun analyzeStream(
        data: DataEntity,
        cacheDirectory: String
    ): AppSignatureInfo {
        val tempFile = createSignatureTempFile(cacheDirectory)
        return try {
            val input = data.getInputStream()
                ?: return failedSignatureInfo("Unable to open APK input stream")
            input.use { source ->
                tempFile.outputStream().use { output -> source.copyTo(output) }
            }
            analyze(tempFile, data.toString())
        } catch (e: AnalyseException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to get signature hash from APK data: $data")
            failedSignatureInfo(e.message ?: e::class.java.simpleName)
        } finally {
            tempFile.delete()
        }
    }

    private fun createSignatureTempFile(cacheDirectory: String): File {
        val cacheDir = File(cacheDirectory).apply { mkdirs() }
        return File.createTempFile("sig_${UUID.randomUUID()}", ".apk", cacheDir)
    }

    private fun failedSignatureInfo(message: String): AppSignatureInfo {
        return AppSignatureInfo(
            verified = false,
            signerSha256Set = emptySet(),
            certificates = emptyList(),
            errors = listOf(message)
        )
    }

    private fun ApkVerifier.Result.verifiedSchemes(): List<String> {
        return buildList {
            if (isVerifiedUsingV1Scheme) add("V1")
            if (isVerifiedUsingV2Scheme) add("V2")
            if (isVerifiedUsingV3Scheme) add("V3")
            if (isVerifiedUsingV31Scheme) add("V3.1")
            if (isVerifiedUsingV4Scheme) add("V4")
        }
    }

    private fun ApkVerifier.Result.signingCertificateLineageCertificates(): List<X509Certificate> {
        return runCatching {
            signingCertificateLineage?.certificatesInLineage.orEmpty()
        }.getOrElse { error ->
            Timber.w(error, "Failed to read APK signing certificate lineage")
            emptyList()
        }
    }
}
