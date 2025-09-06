package com.rosan.installer.ui.activity

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.rosan.installer.ui.page.miuix.settings.MiuixSettingsPage
import com.rosan.installer.ui.theme.InstallerMiuixTheme
import org.koin.core.component.KoinComponent
import top.yukonga.miuix.kmp.basic.Surface

class SettingsActivity : ComponentActivity(), KoinComponent {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Enable edge-to-edge mode for immersive experience
        enableEdgeToEdge()
        // Compat Navigation Bar color for Xiaomi Devices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            window.isNavigationBarContrastEnforced = false
        super.onCreate(savedInstanceState)
        setContent {
            // A surface based on material design theme.
            /*InstallerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsPage()
                }
            }*/
            InstallerMiuixTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MiuixSettingsPage()
                }
            }
        }
    }
}