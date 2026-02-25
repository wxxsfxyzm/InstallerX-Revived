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
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.ui.common.LocalSessionInstallSupported
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.config.edit.EditViewAction
import com.rosan.installer.ui.page.main.settings.config.edit.EditViewModel
import com.rosan.installer.ui.util.isDhizukuActive

@Composable
fun DataNameWidget(
    viewModel: EditViewModel,
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
        value = viewModel.state.data.name,
        onValueChange = {
            viewModel.dispatch(EditViewAction.ChangeDataName(it))
        },
        singleLine = true,
        // TODO do not allow create another Default name
        isError = viewModel.state.data.errorName
    )
    trailingContent()
}

@Composable
fun DataDescriptionWidget(viewModel: EditViewModel) {
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
        value = viewModel.state.data.description,
        onValueChange = { viewModel.dispatch(EditViewAction.ChangeDataDescription(it)) },
        maxLines = 8,
    )
}

@Composable
fun DataAuthorizerWidget(viewModel: EditViewModel) {
    val stateAuthorizer = viewModel.state.data.authorizer
    val globalAuthorizer = viewModel.globalAuthorizer
    val isSessionInstallSupported = LocalSessionInstallSupported.current
    val data = buildMap {
        put(
            ConfigEntity.Authorizer.Global, stringResource(
                R.string.config_authorizer_global_desc,
                when (globalAuthorizer) {
                    ConfigEntity.Authorizer.None -> stringResource(R.string.config_authorizer_none)
                    ConfigEntity.Authorizer.Root -> stringResource(R.string.config_authorizer_root)
                    ConfigEntity.Authorizer.Shizuku -> stringResource(R.string.config_authorizer_shizuku)
                    ConfigEntity.Authorizer.Dhizuku -> stringResource(R.string.config_authorizer_dhizuku)
                    ConfigEntity.Authorizer.Customize -> stringResource(R.string.config_authorizer_customize)
                    else -> stringResource(R.string.config_authorizer_global)
                }
            )
        )
        if (isSessionInstallSupported)
            put(ConfigEntity.Authorizer.None, stringResource(R.string.config_authorizer_none))
        put(ConfigEntity.Authorizer.Root, stringResource(R.string.config_authorizer_root))
        put(ConfigEntity.Authorizer.Shizuku, stringResource(R.string.config_authorizer_shizuku))
        put(ConfigEntity.Authorizer.Dhizuku, stringResource(R.string.config_authorizer_dhizuku))
        put(ConfigEntity.Authorizer.Customize, stringResource(R.string.config_authorizer_customize))
    }
    DropDownMenuWidget(
        icon = Icons.TwoTone.Memory,
        title = stringResource(R.string.config_authorizer),
        description = if (data.containsKey(stateAuthorizer)) data[stateAuthorizer] else null,
        choice = data.keys.toList().indexOf(stateAuthorizer),
        data = data.values.toList(),
    ) {
        data.keys.toList().getOrNull(it)?.let {
            viewModel.dispatch(EditViewAction.ChangeDataAuthorizer(it))
        }
    }
}

@Composable
fun DataCustomizeAuthorizerWidget(viewModel: EditViewModel) {
    if (!viewModel.state.data.authorizerCustomize) return
    val customizeAuthorizer = viewModel.state.data.customizeAuthorizer
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
        onValueChange = { viewModel.dispatch(EditViewAction.ChangeDataCustomizeAuthorizer(it)) },
        maxLines = 8,
        isError = viewModel.state.data.errorCustomizeAuthorizer
    )
}

