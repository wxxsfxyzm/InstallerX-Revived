// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.executor

import com.rosan.installer.domain.engine.exception.ModuleInstallException
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.source.DataEntity
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ModuleInstallerUtilsTest {
    private lateinit var tempDirectory: File

    @BeforeTest
    fun setUp() {
        tempDirectory = Files.createTempDirectory("module-installer-utils-test").toFile()
    }

    @AfterTest
    fun tearDown() {
        tempDirectory.deleteRecursively()
    }

    @Test
    fun `uses the materialized backing path instead of the original source identity`() {
        val localFile = File(tempDirectory, "module.zip").apply { writeText("module") }
        val data = DataEntity.FileEntity(localFile.absolutePath).apply {
            source = DataEntity.FileEntity("/smb%3A%2F%2Fserver%2Fmodule.zip")
        }

        val path = ModuleInstallerUtils.getModulePathOrThrow(module(data))

        assertEquals(localFile.absolutePath, path)
    }

    @Test
    fun `rejects a descriptor display path that was not materialized`() {
        val backingFile = File(tempDirectory, "backing.zip").apply { writeText("module") }
        val displayPath = "/smb%3A%2F%2Fserver%2Fmodule.zip"
        val data = DataEntity.FileDescriptorEntity(
            path = displayPath,
            startOffset = 0L,
            length = backingFile.length(),
            channelFactory = {
                FileChannel.open(backingFile.toPath(), StandardOpenOption.READ)
            },
            descriptorFactory = { error("The path validation test must not request an fd") }
        )

        assertFailsWith<ModuleInstallException> {
            ModuleInstallerUtils.getModulePathOrThrow(module(data))
        }
    }

    private fun module(data: DataEntity): AppEntity.ModuleEntity =
        AppEntity.ModuleEntity(
            id = "test",
            name = "Test",
            version = "1",
            versionCode = 1,
            author = "Tester",
            description = "",
            data = data
        )
}
