// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation keys for Navigation3.
 * Each destination is a NavKey (data object/data class) and can be saved/restored in the back stack.
 */
sealed interface Route : NavKey {
    @Serializable
    data object About : Route

    @Serializable
    data object OpenSourceLicense : Route

    @Serializable
    data object Main : Route

    @Serializable
    data object Theme : Route

    @Serializable
    data object InstallerGlobal : Route

    @Serializable
    data object DialogSettings : Route

    @Serializable
    data object NotificationSettings : Route

    @Serializable
    data object UninstallerGlobal : Route

    @Serializable
    data object Lab : Route

    @Serializable
    data object DefaultInstaller : Route

    @Serializable
    data object Priv : Route

    @Serializable
    data class EditConfig(
        val id: Long
    ) : Route

    @Serializable
    data class ApplyConfig(
        val id: Long
    ) : Route
}
