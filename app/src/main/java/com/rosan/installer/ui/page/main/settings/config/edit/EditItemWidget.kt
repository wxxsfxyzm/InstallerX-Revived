// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.config.edit

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.More
import androidx.compose.material.icons.twotone.Downloading
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.material.icons.twotone.Memory
import androidx.compose.material.icons.twotone.Speed
import androidx.compose.material.icons.twotone.Terminal
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.rosan.installer.R
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.config.DexoptMode
import com.rosan.installer.domain.settings.model.config.InstallMode
import com.rosan.installer.domain.settings.model.config.InstallReason
import com.rosan.installer.domain.settings.model.config.InstallRequesterMode
import com.rosan.installer.domain.settings.model.config.InstallerMode
import com.rosan.installer.domain.settings.model.config.PackageSource
import com.rosan.installer.domain.settings.model.config.ToastMode
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.widget.setting.BaseItemContainer
import com.rosan.installer.ui.page.main.widget.setting.DropDownMenuWidget
import com.rosan.installer.ui.page.main.widget.setting.SegmentedColumnScope
import com.rosan.installer.ui.page.main.widget.setting.SwitchWidget
import com.rosan.installer.ui.util.isDhizukuActive
import com.rosan.installer.ui.util.isSystemPackageInstallerActive
import org.koin.compose.koinInject

@Composable
fun DataNameWidget(
    state: EditViewState,
    dispatch: (EditViewAction) -> Unit,
    trailingContent: @Composable (() -> Unit) = {}
) {
    BaseItemContainer {
        // Since the container no longer dictates the layout,
        // we explicitly define a Column here to stack the input and trailing content.
        Column(modifier = Modifier.fillMaxWidth()) {
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
                onValueChange = { dispatch(EditViewAction.ChangeDataName(it)) },
                singleLine = true,
                isError = state.data.errorName
            )

            trailingContent()
        }
    }
}

