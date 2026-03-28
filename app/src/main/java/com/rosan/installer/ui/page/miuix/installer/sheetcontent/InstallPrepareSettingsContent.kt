// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.installer.sheetcontent

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.core.env.DeviceConfig
import com.rosan.installer.domain.device.model.Manufacturer
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.miuix.widgets.MiuixSwitchWidget
import com.rosan.installer.ui.theme.InstallerTheme
import com.rosan.installer.ui.theme.miuixSheetCardColorDark
import com.rosan.installer.ui.util.isGestureNavigation
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.isDynamicColor

@Composable
fun PrepareSettingsContent(
    session: InstallerSessionRepository,
    viewModel: InstallerViewModel
) {
    val isDarkMode = InstallerTheme.isDark
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.viewSettings

    // Local UI state proxy for session.config since it's not a reactive flow.
    // We update these directly in the onCheckedChange callbacks.
    var autoDelete by remember { mutableStateOf(session.config.autoDelete) }
    var displaySdk by remember { mutableStateOf(session.config.displaySdk) }
    var displaySize by remember { mutableStateOf(session.config.displaySize) }

    BackHandler {
        viewModel.dispatch(InstallerViewAction.HideMiuixSheetRightActionSettings)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = if (isGestureNavigation()) 24.dp else 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.padding(bottom = 6.dp),
            colors = CardColors(
                color = if (isDynamicColor) MiuixTheme.colorScheme.surfaceContainer else
                    if (isDarkMode) miuixSheetCardColorDark else Color.White,
                contentColor = MiuixTheme.colorScheme.onSurface
            )
        ) {
            MiuixSwitchWidget(
                title = stringResource(R.string.config_display_sdk_version),
                description = stringResource(
                    id = R.string.combined_description_format,
                    stringResource(id = R.string.config_display_sdk_version_desc),
                    stringResource(id = R.string.config_display_module_extra_info_desc)
                ),
                checked = displaySdk,
                onCheckedChange = { newValue ->
                    // Update local UI state
                    displaySdk = newValue
                    // Sync immediately to session config
                    session.config = session.config.copy(displaySdk = newValue)
                }
            )
            MiuixSwitchWidget(
                title = stringResource(R.string.config_display_size),
                description = stringResource(R.string.config_display_size_desc),
                checked = displaySize,
                onCheckedChange = { newValue ->
                    displaySize = newValue
                    session.config = session.config.copy(displaySize = newValue)
                }
            )
            MiuixSwitchWidget(
                title = stringResource(R.string.config_auto_delete),
                description = stringResource(R.string.config_auto_delete_desc),
                checked = autoDelete,
                onCheckedChange = { newValue ->
                    autoDelete = newValue
                    session.config = session.config.copy(autoDelete = newValue)
                }
            )

            if (DeviceConfig.currentManufacturer == Manufacturer.OPPO || DeviceConfig.currentManufacturer == Manufacturer.ONEPLUS) {
                MiuixSwitchWidget(
                    title = stringResource(R.string.installer_show_oem_special),
                    description = stringResource(R.string.installer_show_oem_special_desc),
                    // Read the effective value (either the repo setting or the temporary override)
                    checked = settings.showOPPOSpecial,
                    onCheckedChange = { newValue ->
                        // Dispatch action to update the temporary override in ViewModel
                        viewModel.dispatch(InstallerViewAction.SetTempShowOPPOSpecial(newValue))
                    }
                )
            }
        }
        Spacer(
            modifier = Modifier
                .navigationBarsPadding()
                .height(if (isGestureNavigation()) 24.dp else 0.dp)
        )
    }
}
