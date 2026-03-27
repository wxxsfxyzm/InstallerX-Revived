// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.widget.setting

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.More
import androidx.compose.material.icons.twotone.Downloading
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.material.icons.twotone.Memory
import androidx.compose.material.icons.twotone.Speed
import androidx.compose.material.icons.twotone.Terminal
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.DexoptMode
import com.rosan.installer.domain.settings.model.InstallMode
import com.rosan.installer.domain.settings.model.InstallReason
import com.rosan.installer.domain.settings.model.PackageSource
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.config.edit.EditViewAction
import com.rosan.installer.ui.page.main.settings.config.edit.EditViewState
import com.rosan.installer.ui.util.isDhizukuActive
import org.koin.compose.koinInject

@Composable
fun DataNameWidget(
    state: EditViewState,
    dispatch: (EditViewAction) -> Unit,
    trailingContent: @Composable (() -> Unit) = {}
) {
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .focusable(),
        leadingIcon = {
            Icon(imageVector = Icons.TwoTone.Edit, contentDescription = null)
        },
        label = {
            Text(text = stringResource(id = R.string.config_name))
        },
        value = state.data.name,
        onValueChange = {
            dispatch(EditViewAction.ChangeDataName(it))
        },
        singleLine = true,
        isError = state.data.errorName
    )
    trailingContent()
}

@Composable
fun DataDescriptionWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    TextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .focusable(),
        leadingIcon = {
            Icon(imageVector = Icons.AutoMirrored.TwoTone.More, contentDescription = null)
        },
        label = {
            Text(text = stringResource(id = R.string.config_description))
        },
        value = state.data.description,
        onValueChange = { dispatch(EditViewAction.ChangeDataDescription(it)) },
        maxLines = 8,
    )
}

@Composable
fun DataAuthorizerWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    val capabilityProvider = koinInject<DeviceCapabilityProvider>()
    val stateAuthorizer = state.data.authorizer
    val globalAuthorizer = state.globalAuthorizer
    val isSessionInstallSupported = capabilityProvider.isSessionInstallSupported
    val data = buildMap {
        put(
            Authorizer.Global, stringResource(
                R.string.config_authorizer_global_desc,
                when (globalAuthorizer) {
                    Authorizer.None -> stringResource(R.string.config_authorizer_none)
                    Authorizer.Root -> stringResource(R.string.config_authorizer_root)
                    Authorizer.Shizuku -> stringResource(R.string.config_authorizer_shizuku)
                    Authorizer.Dhizuku -> stringResource(R.string.config_authorizer_dhizuku)
                    Authorizer.Customize -> stringResource(R.string.config_authorizer_customize)
                    else -> stringResource(R.string.config_authorizer_global)
                }
            )
        )
        if (isSessionInstallSupported)
            put(Authorizer.None, stringResource(R.string.config_authorizer_none))
        put(Authorizer.Root, stringResource(R.string.config_authorizer_root))
        put(Authorizer.Shizuku, stringResource(R.string.config_authorizer_shizuku))
        put(Authorizer.Dhizuku, stringResource(R.string.config_authorizer_dhizuku))
        put(Authorizer.Customize, stringResource(R.string.config_authorizer_customize))
    }
    DropDownMenuWidget(
        icon = Icons.TwoTone.Memory,
        title = stringResource(R.string.config_authorizer),
        description = if (data.containsKey(stateAuthorizer)) data[stateAuthorizer] else null,
        choice = data.keys.toList().indexOf(stateAuthorizer),
        data = data.values.toList(),
    ) { index ->
        data.keys.toList().getOrNull(index)?.let {
            dispatch(EditViewAction.ChangeDataAuthorizer(it))
        }
    }
}

