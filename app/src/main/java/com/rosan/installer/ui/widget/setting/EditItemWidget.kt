package com.rosan.installer.ui.widget.setting

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.More
import androidx.compose.material.icons.twotone.Downloading
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.material.icons.twotone.Memory
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
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.settings.config.edit.EditViewAction
import com.rosan.installer.ui.page.settings.config.edit.EditViewModel

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
    val data = mapOf(
        ConfigEntity.Authorizer.Global to stringResource(
            R.string.config_authorizer_global_desc,
            when (globalAuthorizer) {
                ConfigEntity.Authorizer.None -> stringResource(R.string.config_authorizer_none)
                ConfigEntity.Authorizer.Root -> stringResource(R.string.config_authorizer_root)
                ConfigEntity.Authorizer.Shizuku -> stringResource(R.string.config_authorizer_shizuku)
                ConfigEntity.Authorizer.Dhizuku -> stringResource(R.string.config_authorizer_dhizuku)
                ConfigEntity.Authorizer.Customize -> stringResource(R.string.config_authorizer_customize)
                else -> stringResource(R.string.config_authorizer_global)
            }
        ),
        ConfigEntity.Authorizer.None to stringResource(R.string.config_authorizer_none),
        ConfigEntity.Authorizer.Root to stringResource(R.string.config_authorizer_root),
        ConfigEntity.Authorizer.Shizuku to stringResource(R.string.config_authorizer_shizuku),
        ConfigEntity.Authorizer.Dhizuku to stringResource(R.string.config_authorizer_dhizuku),
        ConfigEntity.Authorizer.Customize to stringResource(R.string.config_authorizer_customize)
    )
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
fun DataDeclareInstallerWidget(viewModel: EditViewModel) {
    val stateAuthorizer = viewModel.state.data.authorizer
    val globalAuthorizer = viewModel.globalAuthorizer

    val isDhizuku = when (stateAuthorizer) {
        // 如果当前授权方式是 Dhizuku，结果直接为 true
        ConfigEntity.Authorizer.Dhizuku -> true
        // 如果当前授权方式是 Global，则取决于全局授权方式是否为 Dhizuku
        ConfigEntity.Authorizer.Global -> globalAuthorizer == ConfigEntity.Authorizer.Dhizuku
        // 其他任何情况，结果都为 false
        else -> false
    }

    val description =
        if (isDhizuku) stringResource(R.string.dhizuku_cannot_set_installer_desc)
        else null // 其他模式下没有特殊描述

    SwitchWidget(
        icon = AppIcons.InstallSource,
        title = stringResource(id = R.string.config_declare_installer),
        checked = viewModel.state.data.declareInstaller,
        onCheckedChange = {
            // 这是该组件唯一允许的 dispatch，即响应用户的直接交互
            viewModel.dispatch(EditViewAction.ChangeDataDeclareInstaller(it))
        },
        // 将从 ViewModel 获取的状态直接传递给下一层
        description = description,
        enabled = !isDhizuku,
        isError = isDhizuku
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
                            // Highlight the selected item
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
fun DataAutoDeleteWidget(viewModel: EditViewModel) {
    SwitchWidget(
        icon = AppIcons.Delete,
        title = stringResource(id = R.string.config_auto_delete),
        description = stringResource(id = R.string.config_auto_delete_dsp),
        checked = viewModel.state.data.autoDelete,
        onCheckedChange = {
            viewModel.dispatch(EditViewAction.ChangeDataAutoDelete(it))
        }
    )
}

@Composable
fun DisplaySdkWidget(viewModel: EditViewModel) {
    SwitchWidget(
        icon = AppIcons.Info,
        title = stringResource(id = R.string.config_display_sdk_version),
        description = stringResource(id = R.string.config_display_sdk_version_sdp),
        checked = viewModel.state.data.displaySdk,
        onCheckedChange = {
            viewModel.dispatch(EditViewAction.ChangeDisplaySdk(it))
        }
    )
}

@Composable
fun DataForAllUserWidget(viewModel: EditViewModel) {
    SwitchWidget(
        icon = AppIcons.InstallForAllUsers,
        title = stringResource(id = R.string.config_all_users),
        description = stringResource(id = R.string.config_all_users_desc),
        checked = viewModel.state.data.forAllUser,
        onCheckedChange = { viewModel.dispatch(EditViewAction.ChangeDataForAllUser(it)) }
    )
}

@Composable
fun DataAllowTestOnlyWidget(viewModel: EditViewModel) {
    SwitchWidget(
        icon = AppIcons.BugReport,
        title = stringResource(id = R.string.config_allow_test),
        description = stringResource(id = R.string.config_allow_test_desc),
        checked = viewModel.state.data.allowTestOnly,
        onCheckedChange = {
            viewModel.dispatch(EditViewAction.ChangeDataAllowTestOnly(it))
        }
    )
}

@Composable
fun DataAllowDowngradeWidget(viewModel: EditViewModel) {
    SwitchWidget(
        icon = AppIcons.InstallAllowDowngrade,
        title = stringResource(id = R.string.config_allow_downgrade),
        description = stringResource(id = R.string.config_allow_downgrade_desc),
        checked = viewModel.state.data.allowDowngrade,
        onCheckedChange = {
            viewModel.dispatch(EditViewAction.ChangeDataAllowDowngrade(it))
        }
    )
}

@Composable
fun DataBypassLowTargetSdkWidget(viewModel: EditViewModel) {
    SwitchWidget(
        icon = AppIcons.InstallBypassLowTargetSdk,
        title = stringResource(id = R.string.config_bypass_low_target_sdk),
        description = stringResource(id = R.string.config_bypass_low_target_sdk_desc),
        checked = viewModel.state.data.bypassLowTargetSdk,
        onCheckedChange = { viewModel.dispatch(EditViewAction.ChangeDataBypassLowTargetSdk(it)) }
    )
}

@Composable
fun DataAllowRestrictedPermissionsWidget(viewModel: EditViewModel) {
    SwitchWidget(
        icon = AppIcons.InstallAllowRestrictedPermissions,
        title = stringResource(id = R.string.config_all_whitelist_restricted_permissions),
        description = stringResource(id = R.string.config_all_whitelist_restricted_permissions_desc),
        checked = viewModel.state.data.allowRestrictedPermissions,
        onCheckedChange = { viewModel.dispatch(EditViewAction.ChangeDataAllowRestrictedPermissions(it)) }
    )
}

@Composable
fun DataAllowAllRequestedPermissionsWidget(viewModel: EditViewModel) {
    SwitchWidget(
        icon = AppIcons.InstallAllowAllRequestedPermissions,
        title = stringResource(id = R.string.config_grant_all_permissions),
        description = stringResource(id = R.string.config_grant_all_permissions_desc),
        checked = viewModel.state.data.allowAllRequestedPermissions,
        onCheckedChange = { viewModel.dispatch(EditViewAction.ChangeDataAllowAllRequestedPermissions(it)) }
    )
}

