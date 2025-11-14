package com.rosan.installer.ui.page.main.installer

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.main.installer.dialog.DialogPage
import com.rosan.installer.ui.theme.m3color.PaletteStyle

@Composable
fun InstallerPage(
    installer: InstallerRepo,
    activeColorSchemeState: MutableState<ColorScheme>,
    globalColorScheme: ColorScheme,
    isDarkMode: Boolean,
    basePaletteStyle: PaletteStyle
) {
    DialogPage(
        installer = installer,
        activeColorSchemeState = activeColorSchemeState,
        globalColorScheme = globalColorScheme,
        isDarkMode = isDarkMode,
        basePaletteStyle = basePaletteStyle
    )
}