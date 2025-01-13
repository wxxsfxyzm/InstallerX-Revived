package com.rosan.installer.ui.page.settings

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.navigation.NavType
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.rosan.installer.ui.page.settings.config.apply.ApplyPage
import com.rosan.installer.ui.page.settings.config.edit.EditPage
import com.rosan.installer.ui.page.settings.main.MainPage

@OptIn(ExperimentalAnimationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun SettingsPage() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = SettingsScreen.Main.route,
    ) {
        composable(
            route = SettingsScreen.Main.route,
            enterTransition = {
                null
            },
            exitTransition = {
                null
            },
            popEnterTransition = {
                null
            },
            popExitTransition = {
                null
            }
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
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Up,
                )
            },
            exitTransition = {
                null
            },
            popEnterTransition = {
                null
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Down,
                )
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
            enterTransition = { null },
            exitTransition = { null },
            popEnterTransition = { null },
            popExitTransition = { null }
        ) {
            val id = it.arguments?.getLong("id")!!
            ApplyPage(
                navController = navController,
                id = id
            )
        }
    }
}