@Composable
fun DataDescriptionWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    OutlinedTextField(
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

fun SegmentedColumnScope.dataAuthorizerWidget(
    state: EditViewState,
    dispatch: (EditViewAction) -> Unit,
) {
    // Read states outside the Composable scope is fine
    val stateAuthorizer = state.data.authorizer
    val globalAuthorizer = state.globalAuthorizer
    val isCustomExpanded = state.data.authorizerCustomize

    expandableItem(
        expanded = isCustomExpanded,
        topContent = {
            val capabilityProvider = koinInject<DeviceCapabilityProvider>()

            val authorizers = buildList {
                add(Authorizer.Global)

                if (capabilityProvider.isSessionInstallSupported) {
                    add(Authorizer.None)
                }

                addAll(
                    listOf(
                        Authorizer.Root,
                        Authorizer.Shizuku,
                        Authorizer.Dhizuku,
                        Authorizer.Customize,
                    )
                )
            }

            // Map authorizers to their dynamic display names
            val descriptions = authorizers.map { authorizer ->
                when (authorizer) {
                    Authorizer.Global -> {
                        // Dynamically resolve the global authorizer's name
                        val globalName = if (globalAuthorizer == Authorizer.None && capabilityProvider.isSystemApp) {
                            stringResource(R.string.working_status_system_installer)
                        } else {
                            stringResource(globalAuthorizer.displayNameRes)
                        }
                        stringResource(R.string.config_authorizer_global_desc, globalName)
                    }

                    Authorizer.None -> {
                        // Check if it should be displayed as system installer
                        if (capabilityProvider.isSystemApp) {
                            stringResource(R.string.working_status_system_installer)
                        } else {
                            stringResource(authorizer.displayNameRes)
                        }
                    }

                    else -> stringResource(authorizer.displayNameRes)
                }
            }

            DropDownMenuWidget(
                icon = Icons.TwoTone.Memory,
                title = stringResource(R.string.config_authorizer),
                description = authorizers
                    .indexOf(stateAuthorizer)
                    .takeIf { it >= 0 }
                    ?.let(descriptions::get),
                choice = authorizers.indexOf(stateAuthorizer),
                data = descriptions,
            ) { index ->
                authorizers.getOrNull(index)?.let {
                    dispatch(EditViewAction.ChangeDataAuthorizer(it))
                }
            }
        },
        bottomContent = { bottomShape ->
            val customizeAuthorizer = state.data.customizeAuthorizer
            BaseItemContainer {
                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 16.dp)
                        .focusable(),
                    shape = bottomShape,
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
        }
    )
}

@Composable
fun DataInstallModeWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    val stateInstallMode = state.data.installMode
    val data = mapOf(
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
fun DataAutoApproveSessionWidget(
    state: EditViewState,
    dispatch: (EditViewAction) -> Unit,
    onEnableRequest: () -> Unit
) {
    val enabled = !state.labRespectPlatformInstallPolicy
    SwitchWidget(
        icon = AppIcons.Active,
        title = stringResource(id = R.string.config_auto_approve_session),
        description = stringResource(
            id = if (enabled) {
                R.string.config_auto_approve_session_desc
            } else {
                R.string.config_auto_approve_session_disabled_desc
            }
        ),
        enabled = enabled,
        checked = state.data.autoApproveSession,
        onCheckedChange = { checked ->
            if (checked) {
                onEnableRequest()
            } else {
                dispatch(EditViewAction.ChangeDataAutoApproveSession(false))
            }
        }
    )
}

@Composable
fun DataToastModeWidget(
    state: EditViewState,
    dispatch: (EditViewAction) -> Unit
) {
    val currentMode = state.data.toastMode
    val data = mapOf(
        ToastMode.Disable to stringResource(R.string.config_toast_mode_disable),
        ToastMode.BackgroundOnly to stringResource(R.string.config_toast_mode_background_only),
        ToastMode.Always to stringResource(R.string.config_toast_mode_always)
    )

    DropDownMenuWidget(
        icon = AppIcons.Toast,
        title = stringResource(R.string.config_install_show_toast),
        description = data[currentMode],
        choice = data.keys.toList().indexOf(currentMode),
        data = data.values.toList(),
    ) { index ->
        data.keys.toList().getOrNull(index)?.let { mode ->
            dispatch(EditViewAction.ChangeDataToastMode(mode))
        }
    }
}

// Extension to integrate directly with the physics layout engine
fun SegmentedColumnScope.dataInstallReasonWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    val enableCustomizeInstallReason = state.data.enableCustomizeInstallReason
    val enabled = !state.labRespectPlatformInstallPolicy
    val expanded = enabled && enableCustomizeInstallReason
    val currentInstallReason = if (state.labRespectPlatformInstallPolicy) {
        InstallReason.USER
    } else {
        state.data.installReason
    }

    expandableItem(
        expanded = expanded,
        topContent = {
            SwitchWidget(
                icon = AppIcons.InstallReason,
                title = stringResource(id = R.string.config_customize_install_reason),
                description = stringResource(
                    id = if (enabled) {
                        R.string.config_customize_install_reason_desc
                    } else {
                        R.string.config_customize_install_reason_disabled_desc
                    }
                ),
                enabled = enabled,
                checked = expanded,
                onCheckedChange = {
                    dispatch(EditViewAction.ChangeDataEnableCustomizeInstallReason(it))
                }
            )
        },
        bottomContent = {
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
                enabled = enabled,
                choice = data.keys.toList().indexOf(currentInstallReason),
                data = data.values.toList(),
            ) { index ->
                data.keys.toList().getOrNull(index)?.let { reason ->
                    dispatch(EditViewAction.ChangeDataInstallReason(reason))
                }
            }
        }
    )
}

