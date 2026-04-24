// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.rosan.installer.ui.page.main.settings.preferred

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.data.engine.executor.PackageManagerUtil
import com.rosan.installer.domain.settings.model.BiometricAuthMode
import com.rosan.installer.domain.settings.model.HttpProfile
import com.rosan.installer.domain.settings.model.NamedPackage
import com.rosan.installer.domain.settings.model.RootMode
import com.rosan.installer.domain.settings.model.SharedUid
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.preferred.about.AboutAction
import com.rosan.installer.ui.page.main.settings.preferred.about.AboutViewModel
import com.rosan.installer.ui.page.main.settings.preferred.lab.LabSettingsAction
import com.rosan.installer.ui.page.main.settings.preferred.lab.LabSettingsViewModel
import com.rosan.installer.ui.page.main.settings.preferred.uninstaller.UninstallerSettingsAction
import com.rosan.installer.ui.page.main.settings.preferred.uninstaller.UninstallerSettingsViewModel
import com.rosan.installer.ui.page.main.widget.setting.BaseWidget
import com.rosan.installer.ui.page.main.widget.setting.DraggableManagedList
import com.rosan.installer.ui.page.main.widget.setting.DropDownMenuWidget
import com.rosan.installer.ui.page.main.widget.setting.SwitchWidget
import com.rosan.installer.util.hasFlag

/**
 * A DropDownMenuWidget for selecting the auto-clear time for success notifications.
 */
@Composable
fun AutoClearNotificationTimeWidget(
    currentValue: Int,
    onValueChange: (Int) -> Unit
) {
    val options = remember { listOf(0, 3, 5, 10, 15, 20, 30) }

    val selectedIndex = remember(currentValue, options) {
        options.indexOf(currentValue).coerceAtLeast(0)
    }
    val currentOption = options.getOrElse(selectedIndex) { 0 }

    val descriptionText = if (currentOption == 0) {
        stringResource(R.string.installer_settings_auto_clear_time_never_desc)
    } else {
        stringResource(
            R.string.installer_settings_auto_clear_time_seconds_format_desc,
            currentOption
        )
    }

    val dropdownItems = options.map { time ->
        if (time == 0) {
            stringResource(R.string.installer_settings_auto_clear_time_never)
        } else {
            stringResource(R.string.installer_settings_auto_clear_time_seconds_format, time)
        }
    }

    DropDownMenuWidget(
        icon = AppIcons.Timer,
        title = stringResource(id = R.string.installer_settings_auto_clear_success_notification),
        description = descriptionText,
        choice = selectedIndex,
        data = dropdownItems,
        onChoiceChange = { newIndex ->
            val newValue = options.getOrElse(newIndex) { 0 }
            if (currentValue != newValue) {
                onValueChange(newValue)
            }
        }
    )
}

@Composable
fun DisableAdbVerify(
    checked: Boolean,
    isError: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SwitchWidget(
        icon = AppIcons.DisableAdbVerify,
        title = stringResource(R.string.disable_adb_install_verify),
        description = if (!isError) stringResource(R.string.disable_adb_install_verify_desc)
        else stringResource(R.string.disable_adb_install_verify_not_support_dhizuku_desc),
        isError = isError,
        checked = checked,
        enabled = enabled,
        onCheckedChange = onCheckedChange
    )
}

/**
 * A setting pkg for requesting to ignore battery optimizations.
 *
 * @param checked Whether the app is currently ignoring battery optimizations.
 * @param onCheckedChange Callback invoked when the user toggles the switch.
 */
@Composable
fun IgnoreBatteryOptimizationSetting(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SwitchWidget(
        icon = AppIcons.BatteryOptimization,
        title = stringResource(R.string.ignore_battery_optimizations),
        description = if (enabled) stringResource(R.string.ignore_battery_optimizations_desc)
        else stringResource(R.string.ignore_battery_optimizations_desc_disabled),
        checked = checked,
        enabled = enabled,
        onCheckedChange = onCheckedChange
    )
}

@Composable
fun AutoLockInstaller(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SwitchWidget(
        icon = AppIcons.AutoLockDefault,
        title = stringResource(R.string.auto_lock_default_installer),
        description = stringResource(R.string.auto_lock_default_installer_desc),
        checked = checked,
        enabled = enabled,
        onCheckedChange = onCheckedChange
    )
}

