// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.session.util

import com.rosan.installer.domain.engine.model.install.hasZipSource
import com.rosan.installer.domain.engine.model.source.DataEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SourcePathUtilTest {
    @Test
    fun `material files SMB path is decoded once for display`() {
        val encoded =
            "/smb%3A%2F%2FAdministrator%40192.168.31.3%2Fshare%2Fapp-Preview-debug.apk"

        assertEquals(
            "smb://Administrator@192.168.31.3/share/app-Preview-debug.apk",
            encoded.decodeSmbProviderPath(),
        )
    }

    @Test
    fun `literal plus signs in SMB path are preserved`() {
        assertEquals(
            "smb://server/My+Apps/app.apk",
            "/smb%3A%2F%2Fserver%2FMy+Apps%2Fapp.apk".decodeSmbProviderPath(),
        )
    }

    @Test
    fun `non SMB provider path is not decoded`() {
        assertEquals(
            "/primary/My%20Apps/app.apk",
            "/primary/My%20Apps/app.apk".decodeSmbProviderPath(),
        )
    }

    @Test
    fun `shared storage alias normalizes for privileged consumers`() {
        assertEquals(
            "/storage/emulated/0/Download/app.apk",
            "/mnt/user/0/emulated/0/Download/app.apk".pathUnify(),
        )
        assertEquals(
            "/storage/emulated/0/Download/app.apk",
            "/mnt/pass_through/0/emulated/0/Download/app.apk".pathUnify(),
        )
    }

    @Test
    fun `canonical emulated storage path remains unchanged`() {
        val path = "/storage/emulated/0/Download/app.apk"

        assertEquals(path, path.pathUnify())
        assertEquals(path, path.resolveDirectFilePath().filePath)
    }

    @Test
    fun `obtainium external app data stays descriptor backed`() {
        val contentUri =
            "content://pl.solidexplorer2.files/storage/6331-6435/Android/data/" +
                "dev.imranr.obtainium/cache/app.apk"
        val readLink = "/mnt/user/0/emulated/0/Android/data/dev.imranr.obtainium/cache/app.apk"
        val resolved = readLink.resolveProviderBackedPath("/external_files/app.apk")

        assertEquals(
            "/storage/emulated/0/Android/data/dev.imranr.obtainium/cache/app.apk",
            resolved.displayPath,
        )
        assertNull(resolved.filePath)
        assertEquals("app.apk", resolved.descriptorEntityPath(contentUri))
        assertEquals(contentUri, resolved.sourceIdentityPath(contentUri))
        assertFalse(resolved.descriptorEntityPath(contentUri).contains("/Android/data"))

        val sdCard = "/storage/6331-6435/Android/data/dev.imranr.obtainium/cache/app.apk"
        val fromProvider = "/mnt/appfuse/1000/42".resolveProviderBackedPath(sdCard)
        assertEquals(sdCard, fromProvider.displayPath)
        assertNull(fromProvider.filePath)
        assertEquals("app.apk", fromProvider.descriptorEntityPath(contentUri))
    }

    @Test
    fun `virtual provider and smb paths are not local files`() {
        val smb = "/mnt/appfuse/1/2".resolveProviderBackedPath(
            "/smb%3A%2F%2FAdministrator%40192.168.31.3%2Fshare%2Fapp.apk",
        )
        assertEquals("smb://Administrator@192.168.31.3/share/app.apk", smb.displayPath)
        assertNull(smb.filePath)
        assertEquals(smb.displayPath, smb.sourceIdentityPath("content://files/app.apk"))

        val virtual = "/mnt/appfuse/1/2".resolveProviderBackedPath("/primary/My%20Apps/app.apk")
        assertEquals("/primary/My%20Apps/app.apk", virtual.displayPath)
        assertNull(virtual.filePath)
        assertEquals(virtual.displayPath, virtual.descriptorEntityPath("content://files/app.apk"))
    }

    @Test
    fun `direct file fallback normalizes aliases and keeps other paths`() {
        assertEquals(
            "/storage/emulated/0/Download/app.apk",
            "/mnt/user/0/emulated/0/Download/app.apk".resolveDirectFilePath().filePath,
        )
        val canonical = "/storage/emulated/0/Download/app.apk"
        assertEquals(canonical, canonical.resolveDirectFilePath().filePath)
        val internal = "/data/local/tmp/app.apk"
        assertEquals(internal, internal.resolveDirectFilePath().filePath)
        val scoped = "/storage/emulated/0/Android/data/dev.imranr.obtainium/cache/app.apk"
        assertEquals(scoped, scoped.resolveDirectFilePath().displayPath)
    }

    @Test
    fun `inaccessible provider paths do not become file paths`() {
        assertNull(providerFileFallbackPath(null) { true })
        assertNull(providerFileFallbackPath("  ") { true })
        assertNull(providerFileFallbackPath("/primary/My%20Apps/app.apk") { true })
        assertNull(
            providerFileFallbackPath("/storage/emulated/0/Download/../Android/data/dev.imranr.obtainium/cache/app.apk") {
                true
            },
        )

        val scoped = "/storage/6331-6435/Android/data/dev.imranr.obtainium/cache/app.apk"
        var scopedChecks = 0
        assertNull(
            providerFileFallbackPath(scoped) {
                scopedChecks += 1
                true
            },
        )
        assertEquals(0, scopedChecks)

        val missing = "/mnt/user/0/emulated/0/Download/app.apk"
        assertNull(providerFileFallbackPath(missing) { false })
        assertEquals(
            "/storage/emulated/0/Download/app.apk",
            providerFileFallbackPath(missing) { true },
        )
        assertNull(providerFileFallbackPath("/storage/emulated/0/Download/app.apk") { false })
    }

    @Test
    fun `scoped zip keeps its extension when the source identity is an opaque uri`() {
        val contentUri = "content://provider/document/1234"
        val resolved = ResolvedSourcePath(
            displayPath = "/storage/emulated/0/Android/data/pkg/files/module.zip",
            filePath = null,
        )
        val entity = DataEntity.FileEntity("/cache/descriptor").apply {
            source = DataEntity.FileEntity(resolved.sourceIdentityPath(contentUri))
            sourceDisplayPath = resolved.descriptorEntityPath(contentUri)
        }

        assertEquals(contentUri, resolved.sourceIdentityPath(contentUri))
        assertTrue(entity.hasZipSource())
    }

    @Test
    fun `opaque uri with an apk display path is not a zip source`() {
        val entity = DataEntity.FileEntity("/cache/descriptor").apply {
            source = DataEntity.FileEntity("content://provider/document/1234")
            sourceDisplayPath = "base.apk"
        }

        assertFalse(entity.hasZipSource())
    }
}
