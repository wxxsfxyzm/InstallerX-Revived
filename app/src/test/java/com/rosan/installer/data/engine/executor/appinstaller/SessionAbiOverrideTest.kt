// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.executor.appinstaller

import com.rosan.installer.core.device.model.Architecture
import com.rosan.installer.domain.engine.model.source.DataType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SessionAbiOverrideTest {
    @Test
    fun `leaves every architecture unset by default and when disabled`() {
        for (sourceType in DataType.entries) {
            for (architecture in Architecture.entries) {
                assertNull(resolveSessionAbiOverride(sourceType, architecture, Architecture.ARM64))
                assertNull(
                    resolveSessionAbiOverride(
                        sourceType,
                        architecture,
                        Architecture.ARM64,
                        enableAbiOverride = false,
                        tryDifferentArchitecture = true,
                    ),
                )
            }
        }
    }

    @Test
    fun `both opt ins preserve legacy concrete abis including translation candidates`() {
        val architectures = mapOf(
            Architecture.ARM64 to "arm64-v8a",
            Architecture.ARM to "armeabi-v7a",
            Architecture.ARMEABI to "armeabi",
            Architecture.X86 to "x86",
            Architecture.X86_64 to "x86_64",
            Architecture.MIPS to "mips",
            Architecture.MIPS64 to "mips64",
        )
        for ((architecture, abi) in architectures) {
            assertEquals(
                abi,
                resolveSessionAbiOverride(
                    DataType.APK,
                    architecture,
                    Architecture.ARM64,
                    enableAbiOverride = true,
                    tryDifferentArchitecture = true,
                ),
            )
        }
    }

    @Test
    fun `main opt in restores the arm32 fallback on arm devices`() {
        for (sourceType in listOf(DataType.APK, DataType.MULTI_APK, DataType.MULTI_APK_ZIP)) {
            assertEquals(
                "armeabi-v7a",
                resolveSessionAbiOverride(sourceType, Architecture.UNKNOWN, Architecture.ARM64, enableAbiOverride = true),
            )
            assertNull(resolveSessionAbiOverride(sourceType, null, Architecture.ARM64, enableAbiOverride = true))
            assertNull(resolveSessionAbiOverride(sourceType, Architecture.NONE, Architecture.ARM64, enableAbiOverride = true))
        }
    }

    @Test
    fun `both opt ins limit overrides to apk and batch apk sources`() {
        val eligibleSources = listOf(DataType.APK, DataType.MULTI_APK, DataType.MULTI_APK_ZIP)
        for (sourceType in eligibleSources) {
            assertEquals(
                "arm64-v8a",
                resolveSessionAbiOverride(
                    sourceType,
                    Architecture.ARM64,
                    Architecture.ARM64,
                    enableAbiOverride = true,
                    tryDifferentArchitecture = true,
                ),
            )
        }
        for (sourceType in DataType.entries - eligibleSources.toSet()) {
            assertNull(
                resolveSessionAbiOverride(
                    sourceType,
                    Architecture.ARM64,
                    Architecture.ARM64,
                    enableAbiOverride = true,
                    tryDifferentArchitecture = true,
                ),
            )
            assertNull(
                resolveSessionAbiOverride(
                    sourceType,
                    Architecture.UNKNOWN,
                    Architecture.ARM64,
                    enableAbiOverride = true,
                    tryDifferentArchitecture = true,
                ),
            )
        }
    }

    @Test
    fun `main opt in permits 32 and 64 bit overrides within the same family`() {
        val families = listOf(
            listOf(Architecture.ARMEABI, Architecture.ARM, Architecture.ARM64),
            listOf(Architecture.X86, Architecture.X86_64),
            listOf(Architecture.MIPS, Architecture.MIPS64),
        )
        for (family in families) {
            for (device in family) {
                for (app in family) {
                    assertEquals(
                        app.arch,
                        resolveSessionAbiOverride(DataType.APK, app, device, enableAbiOverride = true),
                    )
                }
            }
        }
    }

    @Test
    fun `different architecture is disabled by default for arm and x86 in both directions`() {
        val arm = listOf(Architecture.ARMEABI, Architecture.ARM, Architecture.ARM64)
        val x86 = listOf(Architecture.X86, Architecture.X86_64)
        for (armArchitecture in arm) {
            for (x86Architecture in x86) {
                assertNull(
                    resolveSessionAbiOverride(DataType.APK, armArchitecture, x86Architecture, enableAbiOverride = true),
                )
                assertNull(
                    resolveSessionAbiOverride(DataType.APK, x86Architecture, armArchitecture, enableAbiOverride = true),
                )
            }
        }
    }

    @Test
    fun `unknown fallback respects the different architecture opt in`() {
        for (device in listOf(Architecture.X86_64, Architecture.MIPS64, Architecture.UNKNOWN, Architecture.NONE)) {
            assertNull(resolveSessionAbiOverride(DataType.APK, Architecture.UNKNOWN, device, enableAbiOverride = true))
            assertNull(resolveSessionAbiOverride(DataType.APK, Architecture.ARM64, device, enableAbiOverride = true))
            assertEquals(
                "armeabi-v7a",
                resolveSessionAbiOverride(
                    DataType.APK,
                    Architecture.UNKNOWN,
                    device,
                    enableAbiOverride = true,
                    tryDifferentArchitecture = true,
                ),
            )
            assertNull(
                resolveSessionAbiOverride(
                    DataType.APK,
                    Architecture.NONE,
                    device,
                    enableAbiOverride = true,
                    tryDifferentArchitecture = true,
                ),
            )
        }
    }
}
