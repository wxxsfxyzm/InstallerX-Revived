// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer.dialog.inner

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParamsType

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun uninstallingDialog(
    viewModel: InstallerViewModel
): DialogParams {
    // Call uninstallInfoDialog for base structure (icon, title, subtitle with new version)
    val baseParams = uninstallInfoDialog(
        viewModel = viewModel,
        onTitleExtraClick = {}
    )

    // Override text and buttons
    return baseParams.copy(
        text = DialogInnerParams(
            DialogParamsType.InstallerInstalling.id
        ) {
            Column {
                Text(
                    text = stringResource(R.string.uninstall_progress_text),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                // --- M3E ---
                LinearWavyProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    amplitude = 0f // not wavy
                )
            }
        },
        buttons = dialogButtons(DialogParamsType.ButtonsCancel.id) {
            emptyList()
        }
    )
}
