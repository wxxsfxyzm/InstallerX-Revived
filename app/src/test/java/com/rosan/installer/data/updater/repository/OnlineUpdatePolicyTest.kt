// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.updater.repository

import com.rosan.installer.core.device.model.Level
import com.rosan.installer.data.updater.model.GithubAsset
import com.rosan.installer.data.updater.model.GithubRelease
import com.rosan.installer.domain.settings.model.preferences.GithubUpdateChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class OnlineUpdatePolicyTest {
    @Test
    fun `update checks require network release build and official package`() {
        assertTrue(canCheck(level = Level.PREVIEW))
        assertTrue(canCheck(level = Level.STABLE))
        assertFalse(canCheck(level = Level.UNSTABLE))
        assertFalse(canCheck(level = Level.PREVIEW, allowInternetAccess = false))
        assertFalse(canCheck(level = Level.PREVIEW, isDebug = true))
        assertFalse(canCheck(level = Level.PREVIEW, packageName = "com.example.fork"))
    }

    @Test
    fun `preview selection uses the first prerelease returned by GitHub`() {
        val stable = release(tag = "26.08", prerelease = false)
        val newestPreview = release(tag = "26.08.abcdef0", prerelease = true)
        val olderPreview = release(tag = "26.07.1234567", prerelease = true)

        assertSame(
            newestPreview,
            OnlineUpdatePolicy.selectPreviewRelease(listOf(stable, newestPreview, olderPreview)),
        )
        assertNull(OnlineUpdatePolicy.selectPreviewRelease(listOf(stable)))
    }

    @Test
    fun `GitHub release payload exposes the CI APK`() {
        val payload = """
            [
              {
                "tag_name": "26.08.abcdef0",
                "prerelease": true,
                "html_url": "https://github.com/example/releases/tag/26.08.abcdef0",
                "assets": [
                  {
                    "name": "InstallerX-Revived-online-26.08.abcdef0.apk",
                    "browser_download_url": "https://github.com/example/releases/download/26.08.abcdef0/app.apk"
                  }
                ]
              }
            ]
        """.trimIndent()

        val releases = Json.decodeFromString<List<GithubRelease>>(payload)
        val release = OnlineUpdatePolicy.selectPreviewRelease(releases)
        val apkAsset = release?.let(OnlineUpdatePolicy::selectOnlineApkAsset)

        assertEquals("26.08.abcdef0", release?.tagName)
        assertEquals("InstallerX-Revived-online-26.08.abcdef0.apk", apkAsset?.name)
    }

    @Test
    fun `unified release keeps the online APK asset contract`() {
        val offline = asset("InstallerX-Revived-offline-26.08.abcdef0.apk")
        val sourceArchive = asset("source.zip")
        val online = asset("InstallerX-Revived-online-26.08.abcdef0.APK")
        val release = release(assets = listOf(offline, sourceArchive, online))

        assertSame(online, OnlineUpdatePolicy.selectOnlineApkAsset(release))
    }

    @Test
    fun `remote version is parsed from CI asset and falls back to tag`() {
        val release = release(tag = "v26.08.1234567")
        val asset = asset("InstallerX-Revived-online-26.08.abcdef0.apk")

        assertEquals("26.08.abcdef0", OnlineUpdatePolicy.resolveRemoteVersion(release, asset))
        assertEquals("26.08.1234567", OnlineUpdatePolicy.resolveRemoteVersion(release, null))
    }

    @Test
    fun `download URL supports official and proxy channels`() {
        val asset = asset(
            name = "InstallerX-Revived-online-26.08.abcdef0.apk",
            url = "https://github.com/example/release.apk",
        )

        assertEquals(
            asset.browserDownloadUrl,
            OnlineUpdatePolicy.resolveDownloadUrl(asset, GithubUpdateChannel.OFFICIAL, ""),
        )
        assertEquals(
            "https://gh.sevencdn.com/${asset.browserDownloadUrl}",
            OnlineUpdatePolicy.resolveDownloadUrl(asset, GithubUpdateChannel.PROXY_7ED, ""),
        )
        assertEquals(
            "https://proxy.example/${asset.browserDownloadUrl}",
            OnlineUpdatePolicy.resolveDownloadUrl(
                asset,
                GithubUpdateChannel.CUSTOM,
                "https://proxy.example/",
            ),
        )
        assertEquals(
            "",
            OnlineUpdatePolicy.resolveDownloadUrl(null, GithubUpdateChannel.OFFICIAL, ""),
        )
    }

    @Test
    fun `version comparison preserves release ordering rules`() {
        assertTrue(OnlineUpdatePolicy.compareVersions("26.09.abcdef0", "26.08.1234567") > 0)
        assertTrue(OnlineUpdatePolicy.compareVersions("26.08.01", "26.08") > 0)
        assertTrue(OnlineUpdatePolicy.compareVersions("26.08.abcdef0", "26.08") > 0)
        assertTrue(OnlineUpdatePolicy.compareVersions("26.08", "26.08.abcdef0") < 0)
        assertTrue(OnlineUpdatePolicy.compareVersions("26.08.abcdef0", "26.08.1234567") > 0)
        assertEquals(0, OnlineUpdatePolicy.compareVersions("26.08.abcdef0", "26.08.abcdef0"))
    }

    private fun canCheck(
        level: Level,
        allowInternetAccess: Boolean = true,
        isDebug: Boolean = false,
        packageName: String = OFFICIAL_PACKAGE,
    ): Boolean = OnlineUpdatePolicy.canCheckUpdates(
        allowInternetAccess = allowInternetAccess,
        isDebug = isDebug,
        level = level,
        packageName = packageName,
        officialPackageName = OFFICIAL_PACKAGE,
    )

    private fun release(tag: String = "26.08.abcdef0", prerelease: Boolean = true, assets: List<GithubAsset> = emptyList()) = GithubRelease(
        tagName = tag,
        isPrerelease = prerelease,
        assets = assets,
    )

    private fun asset(name: String, url: String = "https://github.com/example/$name") = GithubAsset(name = name, browserDownloadUrl = url)

    private companion object {
        const val OFFICIAL_PACKAGE = "com.rosan.installer.x.revived"
    }
}
