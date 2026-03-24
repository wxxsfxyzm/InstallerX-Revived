// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.activity

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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

class SettingsActivity : AppCompatActivity(), KoinComponent {
    companion object {
        private const val LAUNCH_REQUEST_CODE = 1001

        fun createLaunchIntent(context: Context): Intent =
            Intent(context, SettingsActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }

        fun createLaunchPendingIntent(context: Context): PendingIntent = PendingIntent.getActivity(
            context,
            LAUNCH_REQUEST_CODE,
            createLaunchIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

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
            /*CompositionLocalProvider(
                LocalSessionInstallSupported provides capabilityProvider.isSessionInstallSupported
            ) {*/
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
                    MiuixSurface(modifier = Modifier.fillMaxSize()) { MiuixSettingsPage() }
                } else {
                    Material3Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = if (uiState.isExpressive) MaterialTheme.colorScheme.surfaceContainer
                        else MaterialTheme.colorScheme.surface
                    ) { SettingsPage() }
                }
            }
        }
    }
}
