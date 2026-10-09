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
    fun `leaves unadvertised architectures unset`() {
        val arm64Only = listOf(Architecture.ARM64)
        val arm64AndArm = listOf(Architecture.ARM64, Architecture.ARM)

        assertNull(resolveSessionAbiOverride(DataType.APK, Architecture.X86_64, arm64Only))
        assertNull(resolveSessionAbiOverride(DataType.APK, Architecture.X86_64, arm64AndArm))
        assertNull(resolveSessionAbiOverride(DataType.APK, Architecture.ARM, arm64Only))
    }

    @Test
    fun `overrides advertised concrete architectures with their abi strings`() {
        assertEquals(
            "arm64-v8a",
            resolveSessionAbiOverride(DataType.APK, Architecture.ARM64, listOf(Architecture.ARM64)),
        )
        assertEquals(
            "x86_64",
            resolveSessionAbiOverride(DataType.APK, Architecture.X86_64, listOf(Architecture.X86_64)),
        )

        val translated = listOf(Architecture.X86_64, Architecture.ARM)
        assertEquals("x86_64", resolveSessionAbiOverride(DataType.APK, Architecture.X86_64, translated))
        assertEquals("armeabi-v7a", resolveSessionAbiOverride(DataType.APK, Architecture.ARM, translated))
    }

    @Test
    fun `leaves empty unknown none and missing base metadata unset`() {
        val malformed = listOf(Architecture.UNKNOWN, Architecture.NONE, Architecture.ARM64)

        assertNull(resolveSessionAbiOverride(DataType.APK, Architecture.ARM64, emptyList()))
        assertNull(resolveSessionAbiOverride(DataType.APK, null, listOf(Architecture.ARM64)))
        assertNull(resolveSessionAbiOverride(DataType.APK, Architecture.UNKNOWN, malformed))
        assertNull(resolveSessionAbiOverride(DataType.APK, Architecture.NONE, malformed))
        assertNull(resolveSessionAbiOverride(DataType.MULTI_APK, null, malformed))
    }

    @Test
    fun `limits overrides to apk and batch apk sources`() {
        val supported = listOf(Architecture.ARM64)

        assertEquals("arm64-v8a", resolveSessionAbiOverride(DataType.APK, Architecture.ARM64, supported))
        assertEquals("arm64-v8a", resolveSessionAbiOverride(DataType.MULTI_APK, Architecture.ARM64, supported))
        assertEquals("arm64-v8a", resolveSessionAbiOverride(DataType.MULTI_APK_ZIP, Architecture.ARM64, supported))
        assertNull(resolveSessionAbiOverride(DataType.APKS, Architecture.ARM64, supported))
        assertNull(resolveSessionAbiOverride(DataType.APKM, Architecture.ARM64, supported))
        assertNull(resolveSessionAbiOverride(DataType.XAPK, Architecture.ARM64, supported))
    }
}
