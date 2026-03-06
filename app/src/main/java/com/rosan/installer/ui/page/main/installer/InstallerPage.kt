package com.rosan.installer.ui.page.main.installer

import androidx.compose.runtime.Composable
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.ui.page.main.installer.dialog.DialogPage

@Composable
fun InstallerPage(installer: InstallerSessionRepository) {
    DialogPage(installer = installer)
}