@Composable
fun DataCustomizeAuthorizerWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    if (!state.data.authorizerCustomize) return
    val customizeAuthorizer = state.data.customizeAuthorizer
    TextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .focusable(),
        leadingIcon = {
            Icon(imageVector = Icons.TwoTone.Terminal, contentDescription = null)
        },
        label = {
            Text(text = stringResource(R.string.config_customize_authorizer))
        },
        value = customizeAuthorizer,
        onValueChange = { dispatch(EditViewAction.ChangeDataCustomizeAuthorizer(it)) },
        maxLines = 8,
        isError = state.data.errorCustomizeAuthorizer
    )
}

@Composable
fun DataInstallModeWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    val stateInstallMode = state.data.installMode
    val globalInstallMode = state.globalInstallMode
    val data = mapOf(
        InstallMode.Global to stringResource(
            R.string.config_install_mode_global_desc,
            when (globalInstallMode) {
                InstallMode.Dialog -> stringResource(R.string.config_install_mode_dialog)
                InstallMode.AutoDialog -> stringResource(R.string.config_install_mode_auto_dialog)
                InstallMode.Notification -> stringResource(R.string.config_install_mode_notification)
                InstallMode.AutoNotification -> stringResource(R.string.config_install_mode_auto_notification)
                InstallMode.Ignore -> stringResource(R.string.config_install_mode_ignore)
                else -> stringResource(R.string.config_install_mode_global)
            }
        ),
        InstallMode.Dialog to stringResource(R.string.config_install_mode_dialog),
        InstallMode.AutoDialog to stringResource(R.string.config_install_mode_auto_dialog),
        InstallMode.Notification to stringResource(R.string.config_install_mode_notification),
        InstallMode.AutoNotification to stringResource(R.string.config_install_mode_auto_notification),
        InstallMode.Ignore to stringResource(R.string.config_install_mode_ignore),
    )
    DropDownMenuWidget(
        icon = Icons.TwoTone.Downloading,
        title = stringResource(R.string.config_install_mode),
        description = if (data.containsKey(stateInstallMode)) data[stateInstallMode] else null,
        choice = data.keys.toList().indexOf(stateInstallMode),
        data = data.values.toList(),
    ) { index ->
        data.keys.toList().getOrNull(index)?.let {
            dispatch(EditViewAction.ChangeDataInstallMode(it))
        }
    }
}

@Composable
fun DataShowToastWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit, isM3E: Boolean = true) {
    SwitchWidget(
        icon = AppIcons.Toast,
        title = stringResource(id = R.string.config_install_show_toast),
        description = stringResource(R.string.config_install_show_toast_desc),
        checked = state.data.showToast,
        isM3E = isM3E,
        onCheckedChange = {
            dispatch(EditViewAction.ChangeDataShowToast(it))
        }
    )
}

@Composable
fun DataInstallReasonWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit, isM3E: Boolean = true) {
    val enableCustomizeInstallReason = state.data.enableCustomizeInstallReason
    val currentInstallReason = state.data.installReason

    Column {
        SwitchWidget(
            icon = AppIcons.InstallReason,
            title = stringResource(id = R.string.config_customize_install_reason),
            description = stringResource(id = R.string.config_customize_install_reason_desc),
            checked = enableCustomizeInstallReason,
            isM3E = isM3E,
            onCheckedChange = {
                dispatch(EditViewAction.ChangeDataEnableCustomizeInstallReason(it))
            }
        )

        AnimatedVisibility(
            visible = enableCustomizeInstallReason,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            val data = mapOf(
                InstallReason.UNKNOWN to stringResource(R.string.config_install_reason_unknown),
                InstallReason.POLICY to stringResource(R.string.config_install_reason_policy),
                InstallReason.DEVICE_RESTORE to stringResource(R.string.config_install_reason_device_restore),
                InstallReason.DEVICE_SETUP to stringResource(R.string.config_install_reason_device_setup),
                InstallReason.USER to stringResource(R.string.config_install_reason_user)
            )

            DropDownMenuWidget(
                title = stringResource(R.string.config_install_reason),
                description = data[currentInstallReason],
                choice = data.keys.toList().indexOf(currentInstallReason),
                data = data.values.toList(),
            ) { index ->
                data.keys.toList().getOrNull(index)?.let { reason ->
                    dispatch(EditViewAction.ChangeDataInstallReason(reason))
                }
            }
        }
    }
}

