// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.parser

import com.rosan.installer.core.reflection.ReflectionProviderImpl
import com.rosan.installer.data.engine.parser.strategy.ApkmStrategy
import com.rosan.installer.data.engine.parser.strategy.XApkStrategy
import com.rosan.installer.data.engine.signature.ApkSourceStampReader
import com.rosan.installer.data.engine.signature.CertificateFormatter
import com.rosan.installer.data.engine.signature.LightweightApkSignatureReader
import com.rosan.installer.data.engine.signature.PendingApkSignatureAnalyzer
import com.rosan.installer.domain.engine.exception.DescriptorAnalysisUnsupportedException
import com.rosan.installer.domain.engine.model.AnalyseExtraEntity
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.engine.model.source.DataType
import com.rosan.installer.domain.settings.model.config.ConfigModel
import com.rosan.installer.domain.settings.model.preferences.NetworkAppNameSource
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class ContainerIconFallbackTest {
    private val provider = UnifiedZipFileProvider(CommonsZipFileProvider(), SeekableZipReader())
    private val formatter = CertificateFormatter()
    private val stampReader = ApkSourceStampReader(CommonsZipFileProvider(), formatter)
    private val signatures = PendingApkSignatureAnalyzer(formatter, LightweightApkSignatureReader(formatter, stampReader), stampReader)
    private val parser = ApkParser(ReflectionProviderImpl(), signatures, provider, XposedModuleParser())

    @Test
    fun `missing XAPK icon requests base resources even with a JSON label and server filename`() = runBlocking {
        verifyFallback(DataType.XAPK)
    }

    @Test
    fun `missing APKM icon requests base resources even with a JSON label and server filename`() = runBlocking {
        verifyFallback(DataType.APKM)
    }

    private suspend fun verifyFallback(type: DataType) {
        val directory = Files.createTempDirectory("container-icon").toFile()
        try {
            val file = File(directory, "bundle.zip")
            val metadata = if (type == DataType.XAPK) {
                """{"package_name":"pkg","version_code":1,"name":"JSON label","split_apks":[{"file":"base.apk","id":"base"}]}"""
            } else {
                """{"pname":"pkg","versioncode":"1","app_name":"JSON label","release_version":null,"apk_title":null,"release_title":null}"""
            }
            ZipOutputStream(file.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry(if (type == DataType.XAPK) "manifest.json" else "info.json"))
                zip.write(metadata.toByteArray())
                zip.closeEntry()
                // A compressed base requires the established cached-source fallback for resource loading.
                zip.putNextEntry(ZipEntry("base.apk"))
                zip.write("base payload must not be read when icons are disabled".toByteArray())
                zip.closeEntry()
            }
            val strategy = if (type == DataType.XAPK) XApkStrategy(Json.Default, parser, signatures) else ApkmStrategy(json = Json.Default, pendingApkSignatureAnalyzer = signatures, apkParser = parser)
            for (loadIcon in listOf(false, true)) {
                val source = DataEntity.FileDescriptorEntity(
                    path = "https://example.com/bundle.zip",
                    startOffset = 0,
                    length = file.length(),
                    channelFactory = { FileChannel.open(file.toPath(), StandardOpenOption.READ) },
                    descriptorFactory = { error("Compressed base must request cached fallback before opening resources") },
                    preInstallIconAnalysis = loadIcon,
                    isNetworkSource = true,
                    networkAppNameSource = NetworkAppNameSource.ServerFilename,
                )
                provider.open(source).use { archive ->
                    val extra = prepareStreamingContainerAnalysis(
                        source,
                        type,
                        AnalyseExtraEntity(directory.path, dataType = type, checkAppSignature = true),
                    )
                    if (loadIcon) {
                        val error = assertFailsWith<DescriptorAnalysisUnsupportedException> {
                            strategy.analyze(ConfigModel.default, source, archive, extra)
                        }
                        assertEquals("Loading resources from this base APK requires a cached source", error.cause?.message)
                    } else {
                        val base = assertIs<AppEntity.BaseEntity>(strategy.analyze(ConfigModel.default, source, archive, extra).single())
                        assertEquals("JSON label", base.label)
                        assertNull(base.icon)
                    }
                }
            }
        } finally {
            directory.deleteRecursively()
        }
    }
}
