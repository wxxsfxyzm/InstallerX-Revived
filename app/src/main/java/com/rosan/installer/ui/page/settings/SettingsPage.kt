package com.rosan.installer.ui.page.settings

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
import com.rosan.installer.ui.page.settings.config.apply.ApplyPage
import com.rosan.installer.ui.page.settings.config.edit.EditPage
import com.rosan.installer.ui.page.settings.main.MainPage

@Composable
fun SettingsPage() {
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
                // 当从 EditPage 返回时，EditPage 的退出动画
                // 它会随着手势逐渐缩小和淡出
                scaleOut(targetScale = 0.9f) + fadeOut()
            }
        ) {
            val id = it.arguments?.getLong("id")
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
            ApplyPage(
                navController = navController,
                id = id
            )
        }
    }
}
