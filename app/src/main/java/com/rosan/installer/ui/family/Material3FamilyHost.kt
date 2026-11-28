// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.family

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.domain.settings.model.preferences.ThemeState
import com.rosan.installer.ui.feature.settings.SettingsSharedViewModel
import com.rosan.installer.ui.md3.theme.Material3InstallerTheme
import com.rosan.installer.ui.navigation.Material3MainPageWrapper
import com.rosan.installer.ui.navigation.Route
import com.rosan.installer.ui.page.main.settings.config.apply.ApplyPage
import com.rosan.installer.ui.page.main.settings.config.edit.EditPage
import com.rosan.installer.ui.page.main.settings.home.installer.DefaultInstallerPage
import com.rosan.installer.ui.page.main.settings.home.priv.PrivPage
import com.rosan.installer.ui.page.main.settings.preferred.about.AboutPage
import com.rosan.installer.ui.page.main.settings.preferred.about.OpenSourceLicensePage
import com.rosan.installer.ui.page.main.settings.preferred.installer.InstallerGlobalSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.installer.authorizer.AuthorizerCustPage
import com.rosan.installer.ui.page.main.settings.preferred.installer.dialog.DialogSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.installer.notification.NotificationSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.lab.LabPage
import com.rosan.installer.ui.page.main.settings.preferred.network.NetworkPage
import com.rosan.installer.ui.page.main.settings.preferred.theme.ThemeSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.uninstaller.UninstallerGlobalSettingsPage
import com.rosan.installer.ui.theme.InstallerTheme

internal object Material3FamilyHost : UiFamilyHost {
    @Composable
    override fun rememberPageTheme(useAmoled: Boolean): UiFamilyPageTheme {
        // AMOLED surfaces are already applied to the shared color scheme.
        val backdropColor = InstallerTheme.colorScheme.surfaceContainer
        return remember(backdropColor) {
            UiFamilyPageTheme(backdropColor) { content ->
                Material3InstallerTheme(colorScheme = InstallerTheme.colorScheme, content = content)
            }
        }
    }

    @Composable
    override fun InstallerPage(session: InstallerSessionRepository) {
        com.rosan.installer.ui.page.main.installer.InstallerPage(session)
    }

    @Composable
    override fun RouteContent(route: Route, uiState: ThemeState, sharedViewModel: SettingsSharedViewModel) {
        val useBlur = uiState.useBlur
        when (route) {
            Route.Main -> Material3MainPageWrapper(uiState, sharedViewModel)
            is Route.EditConfig -> EditPage(id = route.id.takeIf { it != -1L }, useBlur = useBlur)
            is Route.ApplyConfig -> ApplyPage(route.id, useBlur)
            Route.About -> AboutPage(useBlur)
            Route.OpenSourceLicense -> OpenSourceLicensePage(useBlur)
            Route.Theme -> ThemeSettingsPage()
            Route.Network -> NetworkPage(useBlur)
            Route.InstallerGlobal -> InstallerGlobalSettingsPage(useBlur)
            Route.AuthorizerCust -> AuthorizerCustPage(useBlur)
            Route.DialogSettings -> DialogSettingsPage(useBlur)
            Route.NotificationSettings -> NotificationSettingsPage(useBlur)
            Route.UninstallerGlobal -> UninstallerGlobalSettingsPage(useBlur)
            Route.Lab -> LabPage(useBlur)
            Route.DefaultInstaller -> DefaultInstallerPage(useBlur)
            Route.Priv -> PrivPage(useBlur)
        }
    }
}
