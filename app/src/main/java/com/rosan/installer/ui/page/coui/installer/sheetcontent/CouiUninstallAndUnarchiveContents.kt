// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.installer.sheetcontent

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.itos.coui.layout.Divider
import com.itos.coui.theme.CouiTheme
import com.rosan.installer.R
import com.rosan.installer.core.bitmask.hasFlag
import com.rosan.installer.domain.archive.model.UnarchiveErrorAction
import com.rosan.installer.domain.archive.model.UnarchiveStatus
import com.rosan.installer.domain.engine.model.install.UninstallFlags
import com.rosan.installer.ui.page.coui.installer.components.CouiAppInfoSlot
import com.rosan.installer.ui.page.coui.installer.components.CouiAppInfoState
import com.rosan.installer.ui.page.coui.widgets.CouiActionButton
import com.rosan.installer.ui.page.coui.widgets.CouiCard
import com.rosan.installer.ui.page.coui.widgets.CouiCheckboxItem
import com.rosan.installer.ui.page.coui.widgets.CouiErrorTextBlock
import com.rosan.installer.ui.page.coui.widgets.CouiProgressButton
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel

@Composable
fun CouiUninstallPrepareContent(
    viewModel: InstallerViewModel,
    onCancel: () -> Unit,
    onUninstall: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val info = uiState.uiUninstallInfo ?: return
    val uninstallFlags = uiState.config.uninstallFlags
    val isArchived = info.isArchived
    val scheme = CouiTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CouiAppInfoSlot(
            appInfo = CouiAppInfoState(
                icon = uiState.displayIcons[info.packageName],
                label = info.appLabel ?: "Unknown App",
                packageName = info.packageName,
            ),
        )
        Spacer(modifier = Modifier.height(20.dp))

        if (isArchived) {
            Text(
                text = stringResource(R.string.uninstall_archive_message),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontSize = 15.sp,
            )
        } else {
            CouiCard(modifier = Modifier.padding(vertical = 6.dp)) {
                CouiCheckboxItem(
                    title = stringResource(R.string.uninstall_keep_data),
                    description = stringResource(R.string.uninstall_keep_data_desc),
                    checked = uninstallFlags.hasFlag(UninstallFlags.DELETE_KEEP_DATA),
                    onCheckedChange = { isChecked ->
                        viewModel.dispatch(
                            InstallerViewAction.ToggleUninstallFlag(
                                flag = UninstallFlags.DELETE_KEEP_DATA,
                                enable = isChecked,
                            ),
                        )
                    },
                )
                Divider()
                CouiCheckboxItem(
                    title = stringResource(R.string.uninstall_all_users),
                    description = stringResource(R.string.uninstall_all_users_desc),
                    checked = uninstallFlags.hasFlag(UninstallFlags.DELETE_ALL_USERS),
                    onCheckedChange = { isChecked ->
                        viewModel.dispatch(
                            InstallerViewAction.ToggleUninstallFlag(
                                flag = UninstallFlags.DELETE_ALL_USERS,
                                enable = isChecked,
                            ),
                        )
                    },
                )
                Divider()
                CouiCheckboxItem(
                    title = stringResource(R.string.uninstall_delete_system_app),
                    description = stringResource(R.string.uninstall_delete_system_app_desc),
                    checked = uninstallFlags.hasFlag(UninstallFlags.DELETE_SYSTEM_APP),
                    onCheckedChange = { isChecked ->
                        viewModel.dispatch(
                            InstallerViewAction.ToggleUninstallFlag(
                                flag = UninstallFlags.DELETE_SYSTEM_APP,
                                enable = isChecked,
                            ),
                        )
                    },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CouiActionButton(
                text = stringResource(R.string.cancel),
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            )
            CouiActionButton(
                text = stringResource(if (isArchived) R.string.uninstall_archive else R.string.uninstall),
                onClick = onUninstall,
                destructive = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun CouiUninstallingContent(viewModel: InstallerViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val info = uiState.uiUninstallInfo ?: return

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CouiAppInfoSlot(
            appInfo = CouiAppInfoState(
                icon = uiState.displayIcons[info.packageName],
                label = info.appLabel ?: "Unknown App",
                packageName = info.packageName,
            ),
        )
        Spacer(modifier = Modifier.height(28.dp))
        CouiProgressButton(
            progress = 0f,
            onClick = {},
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        ) {
            val contentColor = LocalContentColor.current
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = contentColor,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.installer_uninstalling),
                color = contentColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun CouiUninstallSuccessContent(viewModel: InstallerViewModel, onClose: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val info = uiState.uiUninstallInfo ?: return
    val scheme = CouiTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CouiAppInfoSlot(
            appInfo = CouiAppInfoState(
                icon = uiState.displayIcons[info.packageName],
                label = info.appLabel ?: "Unknown App",
                packageName = info.packageName,
            ),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.uninstall_success_message),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = scheme.accentOnSurface,
        )
        Spacer(modifier = Modifier.height(24.dp))
        CouiActionButton(
            text = stringResource(R.string.finish),
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        )
    }
}

@Composable
fun CouiUninstallFailedContent(viewModel: InstallerViewModel, onClose: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val info = uiState.uiUninstallInfo ?: return

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CouiAppInfoSlot(
            appInfo = CouiAppInfoState(
                icon = uiState.displayIcons[info.packageName],
                label = info.appLabel ?: "Unknown App",
                packageName = info.packageName,
            ),
        )
        Spacer(modifier = Modifier.height(20.dp))
        CouiErrorTextBlock(
            error = uiState.error,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
        )
        Spacer(modifier = Modifier.height(16.dp))
        CouiActionButton(
            text = stringResource(R.string.close),
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        )
    }
}

@Composable
fun CouiUnarchivePrepareContent(
    appLabel: CharSequence,
    installerLabel: CharSequence,
    onCancel: () -> Unit,
    onRestore: () -> Unit,
) {
    val scheme = CouiTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.unarchive_title, appLabel),
            fontSize = 18.sp,
            color = scheme.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Text(
            text = stringResource(R.string.unarchive_message, installerLabel),
            fontSize = 15.sp,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CouiActionButton(
                text = stringResource(R.string.cancel),
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            )
            CouiActionButton(
                text = stringResource(R.string.unarchive_restore),
                onClick = onRestore,
                primary = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun CouiUnarchiveErrorContent(
    status: UnarchiveStatus,
    requiredBytes: Long,
    installerLabel: CharSequence?,
    onClose: () -> Unit,
    onPrimaryAction: () -> Unit,
) {
    val scheme = CouiTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = couiUnarchiveErrorTitle(status, installerLabel),
            fontSize = 18.sp,
            color = scheme.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Text(
            text = couiUnarchiveErrorMessage(status, requiredBytes, installerLabel),
            fontSize = 15.sp,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CouiActionButton(
                text = stringResource(R.string.close),
                onClick = onClose,
                modifier = Modifier.weight(1f),
            )
            if (status.primaryAction != UnarchiveErrorAction.CLOSE) {
                CouiActionButton(
                    text = stringResource(status.actionLabelResId),
                    onClick = onPrimaryAction,
                    primary = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun couiUnarchiveErrorTitle(status: UnarchiveStatus, installerLabel: CharSequence?): String = when (status) {
    UnarchiveStatus.InstallerDisabled,
    UnarchiveStatus.InstallerUninstalled,
    -> stringResource(status.titleResId, installerLabel ?: stringResource(R.string.installer_label_unknown))
    else -> stringResource(status.titleResId)
}

@Composable
private fun couiUnarchiveErrorMessage(
    status: UnarchiveStatus,
    requiredBytes: Long,
    installerLabel: CharSequence?,
): String {
    val context = LocalContext.current
    return when (status) {
        UnarchiveStatus.InsufficientStorage -> stringResource(
            status.messageResId,
            Formatter.formatShortFileSize(context, requiredBytes),
        )
        UnarchiveStatus.InstallerDisabled,
        UnarchiveStatus.InstallerUninstalled,
        -> stringResource(status.messageResId, installerLabel ?: stringResource(R.string.installer_label_unknown))
        else -> stringResource(status.messageResId)
    }
}
