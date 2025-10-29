package com.rosan.installer.ui.page.miuix.settings

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
        // Animation from KernelSU manager
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 5 },
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 5 },
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        }

    ) {
        composable(
            route = MiuixSettingsScreen.MiuixMain.route,
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
        ) {
            val id = it.arguments?.getLong("id")!!
            MiuixApplyPage(
                navController = navController,
                id = id
            )
        }
        composable(
            route = MiuixSettingsScreen.MiuixAbout.route,
        ) {
            MiuixHomePage(navController)
        }
        composable(
            route = MiuixSettingsScreen.MiuixTheme.route,
        ) {
            MiuixThemeSettingsPage(navController = navController, viewModel = preferredViewModel)

        }
        composable(
            route = MiuixSettingsScreen.MiuixInstallerGlobal.route,
        ) {
            MiuixInstallerGlobalSettingsPage(navController = navController, viewModel = preferredViewModel)
        }
    }
}