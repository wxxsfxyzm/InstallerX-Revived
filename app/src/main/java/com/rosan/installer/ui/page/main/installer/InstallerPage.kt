package com.rosan.installer.ui.page.main.installer

import androidx.compose.runtime.Composable
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.main.installer.dialog.DialogPage

@Composable
fun InstallerPage(installer: InstallerRepo) {
    DialogPage(installer = installer)
}