package com.rosan.installer.ui.theme

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
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

@Composable
fun InstallerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // 添加动态颜色支持：仅对 Android 12 (API 31) 及以上版本有效
    dynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // 添加版本检查
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}