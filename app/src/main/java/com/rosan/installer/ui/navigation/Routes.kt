// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.navigation

import android.os.Parcelable
import androidx.navigation3.runtime.NavKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation keys for Navigation3.
 * Each destination is a NavKey (data object/data class) and can be saved/restored in the back stack.
 */
sealed interface Route : NavKey, Parcelable {
    @Parcelize
    @Serializable
    data object About : Route

    @Parcelize
    @Serializable
    data object OpenSourceLicense : Route

    @Parcelize
    @Serializable
    data object Main : Route

    @Parcelize
    @Serializable
    data object Theme : Route

    @Parcelize
    @Serializable
    data object InstallerGlobal : Route

    @Parcelize
    @Serializable
    data object NotificationSettings : Route

    @Parcelize
    @Serializable
    data object UninstallerGlobal : Route

    @Parcelize
    @Serializable
    data object Lab : Route

    @Parcelize
    @Serializable
    data class EditConfig(
        val id: Long
    ) : Route

    @Parcelize
    @Serializable
    data class ApplyConfig(
        val id: Long
    ) : Route

}
