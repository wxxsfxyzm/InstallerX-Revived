package com.rosan.installer.ui.theme

import android.app.Activity
import android.os.Build
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.core.view.WindowCompat
import com.rosan.installer.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InstallerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // 添加动态颜色支持：仅对 Android 12 (API 31) 及以上版本有效
    dynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        // 添加版本检查
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> darkColorScheme(
            primary = colorResource(R.color.dark_primary),
            primaryContainer = colorResource(R.color.dark_primary_container),
            secondary = colorResource(R.color.dark_secondary),
            tertiary = colorResource(R.color.dark_tertiary),
            error = colorResource(R.color.dark_error)
        )

        else -> lightColorScheme(
            primary = colorResource(R.color.light_primary),
            primaryContainer = colorResource(R.color.light_primary_container),
            secondary = colorResource(R.color.light_secondary),
            tertiary = colorResource(R.color.light_tertiary)
        )
    }

    // New Status Bar Color Logic
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as ComponentActivity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    DisableNavigationBarContrast(view)

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Disable navigation bar contrast enforcement.
 * This is useful for devices where the navigation bar color should not be enforced
 * to match the system theme, allowing for custom colors.
 */
@Composable
fun DisableNavigationBarContrast(view: View) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.isNavigationBarContrastEnforced = false
        }
    }
}