fun SegmentedColumnScope.dataPackageSourceWidget(
    state: EditViewState,
    dispatch: (EditViewAction) -> Unit,
    visible: Boolean = true
) {
    val stateAuthorizer = state.data.authorizer
    val globalAuthorizer = state.globalAuthorizer
    val enableCustomizePackageSource = state.data.enableCustomizePackageSource
    val currentSource = state.data.packageSource
    val isDhizuku = isDhizukuActive(stateAuthorizer, globalAuthorizer)
    val enabled = !isDhizuku && !state.labRespectPlatformInstallPolicy

    expandableItem(
        animatedVisibility = visible,
        expanded = enabled && enableCustomizePackageSource,
        topContent = {
            val description = when {
                isDhizuku -> stringResource(R.string.dhizuku_cannot_set_package_source_desc)
                state.labRespectPlatformInstallPolicy ->
                    stringResource(R.string.config_customize_package_source_disabled_desc)
                else -> stringResource(id = R.string.config_customize_package_source_desc)
            }
            SwitchWidget(
                icon = AppIcons.InstallPackageSource,
                title = stringResource(id = R.string.config_customize_package_source),
                description = description,
                checked = enableCustomizePackageSource,
                enabled = enabled,
                onCheckedChange = {
                    dispatch(EditViewAction.ChangeDataEnableCustomizePackageSource(it))
                }
            )
        },
        bottomContent = {
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
                enabled = enabled,
                choice = data.keys.toList().indexOf(currentSource),
                data = data.values.toList(),
            ) { index ->
                data.keys.toList().getOrNull(index)?.let { source ->
                    dispatch(EditViewAction.ChangeDataPackageSource(source))
                }
            }
        }
    )
}

fun SegmentedColumnScope.dataInstallRequesterWidget(
    state: EditViewState,
    dispatch: (EditViewAction) -> Unit,
    visible: Boolean = true
) {
    val stateData = state.data
    val currentMode = stateData.installRequesterMode
    val packageName = stateData.installRequester
    val uid = stateData.installRequesterUid

    val isPackageNotFound = packageName.isNotEmpty() && uid == null
    val isError = stateData.errorInstallRequester

    expandableItem(
        animatedVisibility = visible,
        expanded = currentMode == InstallRequesterMode.Custom,
        topContent = {
            val data = mapOf(
                InstallRequesterMode.Disable to stringResource(R.string.config_install_requester_mode_disable),
                InstallRequesterMode.Initiator to stringResource(R.string.config_installer_mode_initiator),
                InstallRequesterMode.Custom to stringResource(R.string.config_installer_mode_custom)
            )
            val description = if (currentMode == InstallRequesterMode.Custom) {
                if (isError) stringResource(R.string.config_declare_install_requester_error_desc)
                else stringResource(R.string.config_declare_install_requester_desc)
            } else {
                data[currentMode]
            }
            DropDownMenuWidget(
                icon = AppIcons.InstallSource,
                title = stringResource(id = R.string.config_declare_install_requester),
                description = description,
                choice = data.keys.toList().indexOf(currentMode),
                data = data.values.toList(),
                isError = isError
            ) { index ->
                data.keys.toList().getOrNull(index)?.let { mode ->
                    dispatch(EditViewAction.ChangeDataInstallRequesterMode(mode))
                }
            }
        },
        bottomContent = {
            BaseItemContainer {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 16.dp)
                        .focusable(),
                    value = packageName,
                    onValueChange = { dispatch(EditViewAction.ChangeDataInstallRequester(it)) },
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
                        } else {
                            Text(text = stringResource(R.string.config_error_package_name_empty))
                        }
                    }
                )
            }
        }
    )
}

