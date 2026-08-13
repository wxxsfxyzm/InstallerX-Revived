// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.session.resolver

import com.rosan.installer.domain.engine.model.source.AnalysisMaterializationPolicy
import com.rosan.installer.domain.settings.model.config.NetworkSourceMode

internal sealed interface RemotePackageProbeResult {
    data object SingleApk : RemotePackageProbeResult
    data object PackageArchive : RemotePackageProbeResult
    data class StreamingUnsupported(val reason: StreamingUnsupportedReason) : RemotePackageProbeResult
    data class ProbeFailed(val cause: Throwable) : RemotePackageProbeResult
}

internal enum class StreamingUnsupportedReason {
    Platform,
    RangeSource,
    StableIdentity
}

internal enum class RemotePackageAction {
    Stream,
    FullDownload,
    Reject
}

internal fun NetworkSourceMode.streamingAnalysisMaterializationPolicy(): AnalysisMaterializationPolicy =
    when (this) {
        NetworkSourceMode.Smart -> AnalysisMaterializationPolicy.RETAINED_SOURCE_REPLACEMENT
        NetworkSourceMode.LowStorage -> AnalysisMaterializationPolicy.DISALLOW
        NetworkSourceMode.Cache -> error("Cache mode does not create remote sources")
    }

internal fun NetworkSourceMode.actionFor(result: RemotePackageProbeResult): RemotePackageAction =
    when (this) {
        NetworkSourceMode.Cache -> RemotePackageAction.FullDownload
        NetworkSourceMode.Smart -> when (result) {
            RemotePackageProbeResult.SingleApk -> RemotePackageAction.Stream
            RemotePackageProbeResult.PackageArchive,
            is RemotePackageProbeResult.StreamingUnsupported,
            is RemotePackageProbeResult.ProbeFailed -> RemotePackageAction.FullDownload
        }

        NetworkSourceMode.LowStorage -> when (result) {
            RemotePackageProbeResult.SingleApk -> RemotePackageAction.Stream
            RemotePackageProbeResult.PackageArchive -> RemotePackageAction.FullDownload
            is RemotePackageProbeResult.StreamingUnsupported,
            is RemotePackageProbeResult.ProbeFailed -> RemotePackageAction.Reject
        }
    }