@Composable
fun DataPackageSourceWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit, isM3E: Boolean = true) {
    val stateAuthorizer = state.data.authorizer
    val globalAuthorizer = state.globalAuthorizer
    val enableCustomizePackageSource = state.data.enableCustomizePackageSource
    val currentSource = state.data.packageSource

    val description =
        if (isDhizukuActive(stateAuthorizer, globalAuthorizer)) stringResource(R.string.dhizuku_cannot_set_package_source_desc)
        else stringResource(id = R.string.config_customize_package_source_desc)

    Column {
        SwitchWidget(
            icon = AppIcons.InstallPackageSource,
            title = stringResource(id = R.string.config_customize_package_source),
            description = description,
            checked = enableCustomizePackageSource,
            enabled = !isDhizukuActive(stateAuthorizer, globalAuthorizer),
            isError = isDhizukuActive(stateAuthorizer, globalAuthorizer),
            isM3E = isM3E,
            onCheckedChange = {
                dispatch(EditViewAction.ChangeDataEnableCustomizePackageSource(it))
            }
        )

        AnimatedVisibility(
            visible = enableCustomizePackageSource,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            val data = mapOf(
                PackageSource.UNSPECIFIED to stringResource(R.string.config_package_source_unspecified),
                PackageSource.OTHER to stringResource(R.string.config_package_source_other),
                PackageSource.STORE to stringResource(R.string.config_package_source_store),
                PackageSource.LOCAL_FILE to stringResource(R.string.config_package_source_local_file),
                PackageSource.DOWNLOADED_FILE to stringResource(R.string.config_package_source_downloaded_file),
            )
            DropDownMenuWidget(
                title = stringResource(R.string.config_package_source),
                description = data[currentSource],
                choice = data.keys.toList().indexOf(currentSource),
                data = data.values.toList(),
            ) { index ->
                data.keys.toList().getOrNull(index)?.let { source ->
                    dispatch(EditViewAction.ChangeDataPackageSource(source))
                }
            }
        }
    }
}

@Composable
fun DataInstallRequesterWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit, isM3E: Boolean = true) {
    val stateData = state.data
    val enableCustomize = stateData.enableCustomizeInstallRequester
    val packageName = stateData.installRequester
    val uid = stateData.installRequesterUid

    val isPackageNotFound = packageName.isNotEmpty() && uid == null
    val isError = stateData.errorInstallRequester

    val description =
        if (isError) stringResource(R.string.config_declare_install_requester_error_desc)
        else stringResource(R.string.config_declare_install_requester_desc)

    Column {
        SwitchWidget(
            icon = AppIcons.InstallSource,
            title = stringResource(id = R.string.config_declare_install_requester),
            description = description,
            checked = enableCustomize,
            onCheckedChange = {
                dispatch(EditViewAction.ChangeDataEnableCustomizeInstallRequester(it))
            },
            isM3E = isM3E,
            isError = isError
        )

        AnimatedVisibility(
            visible = enableCustomize,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 16.dp)
                    .focusable(),
                value = packageName,
                onValueChange = {
                    dispatch(EditViewAction.ChangeDataInstallRequester(it))
                },
                label = { Text(text = stringResource(id = R.string.config_install_requester)) },
                leadingIcon = {
                    Icon(imageVector = AppIcons.InstallSourceInput, contentDescription = null)
                },
                singleLine = true,
                isError = isPackageNotFound || (isError && packageName.isEmpty()),
                supportingText = {
                    if (packageName.isNotEmpty()) {
                        if (uid != null) {
                            Text(
                                text = "UID: $uid",
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.config_error_package_not_found),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    } else stringResource(R.string.config_error_package_name_empty)
                }
            )
        }
    }
}