fun SegmentedColumnScope.dataDeclareInstallerWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    val stateAuthorizer = state.data.authorizer
    val globalAuthorizer = state.globalAuthorizer
    val currentMode = state.data.installerMode

    val isDhizuku = isDhizukuActive(stateAuthorizer, globalAuthorizer)
    val isExpanded = currentMode == InstallerMode.Custom && !isDhizuku

    expandableItem(
        expanded = isExpanded,
        topContent = {
            val capabilityProvider = koinInject<DeviceCapabilityProvider>()
            val data = mapOf(
                InstallerMode.Self to stringResource(R.string.config_installer_mode_self),
                InstallerMode.Initiator to stringResource(R.string.config_installer_mode_initiator),
                InstallerMode.Custom to stringResource(R.string.config_installer_mode_custom)
            )

            val description = if (isDhizuku) {
                stringResource(R.string.dhizuku_cannot_set_installer_desc)
            } else {
                when (currentMode) {
                    InstallerMode.Self -> {
                        val descRes = if (
                            isSystemPackageInstallerActive(
                                stateAuthorizer = stateAuthorizer,
                                globalAuthorizer = globalAuthorizer,
                                isSystemApp = capabilityProvider.isSystemApp
                            )
                        ) {
                            R.string.config_declare_installer_system_default_desc
                        } else {
                            R.string.config_declare_installer_desc
                        }
                        stringResource(descRes)
                    }
                    InstallerMode.Initiator -> stringResource(R.string.config_installer_mode_initiator)
                    InstallerMode.Custom -> stringResource(R.string.config_installer_mode_custom)
                }
            }

            DropDownMenuWidget(
                icon = AppIcons.InstallSource,
                title = stringResource(id = R.string.config_declare_installer),
                description = description,
                choice = data.keys.toList().indexOf(currentMode),
                data = data.values.toList(),
                enabled = !isDhizuku,
            ) { index ->
                if (!isDhizuku) {
                    data.keys.toList().getOrNull(index)?.let { mode ->
                        dispatch(EditViewAction.ChangeDataInstallerMode(mode))
                    }
                }
            }
        },
        bottomContent = {
            DataInstallerWidget(state, dispatch)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DataInstallerWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    val stateData = state.data
    val managedPackages = state.managedInstallerPackages
    val currentInstaller = stateData.installer
    var expanded by remember { mutableStateOf(false) }

    val matchingPackage = remember(currentInstaller, managedPackages) {
        managedPackages.find { it.packageName == currentInstaller }
    }

    val layoutDirection = LocalLayoutDirection.current
    val leftCutoutOffset = WindowInsets.displayCutout
        .asPaddingValues()
        .calculateLeftPadding(layoutDirection)

    BaseItemContainer {
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

            DropdownMenuPopup(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.exposedDropdownSize(),
                offset = DpOffset(x = leftCutoutOffset, y = 0.dp),
                properties = PopupProperties(
                    focusable = true,
                    clippingEnabled = false
                )
            ) {
                DropdownMenuGroup(
                    shapes = MenuDefaults.groupShapes()
                ) {
                    if (managedPackages.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.config_no_managed_packages_to_suggest)) },
                            onClick = { expanded = false },
                            enabled = false,
                            shape = MenuDefaults.standaloneItemShape
                        )
                    } else {
                        val count = managedPackages.size
                        managedPackages.forEachIndexed { index, item ->
                            val isSelected = currentInstaller == item.packageName
                            DropdownMenuItem(
                                selected = isSelected,
                                onClick = {
                                    dispatch(EditViewAction.ChangeDataInstaller(item.packageName))
                                    expanded = false
                                },
                                text = { Text("${item.name} (${item.packageName})") },
                                shapes = MenuDefaults.itemShape(index = index, count = count)
                            )
                        }
                    }
                }
            }
        }
    }
}

fun SegmentedColumnScope.dataUserWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    val stateAuthorizer = state.data.authorizer
    val globalAuthorizer = state.globalAuthorizer
    val enableCustomizeUser = state.data.enableCustomizeUser
    val targetUserId = state.data.targetUserId
    val availableUsers = state.availableUsers
    val isDhizuku = isDhizukuActive(stateAuthorizer, globalAuthorizer)

    expandableItem(
        expanded = enableCustomizeUser && !isDhizuku,
        topContent = {
            val description =
                if (isDhizuku) stringResource(R.string.dhizuku_cannot_set_user_desc)
                else stringResource(id = R.string.config_customize_user_desc)
            SwitchWidget(
                icon = AppIcons.InstallUser,
                title = stringResource(id = R.string.config_customize_user),
                description = description,
                checked = enableCustomizeUser,
                enabled = !isDhizuku,
                onCheckedChange = {
                    dispatch(EditViewAction.ChangeDataCustomizeUser(it))
                }
            )
        },
        bottomContent = {
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
    )
}

