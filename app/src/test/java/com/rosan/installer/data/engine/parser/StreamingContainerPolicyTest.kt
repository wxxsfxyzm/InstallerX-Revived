// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.parser

import com.rosan.installer.domain.engine.exception.DescriptorAnalysisUnsupportedException
import com.rosan.installer.domain.engine.model.AnalyseExtraEntity
import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.engine.model.source.DataType
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame

class StreamingContainerPolicyTest {
    private val source = descriptor(network = true)
    private val extra = AnalyseExtraEntity(cacheDirectory = "unused")

    @Test
    fun `streaming splits skip full verification while the signature master switch is enabled`() {
        for (type in listOf(DataType.APKS, DataType.APKM, DataType.XAPK)) {
            val requested = extra.copy(dataType = type)
            val effective = prepareStreamingContainerAnalysis(source, type, requested)
            assertFalse(effective.checkAppSignature)
            assertFalse(prepareStreamingContainerAnalysis(source, type, extra).checkAppSignature)
        }
    }

    @Test
    fun `batch and module archives do not silently enter split streaming`() {
        for (type in listOf(DataType.MULTI_APK_ZIP, DataType.MODULE_ZIP, DataType.MIXED_MODULE_APK, DataType.MIXED_MODULE_ZIP)) {
            assertFailsWith<DescriptorAnalysisUnsupportedException> { prepareStreamingContainerAnalysis(source, type, extra) }
        }
    }

    @Test
    fun `local archives and the existing single APK path retain their policies`() {
        val verify = extra.copy(checkAppSignature = true)
        for (type in DataType.entries) {
            assertSame(verify, prepareStreamingContainerAnalysis(DataEntity.FileEntity("local.zip"), type, verify))
            assertSame(verify, prepareStreamingContainerAnalysis(descriptor(network = false), type, verify))
        }
        assertSame(verify, prepareStreamingContainerAnalysis(source, DataType.APK, verify))
    }

    private fun descriptor(network: Boolean) = DataEntity.FileDescriptorEntity(
        path = "https://example.com/app.apks",
        startOffset = 0,
        length = 128,
        channelFactory = { error("Policy checks must not read the source") },
        descriptorFactory = { error("Policy checks must not open an FD") },
        isNetworkSource = network,
    )
}
