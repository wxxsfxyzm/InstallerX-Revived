package com.rosan.installer.ui.theme

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.core.view.WindowCompat
import com.rosan.installer.ui.theme.m3color.PaletteStyle
import com.rosan.installer.ui.theme.m3color.ThemeMode
import com.rosan.installer.ui.theme.m3color.dynamicColorScheme
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InstallerMaterialExpressiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    compatStatusBarColor: Boolean = false,
    seedColor: Color,
    paletteStyle: PaletteStyle,
    content: @Composable () -> Unit
) {
    val keyColor = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        colorResource(id = android.R.color.system_accent1_500)
    else seedColor

    if (compatStatusBarColor) {
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as ComponentActivity).window
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    val colorScheme = dynamicColorScheme(
        keyColor = keyColor,
        isDark = darkTheme,
        style = paletteStyle
    )

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = Typography,
        content = content
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InstallerMaterialExpressiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorScheme: ColorScheme,
    compatStatusBarColor: Boolean = false,
    content: @Composable () -> Unit
) {
    if (compatStatusBarColor) {
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as ComponentActivity).window
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = Typography,
        content = content
    )
}

@Composable
fun InstallerMiuixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeMode: ThemeMode,
    useMiuixMonet: Boolean,
    useDynamicColor: Boolean = false,
    compatStatusBarColor: Boolean = false,
    seedColor: Color,
    content: @Composable () -> Unit
) {
    if (compatStatusBarColor) {
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as ComponentActivity).window
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    val controller = if (useMiuixMonet) {
        // --- Monet Engine Path ---
        val keyColor = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            colorResource(id = android.R.color.system_accent1_500)
        else seedColor

        val colorSchemeMode = when (themeMode) {
            ThemeMode.SYSTEM -> ColorSchemeMode.MonetSystem
            ThemeMode.LIGHT -> ColorSchemeMode.MonetLight
            ThemeMode.DARK -> ColorSchemeMode.MonetDark
        }

        remember(colorSchemeMode, keyColor, darkTheme) {
            ThemeController(
                colorSchemeMode = colorSchemeMode,
                keyColor = keyColor,
                isDark = darkTheme
            )
        }
    } else {
        // --- Default Miuix Theme Path ---
        val colorSchemeMode = when (themeMode) {
            ThemeMode.SYSTEM -> ColorSchemeMode.System
            ThemeMode.LIGHT -> ColorSchemeMode.Light
            ThemeMode.DARK -> ColorSchemeMode.Dark
        }

        remember(colorSchemeMode, darkTheme) {
            ThemeController(
                colorSchemeMode = colorSchemeMode,
                isDark = darkTheme
            )
        }
    }

    MiuixTheme(
        controller = controller,
        content = content
    )
}