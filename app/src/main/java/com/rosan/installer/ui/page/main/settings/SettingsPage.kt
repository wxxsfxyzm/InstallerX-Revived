// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rosan.installer.domain.settings.model.ThemeState
import com.rosan.installer.domain.settings.provider.ThemeStateProvider
import com.rosan.installer.ui.page.main.settings.config.apply.ApplyPage
import com.rosan.installer.ui.page.main.settings.config.apply.NewApplyPage
import com.rosan.installer.ui.page.main.settings.config.edit.EditPage
import com.rosan.installer.ui.page.main.settings.config.edit.NewEditPage
import com.rosan.installer.ui.page.main.settings.main.MainPage
import com.rosan.installer.ui.page.main.settings.preferred.subpage.about.AboutPage
import com.rosan.installer.ui.page.main.settings.preferred.subpage.about.NewAboutPage
import com.rosan.installer.ui.page.main.settings.preferred.subpage.about.OpenSourceLicensePage
import com.rosan.installer.ui.page.main.settings.preferred.subpage.installer.LegacyInstallerGlobalSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.subpage.installer.NewInstallerGlobalSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.subpage.lab.LegacyLabPage
import com.rosan.installer.ui.page.main.settings.preferred.subpage.lab.NewLabPage
import com.rosan.installer.ui.page.main.settings.preferred.subpage.theme.LegacyThemeSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.subpage.theme.NewThemeSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.subpage.uninstaller.LegacyUninstallerGlobalSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.subpage.uninstaller.NewUninstallerGlobalSettingsPage
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun SettingsPage(
    sharedViewModel: SettingsSharedViewModel = koinViewModel(viewModelStoreOwner = LocalActivity.current as ComponentActivity)
) {
    val navController = rememberNavController()
    val themeStateProvider = koinInject<ThemeStateProvider>()
    val uiState by themeStateProvider.themeStateFlow.collectAsStateWithLifecycle(initialValue = ThemeState())
    val sharedState by sharedViewModel.state.collectAsStateWithLifecycle()
    val useBlur = uiState.useBlur
    val isExpressive = uiState.isExpressive

    val currentBackStackEntry by navController.currentBackStackEntryAsState()

    LaunchedEffect(currentBackStackEntry, sharedState.pendingNavigateToTheme) {
        if (sharedState.pendingNavigateToTheme && currentBackStackEntry?.destination?.route == SettingsScreen.Main.route) {
            navController.navigate(SettingsScreen.Theme.route)
            sharedViewModel.markPendingNavigateToTheme(false)
        }
    }

    NavHost(
        navController = navController,
        startDestination = SettingsScreen.Main.route,
    ) {
        composable(
            route = SettingsScreen.Main.route,
            exitTransition = {
                // 从 MainPage 到 EditPage 时，MainPage 的退出动画
                slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth / 4 }) +
                        fadeOut()
            },
            // --- MainPage 的 popEnterTransition ---
            popEnterTransition = {
                // 当从 EditPage 返回 MainPage 时，MainPage 的进入动画
                slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth / 4 }) +
                        fadeIn()
            },
            // 其他参数设为 null 或保持不变
            popExitTransition = { null },
            enterTransition = { null }
        ) {
            MainPage(navController = navController)
        }
        composable(
            route = SettingsScreen.EditConfig.route,
            arguments = listOf(
                navArgument("id") {
                    type = NavType.LongType
                }
            ),
            enterTransition = {
                slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth })
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth })
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth })
            },
            // --- EditPage 的 popExitTransition ---
            popExitTransition = {
                scaleOut(targetScale = 0.9f) + fadeOut()
            }
        ) {
            val id = it.arguments?.getLong("id")
            if (isExpressive)
                NewEditPage(
                    navController = navController,
                    id = if (id != -1L) id else null,
                    useBlur = useBlur
                ) else
                EditPage(
                    navController = navController,
                    id = if (id != -1L) id
                    else null
                )
        }

        composable(
            route = SettingsScreen.ApplyConfig.route,
            arguments = listOf(
                navArgument("id") {
                    type = NavType.LongType
                }
            ),
            enterTransition = {
                slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth })
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth })
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth })
            },
            popExitTransition = {
                scaleOut(targetScale = 0.9f) + fadeOut()
            }
        ) {
            val id = it.arguments?.getLong("id")!!
            if (isExpressive)
                NewApplyPage(navController = navController, id = id)
            else
                ApplyPage(navController = navController, id = id)
        }
        composable(
            route = SettingsScreen.About.route,
            enterTransition = {
                slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth })
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth })
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth })
            },
            popExitTransition = {
                scaleOut(targetScale = 0.9f) + fadeOut()
            }
        ) {
            if (isExpressive)
                NewAboutPage(navController = navController)
            else
                AboutPage(navController = navController)
        }
        composable(
            route = SettingsScreen.OpenSourceLicense.route,
            enterTransition = {
                slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth })
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth })
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth })
            },
            popExitTransition = {
                scaleOut(targetScale = 0.9f) + fadeOut()
            }
        ) {
            OpenSourceLicensePage(navController, isExpressive, uiState.useBlur)
        }
        composable(
            route = SettingsScreen.Theme.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            popExitTransition = { scaleOut(targetScale = 0.9f) + fadeOut() }
        ) {
            if (isExpressive) {
                NewThemeSettingsPage(navController = navController)
            } else {
                LegacyThemeSettingsPage(navController = navController)
            }
        }
        composable(
            route = SettingsScreen.InstallerGlobal.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            popExitTransition = { scaleOut(targetScale = 0.9f) + fadeOut() }
        ) {
            if (isExpressive) {
                NewInstallerGlobalSettingsPage(navController = navController)
            } else {
                LegacyInstallerGlobalSettingsPage(navController = navController)
            }
        }
        composable(
            route = SettingsScreen.UninstallerGlobal.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            popExitTransition = { scaleOut(targetScale = 0.9f) + fadeOut() }
        ) {
            if (isExpressive) {
                NewUninstallerGlobalSettingsPage(navController = navController)
            } else {
                LegacyUninstallerGlobalSettingsPage(navController = navController)
            }
        }
        composable(
            route = SettingsScreen.Lab.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            popExitTransition = { scaleOut(targetScale = 0.9f) + fadeOut() }
        ) {
            if (isExpressive) {
                NewLabPage(navController = navController)
            } else {
                LegacyLabPage(navController = navController)
            }
        }
    }
}