fun SegmentedColumnScope.dataManualDexoptWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    val stateAuthorizer = state.data.authorizer
    val globalAuthorizer = state.globalAuthorizer
    val expanded = state.data.enableManualDexopt
    val isDhizuku = isDhizukuActive(stateAuthorizer, globalAuthorizer)
    val showDexoptOptions = expanded && !isDhizuku

    // Multi-item integration completely eliminates the need for AnimatedVisibility and manual shape adjustments
    item(forceFlatBottom = showDexoptOptions) {
        val description =
            if (isDhizuku) stringResource(R.string.dhizuku_cannot_set_dexopt_desc)
            else stringResource(R.string.config_manual_dexopt_desc)
        SwitchWidget(
            icon = Icons.TwoTone.Speed,
            title = stringResource(id = R.string.config_manual_dexopt),
            description = description,
            checked = expanded,
            enabled = !isDhizuku,
            onCheckedChange = {
                dispatch(EditViewAction.ChangeDataEnableManualDexopt(it))
            }
        )
    }

    item(
        animatedVisibility = showDexoptOptions,
        topPadding = 1.dp,
        forceFlatTop = true,
        forceFlatBottom = true
    ) {
        SwitchWidget(
            title = stringResource(id = R.string.config_force_dexopt),
            description = stringResource(id = R.string.config_force_dexopt_desc),
            checked = state.data.forceDexopt,
            onCheckedChange = {
                dispatch(EditViewAction.ChangeDataForceDexopt(it))
            }
        )
    }

    val currentMode = state.data.dexoptMode

    item(
        animatedVisibility = showDexoptOptions,
        topPadding = 1.dp,
        forceFlatTop = true
    ) {
        val data = mapOf(
            DexoptMode.Verify to stringResource(R.string.config_dexopt_mode_verify),
            DexoptMode.SpeedProfile to stringResource(R.string.config_dexopt_mode_speed_profile),
            DexoptMode.Speed to stringResource(R.string.config_dexopt_mode_speed),
            DexoptMode.Everything to stringResource(R.string.config_dexopt_mode_everything),
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

fun SegmentedColumnScope.dataAutoDeleteWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    val expanded = state.data.autoDelete

    expandableItem(
        expanded = expanded,
        topContent = {
            SwitchWidget(
                icon = AppIcons.Delete,
                title = stringResource(id = R.string.config_auto_delete),
                description = stringResource(id = R.string.config_auto_delete_desc),
                checked = expanded,
                onCheckedChange = { dispatch(EditViewAction.ChangeDataAutoDelete(it)) }
            )
        },
        bottomContent = {
            SwitchWidget(
                title = stringResource(id = R.string.config_auto_delete_zip),
                description = stringResource(id = R.string.config_auto_delete_zip_desc),
                checked = state.data.autoDeleteZip,
                onCheckedChange = {
                    dispatch(EditViewAction.ChangeDataZipAutoDelete(it))
                }
            )
        }
    )
}

@Composable
fun DisplaySdkWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    SwitchWidget(
        icon = AppIcons.Info,
        title = stringResource(id = R.string.config_display_sdk_version),
        description = stringResource(id = R.string.config_display_sdk_version_desc),
        checked = state.data.displaySdk,
        onCheckedChange = { dispatch(EditViewAction.ChangeDisplaySdk(it)) }
    )
}

@Composable
fun DisplaySizeWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    SwitchWidget(
        icon = AppIcons.ShowSize,
        title = stringResource(id = R.string.config_display_size),
        description = stringResource(id = R.string.config_display_size_desc),
        checked = state.data.displaySize,
        onCheckedChange = { dispatch(EditViewAction.ChangeDisplaySize(it)) }
    )
}

@Composable
fun DataForAllUserWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    SwitchWidget(
        icon = AppIcons.InstallForAllUsers,
        title = stringResource(id = R.string.config_all_users),
        description = stringResource(id = R.string.config_all_users_desc),
        checked = state.data.forAllUser,
        onCheckedChange = { dispatch(EditViewAction.ChangeDataForAllUser(it)) }
    )
}

@Composable
fun DataAllowTestOnlyWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    SwitchWidget(
        icon = AppIcons.BugReport,
        title = stringResource(id = R.string.config_allow_test),
        description = stringResource(id = R.string.config_allow_test_desc),
        checked = state.data.allowTestOnly,
        onCheckedChange = { dispatch(EditViewAction.ChangeDataAllowTestOnly(it)) }
    )
}

