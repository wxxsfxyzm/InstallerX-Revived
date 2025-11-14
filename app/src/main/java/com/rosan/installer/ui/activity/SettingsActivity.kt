package com.rosan.installer.ui.activity

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.rosan.installer.build.RsConfig
import com.rosan.installer.ui.page.main.settings.SettingsPage
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewAction
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewState
import com.rosan.installer.ui.page.miuix.settings.MiuixSettingsPage
import com.rosan.installer.ui.theme.InstallerMaterialExpressiveTheme
import com.rosan.installer.ui.theme.InstallerMiuixTheme
import com.rosan.installer.ui.theme.m3color.ThemeMode
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinComponent
import timber.log.Timber
import androidx.compose.material3.Surface as Material3Surface
import top.yukonga.miuix.kmp.basic.Surface as MiuixSurface

class SettingsActivity : ComponentActivity(), KoinComponent {
    private val preferredViewModel: PreferredViewModel by viewModel()
    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d(RsConfig.isMiui.toString())
        val splashScreen = installSplashScreen()
        // Enable edge-to-edge mode for immersive experience
        enableEdgeToEdge()
        // Compat Navigation Bar color for Xiaomi Devices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            window.isNavigationBarContrastEnforced = false
        // Keep splash screen visible until data (theme setting) is loaded.
        // We use a flag that will be updated by observing the ViewModel state.
        var isLoading = true
        splashScreen.setKeepOnScreenCondition { isLoading }

        // Observe ViewModel loading state to dismiss the splash screen.
        lifecycleScope.launch {
            snapshotFlow { preferredViewModel.state.progress } // 监听 state.progress 属性
                .map { it == PreferredViewState.Progress.Loaded }
                .distinctUntilChanged()
                .collect { isLoaded ->
                    isLoading = !isLoaded // 当 progress变为Loaded时，isLoaded为true，isLoading变为false
                }
        }

        super.onCreate(savedInstanceState)
        setContent {
            LaunchedEffect(Unit) {
                preferredViewModel.dispatch(PreferredViewAction.Init)
            }
            val state = preferredViewModel.state
            val useDarkTheme = when (state.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            if (state.showMiuixUI) {
                InstallerMiuixTheme {
                    MiuixSurface(modifier = Modifier.fillMaxSize()) {
                        MiuixSettingsPage(preferredViewModel)
                    }
                }
            } else {
                InstallerMaterialExpressiveTheme(
                    darkTheme = useDarkTheme,
                    useDynamicColor = state.useDynamicColor,
                    compatStatusBarColor = true,
                    seedColor = state.seedColor,
                    paletteStyle = state.paletteStyle
                ) {
                    Material3Surface(modifier = Modifier.fillMaxSize()) {
                        SettingsPage(preferredViewModel)
                    }
                }
            }
        }
    }
}
