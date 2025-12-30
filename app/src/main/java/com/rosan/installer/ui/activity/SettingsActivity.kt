package com.rosan.installer.ui.activity

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.rosan.installer.build.model.impl.DeviceCapabilityChecker
import com.rosan.installer.ui.common.LocalSessionInstallSupported
import com.rosan.installer.ui.page.main.settings.SettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewAction
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewState
import com.rosan.installer.ui.page.miuix.settings.MiuixSettingsPage
import com.rosan.installer.ui.theme.InstallerTheme
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.koinInject
import org.koin.core.component.KoinComponent
import androidx.compose.material3.Surface as Material3Surface
import top.yukonga.miuix.kmp.basic.Surface as MiuixSurface

class SettingsActivity : ComponentActivity(), KoinComponent {
    private val preferredViewModel: PreferredViewModel by viewModel()
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        // Enable edge-to-edge mode for immersive experience
        enableEdgeToEdge()
        // Compat Navigation Bar color for Xiaomi Devices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            window.isNavigationBarContrastEnforced = false
        // Keep splash screen visible until data (theme setting) is loaded.
        splashScreen.setKeepOnScreenCondition { preferredViewModel.state.progress != PreferredViewState.Progress.Loaded }

        super.onCreate(savedInstanceState)
        setContent {
            val capabilityChecker = koinInject<DeviceCapabilityChecker>()
            CompositionLocalProvider(
                LocalSessionInstallSupported provides capabilityChecker.isSessionInstallSupported
            ) {
                LaunchedEffect(Unit) {
                    preferredViewModel.dispatch(PreferredViewAction.Init)
                }
                val state = preferredViewModel.state
                InstallerTheme(
                    useMiuix = state.showMiuixUI,
                    themeMode = state.themeMode,
                    paletteStyle = state.paletteStyle,
                    useDynamicColor = state.useDynamicColor,
                    useMiuixMonet = state.useMiuixMonet,
                    seedColor = state.seedColor
                ) {
                    val modifier = Modifier.fillMaxSize()
                    if (state.showMiuixUI) {
                        MiuixSurface(modifier = modifier) { MiuixSettingsPage(preferredViewModel) }
                    } else {
                        Material3Surface(modifier = modifier) { SettingsPage(preferredViewModel) }
                    }
                }
            }
        }
    }
}