// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.activity

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.domain.settings.model.ThemeState
import com.rosan.installer.domain.settings.provider.ThemeStateProvider
import com.rosan.installer.ui.page.main.settings.SettingsPage
import com.rosan.installer.ui.page.miuix.settings.MiuixSettingsPage
import com.rosan.installer.ui.theme.InstallerTheme
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import androidx.compose.material3.Surface as Material3Surface
import top.yukonga.miuix.kmp.basic.Surface as MiuixSurface

class SettingsActivity : ComponentActivity(), KoinComponent {
    private val themeStateProvider by inject<ThemeStateProvider>()
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        // Enable edge-to-edge mode for immersive experience
        enableEdgeToEdge()
        // Compat Navigation Bar color for Xiaomi Devices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            window.isNavigationBarContrastEnforced = false
        // Keep splash screen visible until data (theme setting) is loaded.
        var isThemeLoaded = false
        splashScreen.setKeepOnScreenCondition { !isThemeLoaded }

        super.onCreate(savedInstanceState)
        setContent {
            val uiState by themeStateProvider.themeStateFlow.collectAsStateWithLifecycle(initialValue = ThemeState())
            isThemeLoaded = uiState.isLoaded
            InstallerTheme(
                isExpressive = uiState.isExpressive,
                useMiuix = uiState.useMiuix,
                themeMode = uiState.themeMode,
                paletteStyle = uiState.paletteStyle,
                colorSpec = uiState.colorSpec,
                useDynamicColor = uiState.useDynamicColor,
                useMiuixMonet = uiState.useMiuixMonet,
                seedColor = uiState.seedColor
            ) {
                if (uiState.useMiuix) {
                    MiuixSurface(modifier = Modifier.fillMaxSize()) {
                        MiuixSettingsPage(uiState)
                    }
                } else {
                    Material3Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = if (uiState.isExpressive) MaterialTheme.colorScheme.surfaceContainer
                        else MaterialTheme.colorScheme.surface
                    ) { SettingsPage(uiState) }
                }
            }
        }
    }
}