@Composable
fun DefaultInstaller(
    lock: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    BaseWidget(
        icon = if (lock) AppIcons.LockDefault else AppIcons.UnlockDefault,
        title =
            stringResource(if (lock) R.string.lock_default_installer else R.string.unlock_default_installer),
        description =
            stringResource(if (lock) R.string.lock_default_installer_desc else R.string.unlock_default_installer_desc),
        enabled = enabled,
        onClick = onClick
    ) {}
}

@Composable
fun SettingsAboutItemWidget(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    imageContentDescription: String? = null,
    headlineContentText: String,
    supportingContentText: String? = null,
    supportingContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    BaseWidget(
        icon = imageVector,
        title = headlineContentText,
        description = supportingContentText,
        descriptionColor = supportingContentColor,
        onClick = onClick
    ) {
        // This pkg has no trailing content, so this lambda is empty.
    }
}

/**
 * A setting pkg that navigates to a secondary page, built upon BaseWidget.
 * It includes an icon, title, description, and a trailing arrow.
 *
 * @param icon The leading icon for the pkg.
 * @param title The main title text of the pkg.
 * @param description The supporting description text.
 * @param onClick The callback to be invoked when this pkg is clicked.
 */
@Composable
fun SettingsNavigationItemWidget(
    icon: ImageVector? = null,
    iconPlaceholder: Boolean = true,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    // Call the BaseWidget and pass the parameters accordingly.
    BaseWidget(
        icon = icon,
        iconPlaceholder = iconPlaceholder,
        title = title,
        description = description,
        onClick = onClick
    ) {
        // The content lambda of BaseWidget is used for the trailing content.
        // We place the navigation arrow Icon here.
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null
        )
    }
}

@Composable
fun BottomSheetContent(
    title: String,
    hasUpdate: Boolean,
    canDirectUpdate: Boolean = true,
    onDirectUpdateClick: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 0.dp, 16.dp, 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        if (hasUpdate && canDirectUpdate) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onDirectUpdateClick()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = AppIcons.Update,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.get_update_directly),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                uriHandler.openUri("https://github.com/wxxsfxyzm/InstallerX-Revived/releases")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_github),
                contentDescription = "GitHub Icon",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "GitHub")
        }
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                uriHandler.openUri("https://t.me/installerx_revived")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_telegram),
                contentDescription = "Telegram Icon",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Telegram")
        }
        Spacer(modifier = Modifier.size(60.dp))
    }
}

@Composable
fun ManagedPackagesWidget(
    modifier: Modifier = Modifier,
    noContentTitle: String,
    packages: List<NamedPackage>,
    infoText: String? = null,
    isInfoVisible: Boolean = false,
    infoColor: Color = MaterialTheme.colorScheme.primary,
    onAddPackage: (NamedPackage) -> Unit,
    onRemovePackage: (NamedPackage) -> Unit,
    onMovePackage: (fromIndex: Int, toIndex: Int) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    DraggableManagedList(
        modifier = modifier,
        items = packages,
        itemKey = { it.packageName },
        itemName = { it.name },
        itemDescription = { it.packageName },
        leadingIcon = AppIcons.Android,
        onMove = onMovePackage,
        onRemove = onRemovePackage,
        noContentTitle = noContentTitle,
        bottomBarContent = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedVisibility(visible = isInfoVisible && !infoText.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(infoColor.copy(alpha = 0.1f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = infoText ?: "", color = infoColor, style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.add))
                }
            }
        }
    )

    if (showAddDialog) {
        AddPackageDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = {
                onAddPackage(it)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ManagedUidsWidget(
    modifier: Modifier = Modifier,
    noContentTitle: String,
    uids: List<SharedUid>,
    onAddUid: (SharedUid) -> Unit,
    onRemoveUid: (SharedUid) -> Unit,
    onMoveUid: (fromIndex: Int, toIndex: Int) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    DraggableManagedList(
        modifier = modifier,
        items = uids,
        itemKey = { it.uidValue },
        itemName = { it.uidName },
        itemDescription = { "UID: ${it.uidValue}" },
        leadingIcon = AppIcons.BugReport,
        onMove = onMoveUid,
        onRemove = onRemoveUid,
        noContentTitle = noContentTitle,
        bottomBarContent = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.add))
                }
            }
        }
    )

    if (showAddDialog) {
        AddUidDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { uid ->
                onAddUid(uid)
                showAddDialog = false
            }
        )
    }
}

/**
 * Widget for selecting the Root Implementation (Magisk/KernelSU/APatch).
 * Mimics the logic from MiuixRootImplementationDialog but uses DropDownMenuWidget.
 */