@Composable
fun DataDeclareInstallerWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit, isM3E: Boolean = true) {
    val stateAuthorizer = state.data.authorizer
    val globalAuthorizer = state.globalAuthorizer

    val description =
        if (isDhizukuActive(stateAuthorizer, globalAuthorizer)) stringResource(R.string.dhizuku_cannot_set_installer_desc)
        else stringResource(id = R.string.config_declare_installer_desc)

    SwitchWidget(
        icon = AppIcons.InstallSource,
        title = stringResource(id = R.string.config_declare_installer),
        checked = state.data.declareInstaller,
        onCheckedChange = { dispatch(EditViewAction.ChangeDataDeclareInstaller(it)) },
        isM3E = isM3E,
        description = description,
        enabled = !isDhizukuActive(stateAuthorizer, globalAuthorizer),
        isError = isDhizukuActive(stateAuthorizer, globalAuthorizer)
    )

    AnimatedVisibility(
        visible = state.data.declareInstaller,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        DataInstallerWidget(state, dispatch)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataInstallerWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    val stateData = state.data
    val managedPackages = state.managedInstallerPackages
    val currentInstaller = stateData.installer
    var expanded by remember { mutableStateOf(false) }

    val matchingPackage = remember(currentInstaller, managedPackages) {
        managedPackages.find { it.packageName == currentInstaller }
    }

    AnimatedVisibility(
        visible = stateData.declareInstaller,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp)
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusable()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                value = currentInstaller,
                onValueChange = {
                    dispatch(EditViewAction.ChangeDataInstaller(it))
                },
                label = { Text(text = stringResource(id = R.string.config_installer)) },
                leadingIcon = {
                    Icon(imageVector = AppIcons.InstallSourceInput, contentDescription = null)
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                supportingText = {
                    matchingPackage?.let {
                        Text(stringResource(R.string.config_installer_matches, it.name))
                    }
                },
                singleLine = true,
                isError = stateData.errorInstaller
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (managedPackages.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.config_no_managed_packages_to_suggest)) },
                        onClick = { expanded = false },
                        enabled = false
                    )
                } else {
                    managedPackages.forEach { item ->
                        val isSelected = currentInstaller == item.packageName
                        DropdownMenuItem(
                            text = { Text("${item.name} (${item.packageName})") },
                            onClick = {
                                dispatch(EditViewAction.ChangeDataInstaller(item.packageName))
                                expanded = false
                            },
                            colors = if (isSelected) MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.primary
                            ) else MenuDefaults.itemColors()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DataUserWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit, isM3E: Boolean = true) {
    val stateAuthorizer = state.data.authorizer
    val globalAuthorizer = state.globalAuthorizer
    val enableCustomizeUser = state.data.enableCustomizeUser
    val targetUserId = state.data.targetUserId
    val availableUsers = state.availableUsers

    val description =
        if (isDhizukuActive(stateAuthorizer, globalAuthorizer)) stringResource(R.string.dhizuku_cannot_set_user_desc)
        else stringResource(id = R.string.config_customize_user_desc)

    Column {
        SwitchWidget(
            icon = AppIcons.InstallUser,
            title = stringResource(id = R.string.config_customize_user),
            description = description,
            checked = enableCustomizeUser,
            enabled = !isDhizukuActive(stateAuthorizer, globalAuthorizer),
            isError = isDhizukuActive(stateAuthorizer, globalAuthorizer),
            isM3E = isM3E,
            onCheckedChange = {
                dispatch(EditViewAction.ChangeDataCustomizeUser(it))
            }
        )

        AnimatedVisibility(
            visible = enableCustomizeUser,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            val descriptionUser = availableUsers[targetUserId] ?: stringResource(R.string.config_user_not_found)
            DropDownMenuWidget(
                title = stringResource(R.string.config_target_user),
                description = descriptionUser,
                choice = availableUsers.keys.toList().indexOf(targetUserId),
                data = availableUsers.values.toList(),
            ) { index ->
                availableUsers.keys.toList().getOrNull(index)?.let {
                    dispatch(EditViewAction.ChangeDataTargetUserId(it))
                }
            }
        }
    }
}

@Composable
fun DataManualDexoptWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit, isM3E: Boolean = true) {
    val stateAuthorizer = state.data.authorizer
    val globalAuthorizer = state.globalAuthorizer

    val description =
        if (isDhizukuActive(stateAuthorizer, globalAuthorizer)) stringResource(R.string.dhizuku_cannot_set_dexopt_desc)
        else stringResource(R.string.config_manual_dexopt_desc)

    SwitchWidget(
        icon = Icons.TwoTone.Speed,
        title = stringResource(id = R.string.config_manual_dexopt),
        description = description,
        checked = state.data.enableManualDexopt,
        enabled = !isDhizukuActive(stateAuthorizer, globalAuthorizer),
        isError = isDhizukuActive(stateAuthorizer, globalAuthorizer),
        isM3E = isM3E,
        onCheckedChange = {
            dispatch(EditViewAction.ChangeDataEnableManualDexopt(it))
        }
    )

    AnimatedVisibility(
        visible = state.data.enableManualDexopt,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        val currentMode = state.data.dexoptMode
        val data = mapOf(
            DexoptMode.Verify to stringResource(R.string.config_dexopt_mode_verify),
            DexoptMode.SpeedProfile to stringResource(R.string.config_dexopt_mode_speed_profile),
            DexoptMode.Speed to stringResource(R.string.config_dexopt_mode_speed),
            DexoptMode.Everything to stringResource(R.string.config_dexopt_mode_everything),
        )
        Column {
            SwitchWidget(
                title = stringResource(id = R.string.config_force_dexopt),
                description = stringResource(id = R.string.config_force_dexopt_desc),
                checked = state.data.forceDexopt,
                isM3E = isM3E,
                onCheckedChange = {
                    dispatch(EditViewAction.ChangeDataForceDexopt(it))
                }
            )
            DropDownMenuWidget(
                title = stringResource(R.string.config_dexopt_mode),
                description = data[currentMode],
                choice = data.keys.toList().indexOf(currentMode),
                data = data.values.toList(),
            ) { index ->
                data.keys.toList().getOrNull(index)?.let { mode ->
                    dispatch(EditViewAction.ChangeDataDexoptMode(mode))
                }
            }
        }
    }
}