@Composable
fun DataInstallModeWidget(viewModel: EditViewModel) {
    val stateInstallMode = viewModel.state.data.installMode
    val globalInstallMode = viewModel.globalInstallMode
    val data = mapOf(
        ConfigEntity.InstallMode.Global to stringResource(
            R.string.config_install_mode_global_desc,
            when (globalInstallMode) {
                ConfigEntity.InstallMode.Dialog -> stringResource(R.string.config_install_mode_dialog)
                ConfigEntity.InstallMode.AutoDialog -> stringResource(R.string.config_install_mode_auto_dialog)
                ConfigEntity.InstallMode.Notification -> stringResource(R.string.config_install_mode_notification)
                ConfigEntity.InstallMode.AutoNotification -> stringResource(R.string.config_install_mode_auto_notification)
                ConfigEntity.InstallMode.Ignore -> stringResource(R.string.config_install_mode_ignore)
                else -> stringResource(R.string.config_install_mode_global)
            }
        ),
        ConfigEntity.InstallMode.Dialog to stringResource(R.string.config_install_mode_dialog),
        ConfigEntity.InstallMode.AutoDialog to stringResource(R.string.config_install_mode_auto_dialog),
        ConfigEntity.InstallMode.Notification to stringResource(R.string.config_install_mode_notification),
        ConfigEntity.InstallMode.AutoNotification to stringResource(R.string.config_install_mode_auto_notification),
        ConfigEntity.InstallMode.Ignore to stringResource(R.string.config_install_mode_ignore),
    )
    DropDownMenuWidget(
        icon = Icons.TwoTone.Downloading,
        title = stringResource(R.string.config_install_mode),
        description = if (data.containsKey(stateInstallMode)) data[stateInstallMode] else null,
        choice = data.keys.toList().indexOf(stateInstallMode),
        data = data.values.toList(),
    ) {
        data.keys.toList().getOrNull(it)?.let {
            viewModel.dispatch(EditViewAction.ChangeDataInstallMode(it))
        }
    }
}

@Composable
fun DataInstallReasonWidget(viewModel: EditViewModel, isM3E: Boolean = true) {
    val enableCustomizeInstallReason = viewModel.state.data.enableCustomizeInstallReason
    val currentInstallReason = viewModel.state.data.installReason

    val description = stringResource(id = R.string.config_customize_install_reason_desc)

    Column {
        SwitchWidget(
            icon = AppIcons.InstallReason,
            title = stringResource(id = R.string.config_customize_install_reason),
            description = description,
            checked = enableCustomizeInstallReason,
            isM3E = isM3E,
            onCheckedChange = {
                viewModel.dispatch(EditViewAction.ChangeDataEnableCustomizeInstallReason(it))
            }
        )

        AnimatedVisibility(
            visible = enableCustomizeInstallReason,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            // A map to associate the enum values with their human-readable string resources.
            val data = mapOf(
                ConfigEntity.InstallReason.UNKNOWN to stringResource(R.string.config_install_reason_unknown),
                ConfigEntity.InstallReason.POLICY to stringResource(R.string.config_install_reason_policy),
                ConfigEntity.InstallReason.DEVICE_RESTORE to stringResource(R.string.config_install_reason_device_restore),
                ConfigEntity.InstallReason.DEVICE_SETUP to stringResource(R.string.config_install_reason_device_setup),
                ConfigEntity.InstallReason.USER to stringResource(R.string.config_install_reason_user)
            )

            DropDownMenuWidget(
                title = stringResource(R.string.config_install_reason),
                description = data[currentInstallReason],
                choice = data.keys.toList().indexOf(currentInstallReason),
                data = data.values.toList(),
            ) { index ->
                // Dispatch the action to the ViewModel when a new source is selected.
                data.keys.toList().getOrNull(index)?.let { reason ->
                    viewModel.dispatch(EditViewAction.ChangeDataInstallReason(reason))
                }
            }
        }
    }
}

@Composable
fun DataPackageSourceWidget(viewModel: EditViewModel, isM3E: Boolean = true) {
    val stateAuthorizer = viewModel.state.data.authorizer
    val globalAuthorizer = viewModel.globalAuthorizer
    val enableCustomizePackageSource = viewModel.state.data.enableCustomizePackageSource
    val currentSource = viewModel.state.data.packageSource

    // Display a different description when the feature is disabled by Dhizuku.
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
                viewModel.dispatch(EditViewAction.ChangeDataEnableCustomizePackageSource(it))
            }
        )

        AnimatedVisibility(
            visible = enableCustomizePackageSource,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            // A map to associate the enum values with their human-readable string resources.
            val data = mapOf(
                ConfigEntity.PackageSource.UNSPECIFIED to stringResource(R.string.config_package_source_unspecified),
                ConfigEntity.PackageSource.OTHER to stringResource(R.string.config_package_source_other),
                ConfigEntity.PackageSource.STORE to stringResource(R.string.config_package_source_store),
                ConfigEntity.PackageSource.LOCAL_FILE to stringResource(R.string.config_package_source_local_file),
                ConfigEntity.PackageSource.DOWNLOADED_FILE to stringResource(R.string.config_package_source_downloaded_file),

                )
            DropDownMenuWidget(
                title = stringResource(R.string.config_package_source),
                description = data[currentSource],
                choice = data.keys.toList().indexOf(currentSource),
                data = data.values.toList(),
            ) { index ->
                // Dispatch the action to the ViewModel when a new source is selected.
                data.keys.toList().getOrNull(index)?.let { source ->
                    viewModel.dispatch(EditViewAction.ChangeDataPackageSource(source))
                }
            }
        }
    }
}

