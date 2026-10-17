// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.signature

import com.android.apksig.ApkSigner
import com.android.apksig.KeyConfig
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.security.auth.x500.X500Principal

/** Generates real signed APKs in the test's temporary directory, using only JVM APIs and apksig. */
internal object SignatureTestFixtures {
    enum class Stamp { VALID, TIMESTAMP, CERTIFICATE_MISMATCH, MISSING_BLOCK }

    private val appSigner by lazy { createSigner("app") }
    private val stampSigner by lazy { createSigner("stamp") }

    fun create(directory: File, stamp: Stamp): File {
        val workDirectory = Files.createTempDirectory(directory.toPath(), "signature-fixture-").toFile()
        val unsigned = File(workDirectory, "unsigned.apk")
        ZipOutputStream(unsigned.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("AndroidManifest.xml"))
            zip.write(manifest())
            zip.closeEntry()
        }
        val signed = File(workDirectory, "signed.apk")
        ApkSigner.Builder(listOf(appSigner))
            .setInputApk(unsigned)
            .setOutputApk(signed)
            .setMinSdkVersion(28)
            .setV1SigningEnabled(false)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(false)
            .setV4SigningEnabled(false)
            .setSourceStampSignerConfig(stampSigner)
            .setSourceStampTimestampEnabled(stamp == Stamp.TIMESTAMP)
            .build().sign()
        if (stamp == Stamp.CERTIFICATE_MISMATCH || stamp == Stamp.MISSING_BLOCK) {
            alterStamp(signed, stamp)
        }
        return signed
    }

    private fun createSigner(name: String): ApkSigner.SignerConfig {
        val keys = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        // SHA256withRSA AlgorithmIdentifier, including its NULL parameters.
        val algorithm = der(0x30, der(0x06, byteArrayOf(0x2a, 0x86.toByte(), 0x48, 0x86.toByte(), 0xf7.toByte(), 0x0d, 1, 1, 0x0b)), der(0x05))
        val subject = X500Principal("CN=InstallerX test $name").encoded
        val validity = der(0x30, der(0x17, "000101000000Z".toByteArray()), der(0x18, "21000101000000Z".toByteArray()))
        // A v1 self-signed X.509 certificate needs no extensions or provider-specific APIs.
        val body = der(0x30, der(0x02, byteArrayOf(1)), algorithm, subject, validity, subject, keys.public.encoded)
        val signature = Signature.getInstance("SHA256withRSA").apply {
            initSign(keys.private)
            update(body)
        }.sign()
        val encoded = der(0x30, body, algorithm, der(0x03, byteArrayOf(0), signature))
        val certificate = CertificateFactory.getInstance("X.509")
            .generateCertificate(encoded.inputStream()) as X509Certificate
        return ApkSigner.SignerConfig.Builder(name, KeyConfig.Jca(keys.private), listOf(certificate)).build()
    }

    private fun der(tag: Int, vararg fields: ByteArray): ByteArray {
        val content = fields.fold(byteArrayOf()) { result, field -> result + field }
        val length = if (content.size < 128) {
            byteArrayOf(content.size.toByte())
        } else {
            val bytes = generateSequence(content.size) { it ushr 8 }.takeWhile { it > 0 }
                .map { it.toByte() }.toList().reversed().toByteArray()
            byteArrayOf((0x80 or bytes.size).toByte()) + bytes
        }
        return byteArrayOf(tag.toByte()) + length + content
    }

    /** Serializes a minimal binary Android XML document containing an empty manifest element. */
    private fun manifest(): ByteArray {
        val name = "manifest".toByteArray(Charsets.UTF_16LE)
        val strings = buffer(52)
            .putShort(0x0001).putShort(28).putInt(52) // String pool chunk.
            .putInt(1).putInt(0).putInt(0).putInt(32).putInt(0)
            .putInt(0).putShort(8).put(name).putShort(0).array()
        val start = buffer(36)
            .putShort(0x0102).putShort(16).putInt(36)
            .putInt(1).putInt(-1).putInt(-1).putInt(0)
            .putShort(20).putShort(20).putShort(0).putShort(0).putShort(0).putShort(0).array()
        val end = buffer(24)
            .putShort(0x0103).putShort(16).putInt(24)
            .putInt(1).putInt(-1).putInt(-1).putInt(0).array()
        return buffer(8).putShort(0x0003).putShort(8)
            .putInt(8 + strings.size + start.size + end.size).array() + strings + start + end
    }

    /** APK v2 digests exclude the signing block, so these changes preserve the app signature. */
    private fun alterStamp(apk: File, stamp: Stamp) {
        val bytes = apk.readBytes()
        val data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val centralDirectory = data.getInt(bytes.size - 22 + 16) // Generated ZIPs have no comment.
        var offset = centralDirectory - data.getLong(centralDirectory - 24).toInt()
        while (offset < centralDirectory - 24) {
            val size = data.getLong(offset).toInt()
            if (data.getInt(offset + 8) == 0x6dff800d) {
                if (stamp == Stamp.MISSING_BLOCK) {
                    // Replace only the block ID with an unknown ID, retaining the ZIP declaration.
                    data.putInt(offset + 8, 0x53544d50)
                } else {
                    val certificateSize = data.getInt(offset + 16)
                    val lastCertificateByte = offset + 20 + certificateSize - 1
                    // Change the certificate's signature without damaging its DER structure.
                    bytes[lastCertificateByte] = (bytes[lastCertificateByte].toInt() xor 1).toByte()
                }
                apk.writeBytes(bytes)
                return
            }
            offset += 8 + size
        }
        error("Generated APK has no source stamp block")
    }

    private fun buffer(size: Int): ByteBuffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)
}