@Composable
fun DataAutoDeleteWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit, isM3E: Boolean = true) {
    SwitchWidget(
        icon = AppIcons.Delete,
        title = stringResource(id = R.string.config_auto_delete),
        description = stringResource(id = R.string.config_auto_delete_desc),
        checked = state.data.autoDelete,
        isM3E = isM3E,
        onCheckedChange = { dispatch(EditViewAction.ChangeDataAutoDelete(it)) }
    )

    AnimatedVisibility(
        visible = state.data.autoDelete,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        SwitchWidget(
            title = stringResource(id = R.string.config_auto_delete_zip),
            description = stringResource(id = R.string.config_auto_delete_zip_desc),
            checked = state.data.autoDeleteZip,
            isM3E = isM3E,
            onCheckedChange = {
                dispatch(EditViewAction.ChangeDataZipAutoDelete(it))
            }
        )
    }
}

@Composable
fun DisplaySdkWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit, isM3E: Boolean = true) {
    SwitchWidget(
        icon = AppIcons.Info,
        title = stringResource(id = R.string.config_display_sdk_version),
        description = stringResource(id = R.string.config_display_sdk_version_desc),
        checked = state.data.displaySdk,
        isM3E = isM3E,
        onCheckedChange = { dispatch(EditViewAction.ChangeDisplaySdk(it)) }
    )
}

@Composable
fun DisplaySizeWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit, isM3E: Boolean = true) {
    SwitchWidget(
        icon = AppIcons.ShowSize,
        title = stringResource(id = R.string.config_display_size),
        description = stringResource(id = R.string.config_display_size_desc),
        checked = state.data.displaySize,
        isM3E = isM3E,
        onCheckedChange = { dispatch(EditViewAction.ChangeDisplaySize(it)) }
    )
}

