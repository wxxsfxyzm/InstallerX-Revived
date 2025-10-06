package com.rosan.installer.ui.page.miuix.installer

import androidx.compose.runtime.Composable
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.miuix.installer.fullsceen.MiuixFullscreenPage

@Composable
fun MiuixInstallerPage(installer: InstallerRepo) {
    MiuixFullscreenPage(installer = installer)
}