package com.rosan.installer.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.rosan.installer.ui.page.settings.home.HomePage
import com.rosan.installer.ui.theme.InstallerTheme
import org.koin.core.component.KoinComponent

class AboutPageActivity : ComponentActivity(), KoinComponent {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            // A surface based on material design theme.
            InstallerTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    // AgreementDialog()
                    HomePage()
                }
            }
        }
    }
}