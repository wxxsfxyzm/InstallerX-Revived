// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.settings.mapper

import com.rosan.installer.domain.settings.model.preferences.theme.UiFamily

/**
 * Resolve the stored UI family.
 *
 * Older versions and their backups only store [legacyUseMiuix], so it is used when [storedFamily] is missing or unknown.
 */
internal fun resolveUiFamily(storedFamily: String?, legacyUseMiuix: Boolean?): UiFamily = UiFamily.fromValueOrNull(storedFamily)
    ?: if (legacyUseMiuix == true) UiFamily.MIUIX else UiFamily.MATERIAL3

/** The value written to the legacy Miuix key so older versions and their restores keep the same UI family. */
internal fun UiFamily.toLegacyUseMiuix(): Boolean = this == UiFamily.MIUIX
