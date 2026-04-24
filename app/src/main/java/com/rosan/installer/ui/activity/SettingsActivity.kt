// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.activity

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.domain.settings.model.ThemeState
import com.rosan.installer.domain.settings.provider.ThemeStateProvider
import com.rosan.installer.ui.navigation.InstallerNavContainer
import com.rosan.installer.ui.theme.InstallerTheme
import com.rosan.installer.ui.theme.LocalWindowLayoutInfo
import com.rosan.installer.ui.theme.rememberWindowLayoutInfo
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import top.yukonga.miuix.kmp.theme.MiuixTheme

class SettingsActivity : ComponentActivity(), KoinComponent {
    private val themeStateProvider by inject<ThemeStateProvider>()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        // Enable edge-to-edge mode for immersive experience
        enableEdgeToEdge()
        // Compat Navigation Bar color for Xiaomi Devices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            window.isNavigationBarContrastEnforced = false

        var isThemeLoaded = false
        // Keep splash screen visible until data is safely loaded
        splashScreen.setKeepOnScreenCondition { !isThemeLoaded }

        super.onCreate(savedInstanceState)
        setContent {
            val uiState by themeStateProvider.themeStateFlow.collectAsStateWithLifecycle(initialValue = ThemeState())
            isThemeLoaded = uiState.isLoaded

            // Prevent heavy navigation setup until state is ready
            if (!isThemeLoaded) return@setContent

            val layoutInfo = rememberWindowLayoutInfo()

            CompositionLocalProvider(
                LocalWindowLayoutInfo provides layoutInfo
            ) {
                InstallerTheme(
                    useMiuix = uiState.useMiuix,
                    themeMode = uiState.themeMode,
                    paletteStyle = uiState.paletteStyle,
                    colorSpec = uiState.colorSpec,
                    useDynamicColor = uiState.useDynamicColor,
                    useMiuixMonet = uiState.useMiuixMonet,
                    seedColor = uiState.seedColor
                ) {
                    val backgroundColor =
                        if (uiState.useMiuix)
                            MiuixTheme.colorScheme.surface
                        else
                            MaterialTheme.colorScheme.surfaceContainer
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(backgroundColor)
                    ) {
                        InstallerNavContainer(uiState)
                    }
                }
            }
        }
    }
}
