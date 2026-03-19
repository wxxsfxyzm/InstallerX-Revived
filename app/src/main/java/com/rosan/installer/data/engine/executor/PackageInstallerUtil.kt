// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.executor

import android.content.pm.PackageInstaller
import com.rosan.installer.core.reflection.ReflectionProvider
import com.rosan.installer.core.reflection.getValue
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object PackageInstallerUtil : KoinComponent {
    private val reflect by inject<ReflectionProvider>()

    var PackageInstaller.SessionParams.installFlags: Int
        get() = reflect.getValue(this, "installFlags") ?: 0
        set(value) = reflect.setFieldValue(this, "installFlags", PackageInstaller.SessionParams::class.java, value)

    var PackageInstaller.SessionParams.abiOverride: String?
        get() = reflect.getValue(this, "abiOverride")
        set(value) = reflect.setFieldValue(this, "abiOverride", PackageInstaller.SessionParams::class.java, value)
}