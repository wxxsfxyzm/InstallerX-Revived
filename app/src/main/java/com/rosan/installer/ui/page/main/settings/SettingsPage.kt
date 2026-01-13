package com.rosan.installer.ui.page.main.settings

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rosan.installer.ui.page.main.settings.config.apply.ApplyPage
import com.rosan.installer.ui.page.main.settings.config.apply.NewApplyPage
import com.rosan.installer.ui.page.main.settings.config.edit.EditPage
import com.rosan.installer.ui.page.main.settings.config.edit.NewEditPage
import com.rosan.installer.ui.page.main.settings.main.MainPage
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.page.main.settings.preferred.subpage.home.HomePage
import com.rosan.installer.ui.page.main.settings.preferred.subpage.home.NewHomePage
import com.rosan.installer.ui.page.main.settings.preferred.subpage.home.OpenSourceLicensePage
import com.rosan.installer.ui.page.main.settings.preferred.subpage.installer.LegacyInstallerGlobalSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.subpage.installer.LegacyUninstallerGlobalSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.subpage.installer.NewInstallerGlobalSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.subpage.installer.NewUninstallerGlobalSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.subpage.lab.LegacyLabPage
import com.rosan.installer.ui.page.main.settings.preferred.subpage.lab.NewLabPage
import com.rosan.installer.ui.page.main.settings.preferred.subpage.theme.LegacyThemeSettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.subpage.theme.NewThemeSettingsPage

@Composable
fun SettingsPage(preferredViewModel: PreferredViewModel) {
    val navController = rememberNavController()

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
            MainPage(navController = navController, preferredViewModel)
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
            if (preferredViewModel.state.showExpressiveUI)
                NewEditPage(
                    navController = navController,
                    id = if (id != -1L) id
                    else null
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
            if (preferredViewModel.state.showExpressiveUI)
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
            if (preferredViewModel.state.showExpressiveUI)
                NewHomePage(navController = navController, viewModel = preferredViewModel)
            else
                HomePage(navController = navController, viewModel = preferredViewModel)
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
            OpenSourceLicensePage(navController, preferredViewModel.state.showExpressiveUI)
        }
        composable(
            route = SettingsScreen.Theme.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            popExitTransition = { scaleOut(targetScale = 0.9f) + fadeOut() }
        ) {
            if (preferredViewModel.state.showExpressiveUI) {
                NewThemeSettingsPage(navController = navController, viewModel = preferredViewModel)
            } else {
                LegacyThemeSettingsPage(navController = navController, viewModel = preferredViewModel)
            }
        }
        composable(
            route = SettingsScreen.InstallerGlobal.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            popExitTransition = { scaleOut(targetScale = 0.9f) + fadeOut() }
        ) {
            if (preferredViewModel.state.showExpressiveUI) {
                NewInstallerGlobalSettingsPage(navController = navController, viewModel = preferredViewModel)
            } else {
                LegacyInstallerGlobalSettingsPage(navController = navController, viewModel = preferredViewModel)
            }
        }
        composable(
            route = SettingsScreen.UninstallerGlobal.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            popExitTransition = { scaleOut(targetScale = 0.9f) + fadeOut() }
        ) {
            if (preferredViewModel.state.showExpressiveUI) {
                NewUninstallerGlobalSettingsPage(navController = navController, viewModel = preferredViewModel)
            } else {
                LegacyUninstallerGlobalSettingsPage(navController = navController, viewModel = preferredViewModel)
            }
        }
        composable(
            route = SettingsScreen.Lab.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            popExitTransition = { scaleOut(targetScale = 0.9f) + fadeOut() }
        ) {
            if (preferredViewModel.state.showExpressiveUI) {
                NewLabPage(navController = navController, viewModel = preferredViewModel)
            } else {
                LegacyLabPage(navController = navController, viewModel = preferredViewModel)
            }
        }
    }
}