@Composable
fun LabRootImplementationWidget(viewModel: LabSettingsViewModel) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val currentRootImpl = uiState.labRootMode

    val data = remember {
        mapOf(
            RootMode.Magisk to "Magisk",
            RootMode.KernelSU to "KernelSU",
            RootMode.APatch to "APatch"
        )
    }

    val options = data.values.toList()
    val keys = data.keys.toList()

    val selectedIndex = keys.indexOf(currentRootImpl).coerceAtLeast(0)

    DropDownMenuWidget(
        icon = AppIcons.RootMethod,
        title = stringResource(R.string.lab_module_select_root_impl),
        description = options.getOrNull(selectedIndex),
        choice = selectedIndex,
        data = options,
        onChoiceChange = { newIndex ->
            keys.getOrNull(newIndex)?.let { impl ->
                viewModel.dispatch(LabSettingsAction.LabChangeRootImplementation(impl))
            }
        }
    )
}

@Composable
fun LabHttpProfileWidget(viewModel: LabSettingsViewModel) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val profiles = remember {
        listOf(
            HttpProfile.ALLOW_SECURE,
            HttpProfile.ALLOW_LOCAL,
            HttpProfile.ALLOW_ALL
        )
    }
    val options = profiles.map { profile ->
        when (profile) {
            HttpProfile.ALLOW_SECURE -> stringResource(R.string.lab_http_profile_secure)
            HttpProfile.ALLOW_LOCAL -> stringResource(R.string.lab_http_profile_local)
            HttpProfile.ALLOW_ALL -> stringResource(R.string.lab_http_profile_all)
        }
    }

    val currentIndex = profiles.indexOf(uiState.labHttpProfile).coerceAtLeast(0)

    DropDownMenuWidget(
        icon = Icons.Default.Security,
        title = stringResource(R.string.lab_http_profile),
        description = options.getOrNull(currentIndex),
        choice = currentIndex,
        data = options,
        onChoiceChange = { index ->
            val selectedProfile = profiles.getOrElse(index) { HttpProfile.ALLOW_SECURE }
            viewModel.dispatch(LabSettingsAction.LabChangeHttpProfile(selectedProfile))
        }
    )
}

/**
 * An AlertDialog for adding a new NamedPackage.
 */
@Composable
private fun AddPackageDialog(
    onDismiss: () -> Unit,
    onConfirm: (NamedPackage) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }
    val isConfirmEnabled = name.isNotBlank() && packageName.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.config_add_new_package)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.config_name)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text(stringResource(R.string.config_package_name)) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(NamedPackage(name, packageName)) },
                enabled = isConfirmEnabled
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * An AlertDialog for adding a new SharedUid.
 */
@Composable
private fun AddUidDialog(
    onDismiss: () -> Unit,
    onConfirm: (SharedUid) -> Unit
) {
    var uidName by remember { mutableStateOf("") }
    var uidValueString by remember { mutableStateOf("") }

    // Confirm button is enabled if both name and value are not blank
    val isConfirmEnabled = uidName.isNotBlank() && uidValueString.toIntOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.config_add_new_shared_uid)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = uidName,
                    onValueChange = { uidName = it },
                    label = { Text(stringResource(R.string.config_shared_uid_name)) }, // "Shared UID 名称"
                    singleLine = true
                )
                OutlinedTextField(
                    value = uidValueString,
                    onValueChange = { uidValueString = it },
                    label = { Text(stringResource(R.string.config_shared_uid_value)) }, // "Shared UID 值"
                    singleLine = true,
                    // Set the keyboard type to Number
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Convert uidValueString to Int before creating SharedUid
                    val uidValue = uidValueString.toInt()
                    onConfirm(SharedUid(uidName, uidValue))
                },
                enabled = isConfirmEnabled
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * An AlertDialog to confirm the deletion of an pkg.
 */
