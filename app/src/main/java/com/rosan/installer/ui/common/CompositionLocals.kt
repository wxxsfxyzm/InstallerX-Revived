package com.rosan.installer.ui.common

import androidx.compose.runtime.staticCompositionLocalOf

val LocalSessionInstallSupported = staticCompositionLocalOf<Boolean> {
    error("CompositionLocal LocalSessionInstallSupported not present")
}

val LocalMiPackageInstallerPresent = staticCompositionLocalOf { false }