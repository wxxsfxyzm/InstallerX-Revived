// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.settings.mapper

import com.rosan.installer.domain.settings.model.preferences.theme.UiFamily
import kotlin.test.Test
import kotlin.test.assertEquals

class UiFamilyMapperTest {
    @Test
    fun storedFamilyTakesPrecedenceOverLegacyKey() {
        assertEquals(UiFamily.MATERIAL3, resolveUiFamily(storedFamily = "MATERIAL3", legacyUseMiuix = true))
        assertEquals(UiFamily.MIUIX, resolveUiFamily(storedFamily = "MIUIX", legacyUseMiuix = false))
    }

    @Test
    fun legacyMiuixKeySelectsMiuixWhenFamilyIsMissing() {
        assertEquals(UiFamily.MIUIX, resolveUiFamily(storedFamily = null, legacyUseMiuix = true))
    }

    @Test
    fun legacyMaterialKeySelectsMaterial3WhenFamilyIsMissing() {
        assertEquals(UiFamily.MATERIAL3, resolveUiFamily(storedFamily = null, legacyUseMiuix = false))
    }

    @Test
    fun missingKeysSelectMaterial3() {
        assertEquals(UiFamily.MATERIAL3, resolveUiFamily(storedFamily = null, legacyUseMiuix = null))
    }

    @Test
    fun unknownFamilyFallsBackToLegacyKey() {
        assertEquals(UiFamily.MIUIX, resolveUiFamily(storedFamily = "COLOROS", legacyUseMiuix = true))
        assertEquals(UiFamily.MATERIAL3, resolveUiFamily(storedFamily = "COLOROS", legacyUseMiuix = null))
    }

    @Test
    fun legacyKeyMirrorsMiuixSelection() {
        assertEquals(true, UiFamily.MIUIX.toLegacyUseMiuix())
        assertEquals(false, UiFamily.MATERIAL3.toLegacyUseMiuix())
    }
}
