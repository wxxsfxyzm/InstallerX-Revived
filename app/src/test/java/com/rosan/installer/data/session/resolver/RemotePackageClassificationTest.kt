// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.session.resolver

import kotlin.test.Test
import kotlin.test.assertEquals

class RemotePackageClassificationTest {
    @Test
    fun `recognizes APKM XAPK and APKS markers without reading payloads`() {
        for (names in listOf(
            listOf("info.json", "base.apk"),
            listOf("manifest.json", "com.example.apk"),
            listOf("toc.pb", "splits/base-master.apk"),
            listOf("nested/BASE.APK", "nested/config.en.apk"),
        )) {
            assertEquals(RemotePackageProbeResult.SplitArchive, classifyRemotePackageEntries(names.asSequence()))
        }
    }

    @Test
    fun `root manifest takes priority over nested APKs and split markers`() {
        assertEquals(
            RemotePackageProbeResult.SingleApk,
            classifyRemotePackageEntries(sequenceOf("AndroidManifest.xml", "base.apk", "toc.pb")),
        )
    }

    @Test
    fun `module mixed batch and unknown archives retain the download path`() {
        for (names in listOf(
            listOf("module.prop", "base.apk"),
            listOf("common/module.prop", "AndroidManifest.xml"),
            listOf("app-one.apk", "app-two.apk"),
            listOf("readme.txt"),
            emptyList(),
        )) {
            assertEquals(RemotePackageProbeResult.PackageArchive, classifyRemotePackageEntries(names.asSequence()))
        }
    }
}
