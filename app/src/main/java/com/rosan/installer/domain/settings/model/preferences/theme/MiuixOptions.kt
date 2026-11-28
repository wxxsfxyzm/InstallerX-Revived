// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.model.preferences.theme

/**
 * Options that only apply to the Miuix UI family.
 *
 * @property useMonet Whether Miuix generates its palette from the key color.
 * @property useOs4 Whether settings screens use the OS4 glass style when the device supports it.
 */
data class MiuixOptions(
    val useMonet: Boolean = false,
    val useOs4: Boolean = false,
)