@Composable
fun DataAllowDowngradeWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    SwitchWidget(
        icon = AppIcons.InstallAllowDowngrade,
        title = stringResource(id = R.string.config_allow_downgrade),
        description = stringResource(id = R.string.config_allow_downgrade_desc),
        checked = state.data.allowDowngrade,
        onCheckedChange = { dispatch(EditViewAction.ChangeDataAllowDowngrade(it)) }
    )
}

@Composable
fun DataBypassLowTargetSdkWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    SwitchWidget(
        icon = AppIcons.InstallBypassLowTargetSdk,
        title = stringResource(id = R.string.config_bypass_low_target_sdk),
        description = stringResource(id = R.string.config_bypass_low_target_sdk_desc),
        checked = state.data.bypassLowTargetSdk,
        onCheckedChange = { dispatch(EditViewAction.ChangeDataBypassLowTargetSdk(it)) }
    )
}

@Composable
fun DataAllowSigMismatchWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    SwitchWidget(
        icon = AppIcons.InstallAllowSigMismatch,
        title = stringResource(id = R.string.config_allow_sig_mismatch),
        description = stringResource(id = R.string.config_allow_sig_mismatch_desc),
        checked = !state.data.allowSigMismatch,
        onCheckedChange = { dispatch(EditViewAction.ChangeDataAllowSigMismatch(!it)) }
    )
}

@Composable
fun DataAllowSigUnknownWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    SwitchWidget(
        icon = AppIcons.InstallAllowSigUnknown,
        title = stringResource(id = R.string.config_allow_sig_unknown),
        description = stringResource(id = R.string.config_allow_sig_unknown_desc),
        checked = !state.data.allowSigUnknown,
        onCheckedChange = { dispatch(EditViewAction.ChangeDataAllowSigUnknown(!it)) }
    )
}

@Composable
fun DataAllowAllRequestedPermissionsWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    SwitchWidget(
        icon = AppIcons.InstallAllowAllRequestedPermissions,
        title = stringResource(id = R.string.config_grant_all_permissions),
        description = stringResource(id = R.string.config_grant_all_permissions_desc),
        checked = state.data.allowAllRequestedPermissions,
        onCheckedChange = { dispatch(EditViewAction.ChangeDataAllowAllRequestedPermissions(it)) }
    )
}

@Composable
fun DataRequestUpdateOwnershipWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    SwitchWidget(
        icon = AppIcons.InstallRequestUpdateOwnership,
        title = stringResource(id = R.string.config_request_update_ownership),
        description = stringResource(id = R.string.config_request_update_ownership_desc),
        checked = state.data.requestUpdateOwnership,
        onCheckedChange = { dispatch(EditViewAction.ChangeDataRequestUpdateOwnership(it)) }
    )
}

@Composable
fun DataSplitChooseAllWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    SwitchWidget(
        icon = AppIcons.InstallSplitChooseAll,
        title = stringResource(id = R.string.config_split_choose_all),
        description = stringResource(id = R.string.config_split_choose_all_desc),
        checked = state.data.splitChooseAll,
        onCheckedChange = { dispatch(EditViewAction.ChangeSplitChooseAll(it)) }
    )
}

@Composable
fun DataApkChooseAllWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    SwitchWidget(
        icon = AppIcons.InstallApkChooseAll,
        title = stringResource(id = R.string.config_apk_choose_all),
        description = stringResource(id = R.string.config_apk_choose_all_desc),
        checked = state.data.apkChooseAll,
        onCheckedChange = { dispatch(EditViewAction.ChangeApkChooseAll(it)) }
    )
}

@Composable
fun DataRequireBiometricAuthWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    SwitchWidget(
        icon = AppIcons.BiometricAuth,
        title = stringResource(id = R.string.installer_settings_require_biometric_auth),
        description = stringResource(id = R.string.installer_settings_require_biometric_auth_desc),
        checked = state.data.requireBiometricAuth,
        onCheckedChange = { dispatch(EditViewAction.ChangeRequireBiometricAuth(it)) }
    )
}
