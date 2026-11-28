// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.ui.page.main.widget.card

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.core.device.model.Level
import com.rosan.installer.core.env.AppConfig
import com.rosan.installer.ui.page.main.settings.preferred.about.AboutViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatusWidget(viewModel: AboutViewModel, useBlur: Boolean = false) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    val containerColor = when (AppConfig.LEVEL) {
        Level.STABLE -> MaterialTheme.colorScheme.primaryContainer
        Level.PREVIEW -> MaterialTheme.colorScheme.secondaryContainer
        Level.UNSTABLE -> MaterialTheme.colorScheme.tertiaryContainer
    }

    val onContainerColor = when (AppConfig.LEVEL) {
        Level.STABLE -> MaterialTheme.colorScheme.onPrimaryContainer
        Level.PREVIEW -> MaterialTheme.colorScheme.onSecondaryContainer
        Level.UNSTABLE -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    val level = when (AppConfig.LEVEL) {
        Level.STABLE -> stringResource(id = R.string.stable)
        Level.PREVIEW -> stringResource(id = R.string.preview)
        Level.UNSTABLE -> stringResource(id = R.string.unstable)
    }

    val versionInfoText = stringResource(
        id = R.string.app_version_info_format,
        level,
        AppConfig.VERSION_NAME,
        AppConfig.VERSION_CODE,
    )

    FluidStatusCard(
        containerColor = containerColor,
        contentColor = onContainerColor,
        useBlur = useBlur,
        icon = {
            if (uiState.appIcon != null) {
                Image(
                    bitmap = uiState.appIcon!!,
                    modifier = Modifier.size(56.dp),
                    contentDescription = stringResource(id = R.string.app_name),
                )
            } else {
                // Placeholder
                Box(modifier = Modifier.size(56.dp))
            }
        },
        title = {
            Text(
                modifier = Modifier,
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = versionInfoText,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (uiState.hasUpdate) {
                    Text(
                        text = stringResource(R.string.update_available, uiState.remoteVersion),
                        style = MaterialTheme.typography.bodyMediumEmphasized,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
    )
}