@Composable
fun DataInstallRequesterWidget(viewModel: EditViewModel, isM3E: Boolean = true) {
    val stateData = viewModel.state.data
    val enableCustomize = stateData.enableCustomizeInstallRequester
    val packageName = stateData.installRequester
    val uid = stateData.installRequesterUid

    // Validation state for UI:
    // It's an error if the field is not empty but no UID was found.
    // (If it's empty, standard required field error logic applies usually, but here we check existence)
    val isPackageNotFound = packageName.isNotEmpty() && uid == null
    val isError = stateData.errorInstallRequester

    val description =
        if (isError) stringResource(R.string.config_declare_install_requester_error_desc)
        else stringResource(R.string.config_declare_install_requester_desc)

    Column {
        SwitchWidget(
            icon = AppIcons.InstallSource, // Or an appropriate icon for Requester
            title = stringResource(id = R.string.config_declare_install_requester),
            description = description,
            checked = enableCustomize,
            onCheckedChange = {
                viewModel.dispatch(EditViewAction.ChangeDataEnableCustomizeInstallRequester(it))
            },
            isM3E = isM3E,
            isError = isError // Highlight the switch if the inner content is invalid when saving
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
                    viewModel.dispatch(EditViewAction.ChangeDataInstallRequester(it))
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
fun DataDeclareInstallerWidget(viewModel: EditViewModel, isM3E: Boolean = true) {
    val stateAuthorizer = viewModel.state.data.authorizer
    val globalAuthorizer = viewModel.globalAuthorizer

    val description =
        if (isDhizukuActive(stateAuthorizer, globalAuthorizer)) stringResource(R.string.dhizuku_cannot_set_installer_desc)
        else stringResource(id = R.string.config_declare_installer_desc)

    SwitchWidget(
        icon = AppIcons.InstallSource,
        title = stringResource(id = R.string.config_declare_installer),
        checked = viewModel.state.data.declareInstaller,
        onCheckedChange = { viewModel.dispatch(EditViewAction.ChangeDataDeclareInstaller(it)) },
        isM3E = isM3E,
        description = description,
        enabled = !isDhizukuActive(stateAuthorizer, globalAuthorizer),
        isError = isDhizukuActive(stateAuthorizer, globalAuthorizer)
    )

    AnimatedVisibility(
        visible = viewModel.state.data.declareInstaller,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        DataInstallerWidget(viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataInstallerWidget(viewModel: EditViewModel) {
    val stateData = viewModel.state.data
    val managedPackages = viewModel.state.managedInstallerPackages
    val currentInstaller = stateData.installer
    var expanded by remember { mutableStateOf(false) }
    // Find a matching package to display its friendly name
    val matchingPackage = remember(currentInstaller, managedPackages) {
        managedPackages.find { it.packageName == currentInstaller }
    }

    // Use ExposedDropdownMenuBox for the text field with a dropdown menu
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
                    viewModel.dispatch(EditViewAction.ChangeDataInstaller(it))
                },
                label = { Text(text = stringResource(id = R.string.config_installer)) },
                leadingIcon = {
                    Icon(imageVector = AppIcons.InstallSourceInput, contentDescription = null)
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                supportingText = {
                    // If a match is found, show the friendly name as supporting text
                    matchingPackage?.let {
                        Text(stringResource(R.string.config_installer_matches, it.name))
                    }
                },
                singleLine = true,
                isError = stateData.errorInstaller
            )

            // Define the content of the dropdown menu
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
                                viewModel.dispatch(EditViewAction.ChangeDataInstaller(item.packageName))
                                expanded = false
                            },
                            // Highlight the selected pkg
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
fun DataUserWidget(viewModel: EditViewModel, isM3E: Boolean = true) {
    val stateAuthorizer = viewModel.state.data.authorizer
    val globalAuthorizer = viewModel.globalAuthorizer
    val enableCustomizeUser = viewModel.state.data.enableCustomizeUser
    val targetUserId = viewModel.state.data.targetUserId
    val availableUsers = viewModel.state.availableUsers

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
                viewModel.dispatch(EditViewAction.ChangeDataCustomizeUser(it))
            }
        )

        AnimatedVisibility(
            visible = enableCustomizeUser,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            val description = availableUsers[targetUserId] ?: stringResource(R.string.config_user_not_found)
            DropDownMenuWidget(
                title = stringResource(R.string.config_target_user),
                description = description,
                choice = availableUsers.keys.toList().indexOf(targetUserId),
                data = availableUsers.values.toList(),
            ) { index ->
                availableUsers.keys.toList().getOrNull(index)?.let {
                    viewModel.dispatch(EditViewAction.ChangeDataTargetUserId(it))
                }
            }
        }
    }
}

@Composable
fun DataManualDexoptWidget(viewModel: EditViewModel, isM3E: Boolean = true) {
    val stateAuthorizer = viewModel.state.data.authorizer
    val globalAuthorizer = viewModel.globalAuthorizer

    val description =
        if (isDhizukuActive(stateAuthorizer, globalAuthorizer)) stringResource(R.string.dhizuku_cannot_set_dexopt_desc)
        else stringResource(R.string.config_manual_dexopt_desc)

    SwitchWidget(
        icon = Icons.TwoTone.Speed,
        title = stringResource(id = R.string.config_manual_dexopt),
        description = description,
        checked = viewModel.state.data.enableManualDexopt,
        enabled = !isDhizukuActive(stateAuthorizer, globalAuthorizer),
        isError = isDhizukuActive(stateAuthorizer, globalAuthorizer),
        isM3E = isM3E,
        onCheckedChange = {
            viewModel.dispatch(EditViewAction.ChangeDataEnableManualDexopt(it))
        }
    )

    AnimatedVisibility(
        visible = viewModel.state.data.enableManualDexopt,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        val currentMode = viewModel.state.data.dexoptMode
        val data = mapOf(
            ConfigEntity.DexoptMode.Verify to stringResource(R.string.config_dexopt_mode_verify),
            ConfigEntity.DexoptMode.SpeedProfile to stringResource(R.string.config_dexopt_mode_speed_profile),
            ConfigEntity.DexoptMode.Speed to stringResource(R.string.config_dexopt_mode_speed),
            ConfigEntity.DexoptMode.Everything to stringResource(R.string.config_dexopt_mode_everything),
        )
        Column {
            SwitchWidget(
                //icon = AppIcons.Build,
                title = stringResource(id = R.string.config_force_dexopt),
                description = stringResource(id = R.string.config_force_dexopt_desc),
                checked = viewModel.state.data.forceDexopt,
                isM3E = isM3E,
                onCheckedChange = {
                    viewModel.dispatch(EditViewAction.ChangeDataForceDexopt(it))
                }
            )
            DropDownMenuWidget(
                title = stringResource(R.string.config_dexopt_mode),
                description = data[currentMode],
                choice = data.keys.toList().indexOf(currentMode),
                data = data.values.toList(),
            ) {
                data.keys.toList().getOrNull(it)?.let { mode ->
                    viewModel.dispatch(EditViewAction.ChangeDataDexoptMode(mode))
                }
            }
        }
    }
}

@Composable
fun DataAutoDeleteWidget(viewModel: EditViewModel, isM3E: Boolean = true) {
    SwitchWidget(
        icon = AppIcons.Delete,
        title = stringResource(id = R.string.config_auto_delete),
        description = stringResource(id = R.string.config_auto_delete_desc),
        checked = viewModel.state.data.autoDelete,
        isM3E = isM3E,
        onCheckedChange = { viewModel.dispatch(EditViewAction.ChangeDataAutoDelete(it)) }
    )

    AnimatedVisibility (
        visible = viewModel.state.data.autoDelete,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        SwitchWidget(
            title = stringResource(id = R.string.config_auto_delete_zip),
            description = stringResource(id = R.string.config_auto_delete_zip_desc),
            checked = viewModel.state.data.autoDeleteZip,
            isM3E = isM3E,
            onCheckedChange = {
                viewModel.dispatch(EditViewAction.ChangeDataZipAutoDelete(it))
            }
        )
    }
}

@Composable
fun DisplaySdkWidget(viewModel: EditViewModel, isM3E: Boolean = true) {
    SwitchWidget(
        icon = AppIcons.Info,
        title = stringResource(id = R.string.config_display_sdk_version),
        description = stringResource(id = R.string.config_display_sdk_version_desc),
        checked = viewModel.state.data.displaySdk,
        isM3E = isM3E,
        onCheckedChange = { viewModel.dispatch(EditViewAction.ChangeDisplaySdk(it)) }
    )
}

@Composable
fun DisplaySizeWidget(viewModel: EditViewModel, isM3E: Boolean = true) {
    SwitchWidget(
        icon = AppIcons.ShowSize,
        title = stringResource(id = R.string.config_display_size),
        description = stringResource(id = R.string.config_display_size_desc),
        checked = viewModel.state.data.displaySize,
        isM3E = isM3E,
        onCheckedChange = { viewModel.dispatch(EditViewAction.ChangeDisplaySize(it)) }
    )
}

@Composable
fun DataForAllUserWidget(viewModel: EditViewModel, isM3E: Boolean = true) {
    SwitchWidget(
        icon = AppIcons.InstallForAllUsers,
        title = stringResource(id = R.string.config_all_users),
        description = stringResource(id = R.string.config_all_users_desc),
        checked = viewModel.state.data.forAllUser,
        isM3E = isM3E,
        onCheckedChange = { viewModel.dispatch(EditViewAction.ChangeDataForAllUser(it)) }
    )
}

@Composable
fun DataAllowTestOnlyWidget(viewModel: EditViewModel, isM3E: Boolean = true) {
    SwitchWidget(
        icon = AppIcons.BugReport,
        title = stringResource(id = R.string.config_allow_test),
        description = stringResource(id = R.string.config_allow_test_desc),
        checked = viewModel.state.data.allowTestOnly,
        isM3E = isM3E,
        onCheckedChange = { viewModel.dispatch(EditViewAction.ChangeDataAllowTestOnly(it)) }
    )
}

@Composable
fun DataAllowDowngradeWidget(viewModel: EditViewModel, isM3E: Boolean = true) {
    SwitchWidget(
        icon = AppIcons.InstallAllowDowngrade,
        title = stringResource(id = R.string.config_allow_downgrade),
        description = stringResource(id = R.string.config_allow_downgrade_desc),
        checked = viewModel.state.data.allowDowngrade,
        isM3E = isM3E,
        onCheckedChange = { viewModel.dispatch(EditViewAction.ChangeDataAllowDowngrade(it)) }
    )
}

@Composable
fun DataBypassLowTargetSdkWidget(viewModel: EditViewModel, isM3E: Boolean = true) {
    SwitchWidget(
        icon = AppIcons.InstallBypassLowTargetSdk,
        title = stringResource(id = R.string.config_bypass_low_target_sdk),
        description = stringResource(id = R.string.config_bypass_low_target_sdk_desc),
        checked = viewModel.state.data.bypassLowTargetSdk,
        isM3E = isM3E,
        onCheckedChange = { viewModel.dispatch(EditViewAction.ChangeDataBypassLowTargetSdk(it)) }
    )
}

@Composable
fun DataAllowAllRequestedPermissionsWidget(viewModel: EditViewModel, isM3E: Boolean = true) {
    SwitchWidget(
        icon = AppIcons.InstallAllowAllRequestedPermissions,
        title = stringResource(id = R.string.config_grant_all_permissions),
        description = stringResource(id = R.string.config_grant_all_permissions_desc),
        checked = viewModel.state.data.allowAllRequestedPermissions,
        isM3E = isM3E,
        onCheckedChange = { viewModel.dispatch(EditViewAction.ChangeDataAllowAllRequestedPermissions(it)) }
    )
}

@Composable
fun DataSplitChooseAllWidget(viewModel: EditViewModel, isM3E: Boolean = true) {
    SwitchWidget(
        icon = AppIcons.InstallSplitChooseAll,
        title = stringResource(id = R.string.config_split_choose_all),
        description = stringResource(id = R.string.config_split_choose_all_desc),
        checked = viewModel.state.data.splitChooseAll,
        isM3E = isM3E,
        onCheckedChange = { viewModel.dispatch(EditViewAction.ChangeSplitChooseAll(it)) }
    )
}

@Composable
fun DataApkChooseAllWidget(viewModel: EditViewModel, isM3E: Boolean = true) {
    SwitchWidget(
        icon = AppIcons.InstallApkChooseAll,
        title = stringResource(id = R.string.config_apk_choose_all),
        description = stringResource(id = R.string.config_apk_choose_all_desc),
        checked = viewModel.state.data.apkChooseAll,
        isM3E = isM3E,
        onCheckedChange = { viewModel.dispatch(EditViewAction.ChangeApkChooseAll(it)) }
    )
}