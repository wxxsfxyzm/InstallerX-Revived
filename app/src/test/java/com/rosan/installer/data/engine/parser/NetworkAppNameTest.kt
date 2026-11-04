// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.parser

import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.settings.model.preferences.NetworkAppNameSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class NetworkAppNameTest {
    @Test
    fun `server name replaces the base label without changing payloads or split metadata`() {
        val data = source(NetworkAppNameSource.ServerFilename)
        val base = base(data)
        val split = AppEntity.SplitEntity("example.app", data, "config.en", "35", "26", null)
        val result = applyNetworkAppName(listOf(base, split), data)
        assertEquals("Server Name.apks", (result[0] as AppEntity.BaseEntity).label)
        assertSame(base.data, result[0].data)
        assertSame(split, result[1])
    }

    @Test
    fun `resource mode local sources and missing filenames retain the parsed label`() {
        for (data in listOf(
            source(NetworkAppNameSource.ApkResources),
            DataEntity.FileEntity("local.apk"),
            DataEntity.FileEntity("cached.apk", NetworkAppNameSource.ServerFilename),
        )) {
            val entities = listOf(base(data))
            assertSame(entities, applyNetworkAppName(entities, data))
            assertEquals("Resource Name", (entities[0]).label)
        }
    }

    @Test
    fun `batch archives keep distinct application names`() {
        val data = source(NetworkAppNameSource.ServerFilename)
        val entities = listOf(base(data), base(data).copy(packageName = "another.app", label = "Another App"))
        assertSame(entities, applyNetworkAppName(entities, data))
    }

    @Test
    fun `unknown persisted selections default to server filenames`() {
        assertEquals(NetworkAppNameSource.ServerFilename, NetworkAppNameSource.fromValue(null))
        assertEquals(NetworkAppNameSource.ServerFilename, NetworkAppNameSource.fromValue("unknown"))
        assertEquals(NetworkAppNameSource.ApkResources, NetworkAppNameSource.fromValue("apk_resources"))
    }

    private fun source(mode: NetworkAppNameSource) = DataEntity.FileEntity("cached.apk", mode, "Server Name.apks")

    private fun base(data: DataEntity) = AppEntity.BaseEntity(
        packageName = "example.app",
        sharedUserId = null,
        data = data,
        versionCode = 1,
        versionName = "1.0",
        label = "Resource Name",
        icon = null,
        targetSdk = "35",
        minSdk = "26",
    )
}
