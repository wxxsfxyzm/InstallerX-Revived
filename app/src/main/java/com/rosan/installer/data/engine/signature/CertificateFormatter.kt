// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.signature

import android.content.pm.Signature
import com.rosan.installer.domain.engine.model.packageinfo.SignatureCertificateInfo
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class CertificateFormatter {
    fun format(signature: Signature): SignatureCertificateInfo {
        val encoded = signature.toByteArray()
        val certificate = encoded.toX509CertificateOrNull()
        return SignatureCertificateInfo(
            sha256 = encoded.digestHex(SHA_256),
            sha1 = encoded.digestHex(SHA_1),
            subject = certificate?.subjectX500Principal?.name ?: UNKNOWN,
            issuer = certificate?.issuerX500Principal?.name ?: UNKNOWN,
            serialNumber = certificate?.serialNumber?.toString(16) ?: UNKNOWN,
            validFrom = certificate?.notBefore?.formatUtc(),
            validUntil = certificate?.notAfter?.formatUtc(),
            publicKeyAlgorithm = certificate?.publicKey?.algorithm,
            signatureAlgorithm = certificate?.sigAlgName
        )
    }

    fun format(certificate: X509Certificate): SignatureCertificateInfo {
        val encoded = certificate.encoded
        return SignatureCertificateInfo(
            sha256 = encoded.digestHex(SHA_256),
            sha1 = encoded.digestHex(SHA_1),
            subject = certificate.subjectX500Principal.name,
            issuer = certificate.issuerX500Principal.name,
            serialNumber = certificate.serialNumber.toString(16),
            validFrom = certificate.notBefore.formatUtc(),
            validUntil = certificate.notAfter.formatUtc(),
            publicKeyAlgorithm = certificate.publicKey.algorithm,
            signatureAlgorithm = certificate.sigAlgName
        )
    }

    private fun ByteArray.toX509CertificateOrNull() =
        runCatching {
            CertificateFactory.getInstance("X.509")
                .generateCertificate(ByteArrayInputStream(this)) as X509Certificate
        }.getOrNull()

    private fun ByteArray.digestHex(algorithm: String): String {
        val digest = MessageDigest.getInstance(algorithm)
        return digest.digest(this).joinToString("") { "%02x".format(it) }
    }

    private fun java.util.Date.formatUtc(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(this)
    }

    private companion object {
        const val SHA_1 = "SHA-1"
        const val SHA_256 = "SHA-256"
        const val UNKNOWN = "Unknown"
    }
}
