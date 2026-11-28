// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.family

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.domain.settings.model.preferences.ThemeState
import com.rosan.installer.domain.settings.model.preferences.theme.UiFamily
import com.rosan.installer.ui.feature.settings.SettingsSharedViewModel
import com.rosan.installer.ui.navigation.Route

/**
 * Entry points a UI family provides to the app.
 *
 * Add a UI family by implementing this interface and returning it from [host].
 * Shared view models and state live in `ui.feature`, so a family only supplies its theme and screens.
 */
@Stable
internal interface UiFamilyHost {
    /**
     * Create the family theme inside the shared installer theme.
     *
     * @param useAmoled Whether dark surfaces use pure black.
     */
    @Composable
    fun rememberPageTheme(useAmoled: Boolean): UiFamilyPageTheme

    /** The installer dialog or sheet for [session]. */
    @Composable
    fun InstallerPage(session: InstallerSessionRepository)

    /** The settings screen for [route]. */
    @Composable
    fun RouteContent(route: Route, uiState: ThemeState, sharedViewModel: SettingsSharedViewModel)
}

/**
 * A family theme created once above navigation and applied inside each navigation entry.
 *
 * @property backdropColor The color behind navigation transitions.
 * @property apply Wraps page content in the family theme.
 */
@Stable
internal class UiFamilyPageTheme(
    val backdropColor: Color,
    val apply: @Composable (content: @Composable () -> Unit) -> Unit,
)

internal fun UiFamily.host(): UiFamilyHost = when (this) {
    UiFamily.MATERIAL3 -> Material3FamilyHost
    UiFamily.MIUIX -> MiuixFamilyHost
}
