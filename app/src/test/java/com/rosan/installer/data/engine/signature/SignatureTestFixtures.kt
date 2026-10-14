// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.signature

import java.io.File

internal object SignatureTestFixtures {
    fun copy(name: String, directory: File): File = File(directory, name).also { target ->
        requireNotNull(javaClass.getResourceAsStream("/signature/$name")).use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
    }
}