@Composable
fun DataForAllUserWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit, isM3E: Boolean = true) {
    SwitchWidget(
        icon = AppIcons.InstallForAllUsers,
        title = stringResource(id = R.string.config_all_users),
        description = stringResource(id = R.string.config_all_users_desc),
        checked = state.data.forAllUser,
        isM3E = isM3E,
        onCheckedChange = { dispatch(EditViewAction.ChangeDataForAllUser(it)) }
    )
}

@Composable
fun DataAllowTestOnlyWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit, isM3E: Boolean = true) {
    SwitchWidget(
        icon = AppIcons.BugReport,
        title = stringResource(id = R.string.config_allow_test),
        description = stringResource(id = R.string.config_allow_test_desc),
        checked = state.data.allowTestOnly,
        isM3E = isM3E,
        onCheckedChange = { dispatch(EditViewAction.ChangeDataAllowTestOnly(it)) }
    )
}

@Composable
fun DataAllowDowngradeWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit, isM3E: Boolean = true) {
    SwitchWidget(
        icon = AppIcons.InstallAllowDowngrade,
        title = stringResource(id = R.string.config_allow_downgrade),
        description = stringResource(id = R.string.config_allow_downgrade_desc),
        checked = state.data.allowDowngrade,
        isM3E = isM3E,
        onCheckedChange = { dispatch(EditViewAction.ChangeDataAllowDowngrade(it)) }
    )
}

@Composable
fun DataBypassLowTargetSdkWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit, isM3E: Boolean = true) {
    SwitchWidget(
        icon = AppIcons.InstallBypassLowTargetSdk,
        title = stringResource(id = R.string.config_bypass_low_target_sdk),
        description = stringResource(id = R.string.config_bypass_low_target_sdk_desc),
        checked = state.data.bypassLowTargetSdk,
        isM3E = isM3E,
        onCheckedChange = { dispatch(EditViewAction.ChangeDataBypassLowTargetSdk(it)) }
    )
}

@Composable
fun DataAllowAllRequestedPermissionsWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit, isM3E: Boolean = true) {
    SwitchWidget(
        icon = AppIcons.InstallAllowAllRequestedPermissions,
        title = stringResource(id = R.string.config_grant_all_permissions),
        description = stringResource(id = R.string.config_grant_all_permissions_desc),
        checked = state.data.allowAllRequestedPermissions,
        isM3E = isM3E,
        onCheckedChange = { dispatch(EditViewAction.ChangeDataAllowAllRequestedPermissions(it)) }
    )
}

@Composable
fun DataRequestUpdateOwnershipWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit, isM3E: Boolean = true) {
    SwitchWidget(
        icon = AppIcons.InstallRequestUpdateOwnership,
        title = stringResource(id = R.string.config_request_update_ownership),
        description = stringResource(id = R.string.config_request_update_ownership_desc),
        checked = state.data.requestUpdateOwnership,
        isM3E = isM3E,
        onCheckedChange = { dispatch(EditViewAction.ChangeDataRequestUpdateOwnership(it)) }
    )
}

@Composable
fun DataSplitChooseAllWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit, isM3E: Boolean = true) {
    SwitchWidget(
        icon = AppIcons.InstallSplitChooseAll,
        title = stringResource(id = R.string.config_split_choose_all),
        description = stringResource(id = R.string.config_split_choose_all_desc),
        checked = state.data.splitChooseAll,
        isM3E = isM3E,
        onCheckedChange = { dispatch(EditViewAction.ChangeSplitChooseAll(it)) }
    )
}

@Composable
fun DataApkChooseAllWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit, isM3E: Boolean = true) {
    SwitchWidget(
        icon = AppIcons.InstallApkChooseAll,
        title = stringResource(id = R.string.config_apk_choose_all),
        description = stringResource(id = R.string.config_apk_choose_all_desc),
        checked = state.data.apkChooseAll,
        isM3E = isM3E,
        onCheckedChange = { dispatch(EditViewAction.ChangeApkChooseAll(it)) }
    )
}
