// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.navigation

import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.nav.core.NavKey

/**
 * Type-safe navigation keys for Navigation3.
 * Each destination is a NavKey (data object/data class) and can be saved/restored in the back stack.
 */
interface Route : NavKey {
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
    data object AuthorizerCust : Route

    @Serializable
    data class EditConfig(
        val id: Long
    ) : Route

    @Serializable
    data class ApplyConfig(
        val id: Long
    ) : Route
}
