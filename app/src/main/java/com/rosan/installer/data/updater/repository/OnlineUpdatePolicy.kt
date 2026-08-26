// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.updater.repository

import com.rosan.installer.core.device.model.Level
import com.rosan.installer.data.updater.model.GithubAsset
import com.rosan.installer.data.updater.model.GithubRelease
import com.rosan.installer.domain.settings.model.preferences.GithubUpdateChannel

internal object OnlineUpdatePolicy {
    private const val PROXY_7ED = "https://gh.sevencdn.com/"

    fun canCheckUpdates(
        allowInternetAccess: Boolean,
        isDebug: Boolean,
        level: Level,
        packageName: String,
        officialPackageName: String,
    ): Boolean = allowInternetAccess &&
        !isDebug &&
        level != Level.UNSTABLE &&
        packageName == officialPackageName

    fun selectPreviewRelease(releases: List<GithubRelease>): GithubRelease? = releases.firstOrNull(GithubRelease::isPrerelease)

    fun selectOnlineApkAsset(release: GithubRelease): GithubAsset? = release.assets.firstOrNull { asset ->
        asset.name.contains("online", ignoreCase = true) &&
            asset.name.endsWith(".apk", ignoreCase = true)
    }

    fun resolveDownloadUrl(asset: GithubAsset?, channel: GithubUpdateChannel, customProxyUrl: String): String {
        val browserDownloadUrl = asset?.browserDownloadUrl.orEmpty()
        if (browserDownloadUrl.isEmpty()) return ""

        val proxyUrl = when (channel) {
            GithubUpdateChannel.OFFICIAL -> ""
            GithubUpdateChannel.PROXY_7ED -> PROXY_7ED
            GithubUpdateChannel.CUSTOM -> customProxyUrl
        }
        if (proxyUrl.isEmpty()) return browserDownloadUrl

        return "${proxyUrl.trimEnd('/')}/$browserDownloadUrl"
    }

    fun resolveRemoteVersion(release: GithubRelease, asset: GithubAsset?): String {
        val versionFromAsset = asset?.name
            ?.let { Regex("-(\\d.+?)\\.apk", RegexOption.IGNORE_CASE).find(it) }
            ?.groupValues
            ?.get(1)

        return versionFromAsset ?: release.tagName.removePrefix("v")
    }

    fun compareVersions(first: String, second: String): Int {
        val (numericFirst, hashFirst) = splitVersion(first)
        val (numericSecond, hashSecond) = splitVersion(second)

        val numericComparison = compareNumericVersion(numericFirst, numericSecond)
        if (numericComparison != 0) return numericComparison

        return when {
            hashFirst != null && hashSecond != null -> if (hashFirst == hashSecond) 0 else 1
            hashFirst != null -> 1
            hashSecond != null -> -1
            else -> 0
        }
    }

    private fun splitVersion(version: String): Pair<String, String?> {
        val parts = version.split('.')
        val lastPart = parts.lastOrNull().orEmpty()
        val isHash = lastPart.length >= 7 && lastPart.matches(Regex("^[a-fA-F0-9]+$"))

        return if (isHash) {
            parts.dropLast(1).joinToString(".") to lastPart
        } else {
            version to null
        }
    }

    private fun compareNumericVersion(first: String, second: String): Int {
        val firstParts = first.split('.')
        val secondParts = second.split('.')

        for (index in 0 until maxOf(firstParts.size, secondParts.size)) {
            val firstPart = firstParts.getOrNull(index)?.toIntOrNull() ?: 0
            val secondPart = secondParts.getOrNull(index)?.toIntOrNull() ?: 0
            firstPart.compareTo(secondPart).takeIf { it != 0 }?.let { return it }
        }
        return 0
    }
}