@Composable
private fun DeleteNamedPackageConfirmationDialog(
    item: NamedPackage,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.config_confirm_deletion)) },
        text = { Text(stringResource(R.string.config_confirm_deletion_desc, item.name)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * An AlertDialog to confirm the deletion of an pkg.
 */
@Composable
private fun DeleteSharedUidConfirmationDialog(
    item: SharedUid,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.config_confirm_deletion)) },
        text = { Text(stringResource(R.string.config_confirm_deletion_desc, item.uidName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun UninstallKeepDataWidget(viewModel: UninstallerSettingsViewModel) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    SwitchWidget(
        icon = AppIcons.Save,
        title = stringResource(id = R.string.uninstall_keep_data),
        description = stringResource(id = R.string.uninstall_keep_data_desc),
        checked = uiState.uninstallFlags.hasFlag(PackageManagerUtil.DELETE_KEEP_DATA),
        onCheckedChange = {
            viewModel.dispatch(UninstallerSettingsAction.ToggleGlobalUninstallFlag(PackageManagerUtil.DELETE_KEEP_DATA, it))
        }
    )
}

@Composable
fun UninstallForAllUsersWidget(viewModel: UninstallerSettingsViewModel) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    SwitchWidget(
        icon = AppIcons.InstallForAllUsers,
        title = stringResource(id = R.string.uninstall_all_users),
        description = stringResource(id = R.string.uninstall_all_users_desc),
        checked = uiState.uninstallFlags.hasFlag(PackageManagerUtil.DELETE_ALL_USERS),
        onCheckedChange = {
            viewModel.dispatch(UninstallerSettingsAction.ToggleGlobalUninstallFlag(PackageManagerUtil.DELETE_ALL_USERS, it))
        }
    )
}

@Composable
fun UninstallSystemAppWidget(viewModel: UninstallerSettingsViewModel) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    SwitchWidget(
        icon = AppIcons.BugReport,
        title = stringResource(id = R.string.uninstall_delete_system_app),
        description = stringResource(id = R.string.uninstall_delete_system_app_desc),
        checked = uiState.uninstallFlags.hasFlag(PackageManagerUtil.DELETE_SYSTEM_APP),
        onCheckedChange = {
            viewModel.dispatch(
                UninstallerSettingsAction.ToggleGlobalUninstallFlag(
                    PackageManagerUtil.DELETE_SYSTEM_APP,
                    it
                )
            )
        }
    )
}

@Composable
fun DataInstallerBiometricAuthWidget(
    currentMode: BiometricAuthMode,
    onModeChange: (BiometricAuthMode) -> Unit
) {
    val modes = remember {
        listOf(
            BiometricAuthMode.Disable,
            BiometricAuthMode.Enable,
            BiometricAuthMode.FollowConfig
        )
    }

    val options = modes.map { mode ->
        when (mode) {
            BiometricAuthMode.Disable -> stringResource(R.string.installer_biometric_auth_mode_disable)
            BiometricAuthMode.Enable -> stringResource(R.string.installer_biometric_auth_mode_enable)
            BiometricAuthMode.FollowConfig -> stringResource(R.string.installer_biometric_auth_mode_follow_config)
        }
    }

    val selectedIndex = modes.indexOf(currentMode).coerceAtLeast(0)
    val dynamicDescription = when (modes[selectedIndex]) {
        BiometricAuthMode.Disable -> stringResource(R.string.installer_biometric_auth_mode_disable_desc)
        BiometricAuthMode.Enable -> stringResource(R.string.installer_biometric_auth_mode_enable_desc)
        BiometricAuthMode.FollowConfig -> stringResource(R.string.installer_biometric_auth_mode_follow_config_desc)
    }

    DropDownMenuWidget(
        icon = AppIcons.BiometricAuth,
        title = stringResource(R.string.installer_settings_require_biometric_auth),
        description = dynamicDescription,
        choice = selectedIndex,
        data = options,
        onChoiceChange = { index ->
            val selectedMode = modes.getOrElse(index) { BiometricAuthMode.FollowConfig }
            if (currentMode != selectedMode) {
                onModeChange(selectedMode)
            }
        }
    )
}

@Composable
fun UninstallRequireBiometricAuthWidget(viewModel: UninstallerSettingsViewModel) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    if (BiometricManager
            .from(LocalContext.current)
            .canAuthenticate(BIOMETRIC_WEAK or BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
    ) {
        SwitchWidget(
            icon = AppIcons.BiometricAuth,
            title = stringResource(R.string.uninstaller_settings_require_biometric_auth),
            description = stringResource(R.string.uninstaller_settings_require_biometric_auth_desc),
            checked = uiState.uninstallerRequireBiometricAuth,
            onCheckedChange = {
                viewModel.dispatch(UninstallerSettingsAction.ChangeBiometricAuth(it))
            }
        )
    }
}

@Composable
fun ExportLogsWidget(viewModel: AboutViewModel) {
    BaseWidget(
        icon = AppIcons.BugReport,
        title = stringResource(R.string.export_logs),
        description = stringResource(R.string.export_logs_desc),
        onClick = { viewModel.dispatch(AboutAction.ShareLog) }
    ) {}
}