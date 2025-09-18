package com.rosan.installer.ui.page.miuix.settings

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
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.page.miuix.settings.config.apply.MiuixApplyPage
import com.rosan.installer.ui.page.miuix.settings.config.edit.MiuixEditPage
import com.rosan.installer.ui.page.miuix.settings.main.MiuixMainPage
import com.rosan.installer.ui.page.miuix.settings.preferred.subpage.home.MiuixHomePage
import com.rosan.installer.ui.page.miuix.settings.preferred.subpage.installer.MiuixInstallerGlobalSettingsPage
import com.rosan.installer.ui.page.miuix.settings.preferred.subpage.theme.MiuixThemeSettingsPage

@Composable
fun MiuixSettingsPage(preferredViewModel: PreferredViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = MiuixSettingsScreen.MiuixMain.route,
    ) {
        composable(
            route = MiuixSettingsScreen.MiuixMain.route,
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
            MiuixMainPage(navController = navController, preferredViewModel)
        }
        composable(
            route = MiuixSettingsScreen.MiuixEditConfig.route,
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
                // 当从 EditPage 返回时，EditPage 的退出动画
                // 它会随着手势逐渐缩小和淡出
                scaleOut(targetScale = 0.9f) + fadeOut()
            }
        ) {
            val id = it.arguments?.getLong("id")
            MiuixEditPage(
                navController = navController,
                id = if (id != -1L) id
                else null
            )
        }

        composable(
            route = MiuixSettingsScreen.MiuixApplyConfig.route,
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
            MiuixApplyPage(
                navController = navController,
                id = id
            )
        }
        composable(
            route = MiuixSettingsScreen.MiuixAbout.route, // 使用新路由
            enterTransition = { // 使用统一的进入动画
                slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth })
            },
            exitTransition = { // 占位，当从 About 再导航到别的页面时使用
                slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth })
            },
            popEnterTransition = { // 占位，当从其他页面返回 About 时使用
                slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth })
            },
            popExitTransition = { // 当从 About 返回时，使用的退出动画
                scaleOut(targetScale = 0.9f) + fadeOut()
            }
        ) {
            MiuixHomePage(navController)
        }
        composable(
            route = MiuixSettingsScreen.MiuixTheme.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            popExitTransition = { scaleOut(targetScale = 0.9f) + fadeOut() } // Your predictive back animation
        ) {
            MiuixThemeSettingsPage(navController = navController, viewModel = preferredViewModel)

        }
        composable(
            route = MiuixSettingsScreen.MiuixInstallerGlobal.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            popExitTransition = { scaleOut(targetScale = 0.9f) + fadeOut() } // Your predictive back animation
        ) {
            MiuixInstallerGlobalSettingsPage(navController = navController, viewModel = preferredViewModel)
        }
    }
}
