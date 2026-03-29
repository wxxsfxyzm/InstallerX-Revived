// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.installer.sheetcontent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.data.engine.executor.PackageManagerUtil
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.miuix.widgets.MiuixCheckboxWidget
import com.rosan.installer.ui.theme.InstallerTheme
import com.rosan.installer.ui.theme.miuixSheetCardColorDark
import com.rosan.installer.ui.util.isGestureNavigation
import com.rosan.installer.util.hasFlag
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.isDynamicColor

@Composable
fun UninstallPrepareContent(
    viewModel: InstallerViewModel,
    onCancel: () -> Unit,
    onUninstall: () -> Unit
) {
    val isDarkMode = InstallerTheme.isDark
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val info = uiState.uiUninstallInfo ?: return
    val uninstallFlags = uiState.config.uninstallFlags

    val deleteKeepData = uninstallFlags.hasFlag(PackageManagerUtil.DELETE_KEEP_DATA)
    val deleteAllUsers = uninstallFlags.hasFlag(PackageManagerUtil.DELETE_ALL_USERS)
    val deleteSystemApp = uninstallFlags.hasFlag(PackageManagerUtil.DELETE_SYSTEM_APP)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppInfoSlot(
            appInfo = AppInfoState(
                icon = uiState.displayIcons[info.packageName],
                label = info.appLabel ?: "Unknown App",
                packageName = info.packageName
            )
        )
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardColors(
                color = if (isDynamicColor) MiuixTheme.colorScheme.surfaceContainer else
                    if (isDarkMode) miuixSheetCardColorDark else Color.White,
                contentColor = MiuixTheme.colorScheme.onSurface
            )
        ) {
            MiuixCheckboxWidget(
                title = stringResource(id = R.string.uninstall_keep_data),
                description = stringResource(id = R.string.uninstall_keep_data_desc),
                checked = deleteKeepData,
                onCheckedChange = { isChecked ->
                    viewModel.dispatch(
                        InstallerViewAction.ToggleUninstallFlag(
                            flag = PackageManagerUtil.DELETE_KEEP_DATA,
                            enable = isChecked
                        )
                    )
                }
            )

            MiuixCheckboxWidget(
                title = stringResource(id = R.string.uninstall_all_users),
                description = stringResource(id = R.string.uninstall_all_users_desc),
                checked = deleteAllUsers,
                onCheckedChange = { isChecked ->
                    viewModel.dispatch(
                        InstallerViewAction.ToggleUninstallFlag(
                            flag = PackageManagerUtil.DELETE_ALL_USERS,
                            enable = isChecked
                        )
                    )
                }
            )

            MiuixCheckboxWidget(
                title = stringResource(id = R.string.uninstall_delete_system_app),
                description = stringResource(id = R.string.uninstall_delete_system_app_desc),
                checked = deleteSystemApp,
                onCheckedChange = { isChecked ->
                    viewModel.dispatch(
                        InstallerViewAction.ToggleUninstallFlag(
                            flag = PackageManagerUtil.DELETE_SYSTEM_APP,
                            enable = isChecked
                        )
                    )
                }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 24.dp, bottom = if (isGestureNavigation()) 24.dp else 0.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                text = stringResource(R.string.cancel),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColors(
                    color = if (isDynamicColor) MiuixTheme.colorScheme.secondaryContainer else MiuixTheme.colorScheme.secondaryVariant,
                    textColor = if (isDynamicColor) MiuixTheme.colorScheme.onSecondaryContainer else MiuixTheme.colorScheme.onSecondaryVariant
                ),
                onClick = onCancel,
            )
            TextButton(
                text = stringResource(R.string.uninstall),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColors(
                    color = if (isDynamicColor) MiuixTheme.colorScheme.secondaryContainer else MiuixTheme.colorScheme.secondaryVariant,
                    textColor = MaterialTheme.colorScheme.error
                ),
                onClick = onUninstall,
            )
        }
    }
}
