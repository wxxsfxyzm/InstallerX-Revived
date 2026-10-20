// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.parser

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.android.apksig.ApkSigner
import com.android.apksig.ApkVerifier
import com.android.apksig.KeyConfig
import com.rosan.installer.BuildConfig
import com.rosan.installer.core.env.AppConfig
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.engine.exception.AnalyseException
import com.rosan.installer.domain.engine.model.AnalyseExtraEntity
import com.rosan.installer.domain.engine.model.error.AnalyseErrorType
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.source.DataEntity
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.security.auth.x500.X500Principal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Prepares ordinary InstallerX releases for an existing replacement system installer.
 *
 * The package rewrite is never applied on its own: the caller marks eligible APKs so the dialog can
 * offer it, and only an explicit user choice turns the APK into a system installer update.
 */
class SystemInstallerUpdatePreparer(
    private val context: Context,
    private val capabilityProvider: DeviceCapabilityProvider,
    private val apkParser: ApkParser,
) {
    val isReplacementSystemInstaller: Boolean
        get() = context.packageName == SYSTEM_INSTALLER_PACKAGE && capabilityProvider.isSystemApp

    // A persistent, non-exportable device key keeps subsequent rewritten updates consistent.
    private val signer by lazy {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!store.containsAlias(KEY_ALIAS)) {
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore").apply {
                initialize(
                    KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN)
                        .setKeySize(2048)
                        .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                        .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                        .setCertificateSubject(X500Principal("CN=InstallerX system installer update"))
                        .setCertificateSerialNumber(java.math.BigInteger.ONE)
                        .setCertificateNotAfter(Date(4102444800000L))
                        .build(),
                )
            }.generateKeyPair()
        }
        val entry = store.getEntry(KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
        ApkSigner.SignerConfig.Builder(
            "installerx",
            KeyConfig.Jca(entry.privateKey),
            entry.certificateChain.map { it as X509Certificate },
        ).build()
    }

    suspend fun prepare(entities: List<AppEntity>, extra: AnalyseExtraEntity): List<AppEntity> = withContext(Dispatchers.IO) {
        if (!isReplacementSystemInstaller) return@withContext entities
        val packages = entities.filterIsInstance<AppEntity.BaseEntity>()
            .map { it.packageName }
            .filter { it != SYSTEM_INSTALLER_PACKAGE && it in INSTALLERX_PACKAGES }
            .toSet()
        if (packages.isEmpty()) return@withContext entities

        // Keep the APK untouched by default and only advertise that a system installer update is possible.
        if (!extra.installAsSystemInstaller) {
            return@withContext entities.map { entity ->
                val base = entity as? AppEntity.BaseEntity
                if (base == null || base.packageName !in packages) entity
                else base.copy(canInstallAsSystemInstaller = true)
            }
        }

        try {
            entities.mapNotNull { entity ->
                if (entity.packageName !in packages || entity is AppEntity.ModuleEntity) return@mapNotNull entity
                // Dex metadata describes the original APK and is invalid after repackaging.
                if (entity is AppEntity.DexMetadataEntity) return@mapNotNull null
                currentCoroutineContext().ensureActive()
                val rewritten = rewriteApk(entity.data, entity.packageName, extra.cacheDirectory)
                val parsed = apkParser.parseFull(rewritten, extra.copy(dataType = entity.sourceType))
                    .singleOrNull()
                    ?: error("Cannot parse rewritten InstallerX APK")
                require(parsed.packageName == SYSTEM_INSTALLER_PACKAGE)
                when (parsed) {
                    is AppEntity.BaseEntity -> parsed.copy(
                        originalPackageName = entity.packageName,
                        canInstallAsSystemInstaller = true,
                    )

                    else -> parsed
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            throw AnalyseException(AnalyseErrorType.SYSTEM_INSTALLER_UPDATE_FAILED, cause = error)
        }
    }

    private suspend fun rewriteApk(data: DataEntity, packageName: String, cacheDirectory: String): DataEntity.FileEntity {
        val directory = File(cacheDirectory).apply { mkdirs() }
        val input = File.createTempFile("installerx-original-", ".apk", directory)
        val unsigned = File.createTempFile("installerx-unsigned-", ".apk", directory)
        val signed = File.createTempFile("installerx-system-", ".apk", directory)
        try {
            requireNotNull(data.getInstallInputStreamWhileNotEmpty()).use { source ->
                input.outputStream().use { source.copyCancellable(it) }
            }
            val verification = ApkVerifier.Builder(input)
                .setMinCheckedPlatformVersion(Build.VERSION.SDK_INT)
                .setMaxCheckedPlatformVersion(Build.VERSION.SDK_INT)
                .build().verify()
            require(verification.isVerified) { "Original InstallerX APK signature is invalid: ${verification.errors}" }

            ZipFile(input).use { source ->
                ZipOutputStream(unsigned.outputStream().buffered()).use { output ->
                    val entries = source.entries()
                    while (entries.hasMoreElements()) {
                        currentCoroutineContext().ensureActive()
                        val entry = entries.nextElement()
                        if (entry.isDirectory || entry.name.isSignatureEntry()) continue
                        if (entry.name == "AndroidManifest.xml") {
                            val manifest = source.getInputStream(entry).use { it.readBytes() }
                            output.putNextEntry(ZipEntry(entry.name))
                            output.write(ApkManifestPackageRewriter.rewrite(manifest, packageName, SYSTEM_INSTALLER_PACKAGE))
                        } else {
                            // Preserve STORED resources/native libraries; apksig restores ZIP alignment.
                            val copy = ZipEntry(entry.name).apply {
                                method = entry.method
                                if (method == ZipEntry.STORED) {
                                    size = entry.size
                                    compressedSize = entry.size
                                    crc = entry.crc
                                }
                            }
                            output.putNextEntry(copy)
                            source.getInputStream(entry).use { it.copyCancellable(output) }
                        }
                        output.closeEntry()
                    }
                }
            }
            currentCoroutineContext().ensureActive()
            ApkSigner.Builder(listOf(signer))
                .setInputApk(unsigned)
                .setOutputApk(signed)
                .setMinSdkVersion(26)
                .setV1SigningEnabled(false)
                .setV2SigningEnabled(true)
                .setV3SigningEnabled(true)
                .setV4SigningEnabled(false)
                .setOtherSignersSignaturesPreserved(false)
                .setAlignmentPreserved(false)
                .setLibraryPageAlignmentBytes(16384)
                .build().sign()
            currentCoroutineContext().ensureActive()
            return DataEntity.FileEntity(signed.path).apply { source = data.getSourceTop() }
        } catch (error: Throwable) {
            signed.delete()
            throw error
        } finally {
            input.delete()
            unsigned.delete()
        }
    }

    private suspend fun InputStream.copyCancellable(output: OutputStream) {
        val buffer = ByteArray(1024 * 1024)
        while (true) {
            currentCoroutineContext().ensureActive()
            val size = read(buffer)
            if (size < 0) break
            output.write(buffer, 0, size)
        }
    }

    private fun String.isSignatureEntry(): Boolean {
        val upper = uppercase(Locale.ROOT)
        return this == "stamp-cert-sha256" ||
            (
                upper.startsWith("META-INF/") &&
                    (upper == "META-INF/MANIFEST.MF" || upper.endsWith(".SF") || upper.endsWith(".RSA") || upper.endsWith(".DSA") || upper.endsWith(".EC"))
                )
    }

    private companion object {
        const val SYSTEM_INSTALLER_PACKAGE = "com.android.packageinstaller"
        const val KEY_ALIAS = "installerx_system_installer_update"
        val INSTALLERX_PACKAGES = setOf(AppConfig.OFFICIAL_PACKAGE_NAME, BuildConfig.APPLICATION_ID, "com.rosan.installer.x", "com.rosan.installer")
    }
}
