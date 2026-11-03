// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.session.resolver

import com.rosan.installer.domain.engine.model.source.AnalysisMaterializationPolicy
import com.rosan.installer.domain.settings.model.config.NetworkSourceMode

internal sealed interface RemotePackageProbeResult {
    data object SingleApk : RemotePackageProbeResult
    data object SplitArchive : RemotePackageProbeResult
    data object PackageArchive : RemotePackageProbeResult
    data class StreamingUnsupported(val reason: StreamingUnsupportedReason) : RemotePackageProbeResult
    data class ProbeFailed(val cause: Throwable) : RemotePackageProbeResult
}

internal enum class StreamingUnsupportedReason {
    Platform,
    RangeSource,
    StableIdentity,
}

internal enum class RemotePackageAction {
    Stream,
    FullDownload,
    Reject,
}

/** Inspect directory names only; unknown/batch/module ZIPs retain the download path. */
internal fun classifyRemotePackageEntries(entries: Sequence<String>): RemotePackageProbeResult {
    val names = entries.toSet()
    if ("module.prop" in names || "common/module.prop" in names) return RemotePackageProbeResult.PackageArchive
    if ("AndroidManifest.xml" in names) return RemotePackageProbeResult.SingleApk
    val hasBase = names.any {
        val name = it.substringAfterLast('/')
        name.equals("base.apk", true) || name.equals("base-master.apk", true)
    }
    return if ("info.json" in names || "manifest.json" in names || "toc.pb" in names || hasBase) {
        RemotePackageProbeResult.SplitArchive
    } else {
        RemotePackageProbeResult.PackageArchive
    }
}

internal fun NetworkSourceMode.streamingAnalysisMaterializationPolicy(): AnalysisMaterializationPolicy = when (this) {
    NetworkSourceMode.Automatic -> AnalysisMaterializationPolicy.RETAINED_SOURCE_REPLACEMENT
    NetworkSourceMode.StreamOnly -> AnalysisMaterializationPolicy.DISALLOW
    NetworkSourceMode.Cache -> error("Cache mode does not create remote sources")
}

internal fun NetworkSourceMode.actionFor(result: RemotePackageProbeResult): RemotePackageAction = when (this) {
    NetworkSourceMode.Cache -> RemotePackageAction.FullDownload

    NetworkSourceMode.Automatic -> when (result) {
        RemotePackageProbeResult.SingleApk,
        RemotePackageProbeResult.SplitArchive,
        -> RemotePackageAction.Stream

        RemotePackageProbeResult.PackageArchive,
        is RemotePackageProbeResult.StreamingUnsupported,
        is RemotePackageProbeResult.ProbeFailed,
        -> RemotePackageAction.FullDownload
    }

    NetworkSourceMode.StreamOnly -> when (result) {
        RemotePackageProbeResult.SingleApk,
        RemotePackageProbeResult.SplitArchive,
        -> RemotePackageAction.Stream

        RemotePackageProbeResult.PackageArchive -> RemotePackageAction.FullDownload

        is RemotePackageProbeResult.StreamingUnsupported,
        is RemotePackageProbeResult.ProbeFailed,
        -> RemotePackageAction.Reject
    }
}
