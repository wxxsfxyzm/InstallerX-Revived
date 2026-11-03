// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.session.resolver

import com.rosan.installer.domain.engine.model.source.AnalysisMaterializationPolicy
import com.rosan.installer.domain.settings.model.config.NetworkSourceMode
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals

class RemotePackageProbeResultTest {
    private val samples = listOf(
        RemotePackageProbeResult.SingleApk,
        RemotePackageProbeResult.SplitArchive,
        RemotePackageProbeResult.PackageArchive,
        RemotePackageProbeResult.StreamingUnsupported(StreamingUnsupportedReason.Platform),
        RemotePackageProbeResult.StreamingUnsupported(StreamingUnsupportedReason.RangeSource),
        RemotePackageProbeResult.StreamingUnsupported(StreamingUnsupportedReason.StableIdentity),
        RemotePackageProbeResult.ProbeFailed(IOException("probe failed")),
    )

    @Test
    fun `cache mode always downloads the complete source`() {
        samples.forEach { result ->
            assertEquals(RemotePackageAction.FullDownload, NetworkSourceMode.Cache.actionFor(result))
        }
    }

    @Test
    fun `smart mode streams APKs and split archives and downloads other outcomes`() {
        assertEquals(
            RemotePackageAction.Stream,
            NetworkSourceMode.Smart.actionFor(RemotePackageProbeResult.SingleApk),
        )
        assertEquals(RemotePackageAction.Stream, NetworkSourceMode.Smart.actionFor(RemotePackageProbeResult.SplitArchive))
        samples.filter { it != RemotePackageProbeResult.SingleApk && it != RemotePackageProbeResult.SplitArchive }.forEach { result ->
            assertEquals(RemotePackageAction.FullDownload, NetworkSourceMode.Smart.actionFor(result))
        }
    }

    @Test
    fun `low storage streams split archives but downloads other archives and rejects unavailable streaming`() {
        assertEquals(
            RemotePackageAction.Stream,
            NetworkSourceMode.LowStorage.actionFor(RemotePackageProbeResult.SingleApk),
        )
        assertEquals(RemotePackageAction.Stream, NetworkSourceMode.LowStorage.actionFor(RemotePackageProbeResult.SplitArchive))
        assertEquals(
            RemotePackageAction.FullDownload,
            NetworkSourceMode.LowStorage.actionFor(RemotePackageProbeResult.PackageArchive),
        )
        samples.filter {
            it is RemotePackageProbeResult.StreamingUnsupported || it is RemotePackageProbeResult.ProbeFailed
        }.forEach { result ->
            assertEquals(RemotePackageAction.Reject, NetworkSourceMode.LowStorage.actionFor(result))
        }
    }

    @Test
    fun `streaming analysis fallback follows the selected mode contract`() {
        assertEquals(
            AnalysisMaterializationPolicy.RETAINED_SOURCE_REPLACEMENT,
            NetworkSourceMode.Smart.streamingAnalysisMaterializationPolicy(),
        )
        assertEquals(
            AnalysisMaterializationPolicy.DISALLOW,
            NetworkSourceMode.LowStorage.streamingAnalysisMaterializationPolicy(),
        )
    }
}
