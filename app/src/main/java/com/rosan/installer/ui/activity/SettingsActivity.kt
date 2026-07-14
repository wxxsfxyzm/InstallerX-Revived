// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.activity

import android.content.Context
import android.content.Intent
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
import androidx.lifecycle.lifecycleScope
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.preferences.ThemeState
import com.rosan.installer.domain.settings.provider.ThemeStateProvider
import com.rosan.installer.framework.packageupdate.SelfUpdateRecoveryManager
import com.rosan.installer.ui.navigation.InstallerNavContainer
import com.rosan.installer.ui.theme.InstallerTheme
import com.rosan.installer.ui.theme.LocalWindowLayoutInfo
import com.rosan.installer.ui.theme.rememberWindowLayoutInfo
import com.rosan.installer.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import top.yukonga.miuix.kmp.theme.MiuixTheme

class SettingsActivity : ComponentActivity(), KoinComponent {
    companion object {
        // InstallerActivity is singleInstance, so recover into the reusable normal app task
        // before removing the temporary package-update task.
        fun createSelfUpdateRecoveryIntent(context: Context) =
            Intent(context, SettingsActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }
    }

    private val themeStateProvider by inject<ThemeStateProvider>()
    private val selfUpdateRecoveryManager by inject<SelfUpdateRecoveryManager>()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        // Enable edge-to-edge mode for immersive experience
        enableEdgeToEdge()

        var isThemeLoaded = false
        // Keep splash screen visible until data is safely loaded
        splashScreen.setKeepOnScreenCondition { !isThemeLoaded }

        super.onCreate(savedInstanceState)
        showSelfUpdateSuccess()
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
                    seedColor = androidx.compose.ui.graphics.Color(uiState.seedColor)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        showSelfUpdateSuccess()
    }

    private fun showSelfUpdateSuccess() {
        // Package replacement destroyed the original session; consume the durable handoff event
        // instead of expecting a terminal progress state from the old process.
        lifecycleScope.launch {
            if (selfUpdateRecoveryManager.consumeCompletionNotice()) {
                toast(R.string.self_update_install_success)
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            selfUpdateRecoveryManager.deleteCompletedSource()
        }
    }
}
