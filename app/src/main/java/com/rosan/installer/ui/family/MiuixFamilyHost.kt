// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.family

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.domain.settings.model.preferences.ThemeState
import com.rosan.installer.ui.feature.settings.SettingsSharedViewModel
import com.rosan.installer.ui.miuix.theme.MiuixInstallerTheme
import com.rosan.installer.ui.miuix.theme.rememberMiuixThemeController
import com.rosan.installer.ui.navigation.MiuixMainPageWrapper
import com.rosan.installer.ui.navigation.Route
import com.rosan.installer.ui.page.miuix.installer.MiuixInstallerPage
import com.rosan.installer.ui.page.miuix.settings.config.apply.MiuixApplyPage
import com.rosan.installer.ui.page.miuix.settings.config.edit.MiuixEditPage
import com.rosan.installer.ui.page.miuix.settings.home.installer.MiuixDefaultInstallerPage
import com.rosan.installer.ui.page.miuix.settings.home.priv.MiuixPrivPage
import com.rosan.installer.ui.page.miuix.settings.preferred.about.MiuixAboutPage
import com.rosan.installer.ui.page.miuix.settings.preferred.about.ossLicensePage.MiuixOpenSourceLicensePage
import com.rosan.installer.ui.page.miuix.settings.preferred.installer.MiuixInstallerGlobalSettingsPage
import com.rosan.installer.ui.page.miuix.settings.preferred.installer.authorizer.MiuixAuthorizerCustPage
import com.rosan.installer.ui.page.miuix.settings.preferred.installer.dialog.MiuixDialogSettingsPage
import com.rosan.installer.ui.page.miuix.settings.preferred.installer.notification.MiuixNotificationSettingsPage
import com.rosan.installer.ui.page.miuix.settings.preferred.lab.MiuixLabPage
import com.rosan.installer.ui.page.miuix.settings.preferred.network.MiuixNetworkPage
import com.rosan.installer.ui.page.miuix.settings.preferred.theme.MiuixThemeSettingsPage
import com.rosan.installer.ui.page.miuix.settings.preferred.uninstaller.MiuixUninstallerGlobalSettingsPage
import com.rosan.installer.ui.theme.InstallerTheme

internal object MiuixFamilyHost : UiFamilyHost {
    @Composable
    override fun rememberPageTheme(useAmoled: Boolean): UiFamilyPageTheme {
        val controller = rememberMiuixThemeController(
            config = InstallerTheme.colorConfig,
            useMonet = InstallerTheme.useMiuixMonet,
        )
        val useAmoledSurface = useAmoled && InstallerTheme.isDark
        val backdropColor = if (useAmoledSurface) Color.Black else controller.currentColors().surface
        return remember(controller, useAmoledSurface, backdropColor) {
            UiFamilyPageTheme(backdropColor) { content ->
                MiuixInstallerTheme(controller = controller, useAmoledSurface = useAmoledSurface, content = content)
            }
        }
    }

    @Composable
    override fun InstallerPage(session: InstallerSessionRepository) {
        MiuixInstallerPage(session)
    }

    @Composable
    override fun RouteContent(route: Route, uiState: ThemeState, sharedViewModel: SettingsSharedViewModel) {
        val useBlur = uiState.useBlur
        when (route) {
            Route.Main -> MiuixMainPageWrapper(uiState, sharedViewModel)
            is Route.EditConfig -> MiuixEditPage(id = route.id.takeIf { it != -1L }, useBlur = useBlur)
            is Route.ApplyConfig -> MiuixApplyPage(route.id, useBlur)
            Route.About -> MiuixAboutPage(useBlur)
            Route.OpenSourceLicense -> MiuixOpenSourceLicensePage(useBlur)
            Route.Theme -> MiuixThemeSettingsPage()
            Route.Network -> MiuixNetworkPage(useBlur)
            Route.InstallerGlobal -> MiuixInstallerGlobalSettingsPage(useBlur)
            Route.AuthorizerCust -> MiuixAuthorizerCustPage(useBlur)
            Route.DialogSettings -> MiuixDialogSettingsPage(useBlur)
            Route.NotificationSettings -> MiuixNotificationSettingsPage(useBlur)
            Route.UninstallerGlobal -> MiuixUninstallerGlobalSettingsPage(useBlur)
            Route.Lab -> MiuixLabPage(useBlur)
            Route.DefaultInstaller -> MiuixDefaultInstallerPage(useBlur)
            Route.Priv -> MiuixPrivPage(useBlur)
        }
    }
}
