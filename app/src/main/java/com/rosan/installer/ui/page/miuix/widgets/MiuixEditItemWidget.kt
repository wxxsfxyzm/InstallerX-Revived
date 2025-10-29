package com.rosan.installer.ui.page.miuix.widgets

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
import androidx.compose.material.icons.twotone.Speed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.build.RsConfig
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.config.edit.EditViewAction
import com.rosan.installer.ui.page.main.settings.config.edit.EditViewModel
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.SpinnerEntry
import top.yukonga.miuix.kmp.extra.SpinnerMode
import top.yukonga.miuix.kmp.extra.SuperSpinner

@Composable
fun MiuixDataNameWidget(
    viewModel: EditViewModel,
    trailingContent: @Composable (() -> Unit) = {}
) {
    // Replace the old implementation with a call to the new MiuixHintTextField component.
    MiuixHintTextField(
        value = viewModel.state.data.name,
        onValueChange = {
            viewModel.dispatch(EditViewAction.ChangeDataName(it))
        },
        labelText = stringResource(id = R.string.config_name),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 12.dp)
            .focusable()
    )

    // TODO: Implement custom error handling logic if needed,
    // e.g., display a separate error message Text when viewModel.state.data.errorName is true.

    trailingContent()
}

@Composable
fun MiuixDataDescriptionWidget(viewModel: EditViewModel) {
    MiuixHintTextField(
        value = viewModel.state.data.description,
        onValueChange = { viewModel.dispatch(EditViewAction.ChangeDataDescription(it)) },
        labelText = stringResource(id = R.string.config_description),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 12.dp)
            .focusable()
    )
}

@Composable
fun MiuixDataAuthorizerWidget(viewModel: EditViewModel) {
    val stateAuthorizer = viewModel.state.data.authorizer
    val globalAuthorizer = viewModel.globalAuthorizer
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
        if (!RsConfig.isMiui)
            put(ConfigEntity.Authorizer.None, stringResource(R.string.config_authorizer_none))
        put(ConfigEntity.Authorizer.Root, stringResource(R.string.config_authorizer_root))
        put(ConfigEntity.Authorizer.Shizuku, stringResource(R.string.config_authorizer_shizuku))
        put(ConfigEntity.Authorizer.Dhizuku, stringResource(R.string.config_authorizer_dhizuku))
        put(ConfigEntity.Authorizer.Customize, stringResource(R.string.config_authorizer_customize))
    }
    // Convert data Map to List<SpinnerEntry> required by SuperSpinner.
    val spinnerEntries = remember(data) {
        data.values.map { authorizerName ->
            SpinnerEntry(title = authorizerName)
        }
    }

    // Calculate the currently selected index based on the stateAuthorizer enum.
    val selectedIndex = remember(stateAuthorizer, data) {
        data.keys.toList().indexOf(stateAuthorizer).coerceAtLeast(0)
    }

    // Replace DropDownMenuWidget with SuperSpinner.
    SuperSpinner(
        mode = SpinnerMode.AlwaysOnRight,
        title = stringResource(R.string.config_authorizer),
        // summary = data[stateAuthorizer], // Display current selection text
        items = spinnerEntries,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { newIndex ->
            // Convert index back to enum and dispatch the update action.
            data.keys.elementAtOrNull(newIndex)?.let { authorizer ->
                viewModel.dispatch(EditViewAction.ChangeDataAuthorizer(authorizer))
            }
        }
    )
}

@Composable
fun MiuixDataCustomizeAuthorizerWidget(viewModel: EditViewModel) {
    if (!viewModel.state.data.authorizerCustomize) return
    val customizeAuthorizer = viewModel.state.data.customizeAuthorizer
    TextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .focusable(),
        value = customizeAuthorizer,
        onValueChange = { viewModel.dispatch(EditViewAction.ChangeDataCustomizeAuthorizer(it)) },
        label = stringResource(id = R.string.config_customize_authorizer),
        useLabelAsPlaceholder = true,
        singleLine = false,
        maxLines = 8
    )
}

@Composable
fun MiuixDataInstallModeWidget(viewModel: EditViewModel) {
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

    // Convert data Map to List<SpinnerEntry> required by SuperSpinner.
    // In this case, SpinnerEntry only contains the title, as no individual icons are provided per option.
    val spinnerEntries = remember(data) {
        data.values.map { modeName ->
            SpinnerEntry(title = modeName)
        }
    }

    // Calculate the currently selected index based on the stateInstallMode enum.
    val selectedIndex = remember(stateInstallMode, data) {
        data.keys.toList().indexOf(stateInstallMode).coerceAtLeast(0)
    }

    // Replace DropDownMenuWidget with SuperSpinner.
    SuperSpinner(
        mode = SpinnerMode.AlwaysOnRight,
        title = stringResource(R.string.config_install_mode),
        // summary = data[stateInstallMode], // Display current selection text
        items = spinnerEntries,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { newIndex ->
            // Convert index back to enum and dispatch the update action.
            data.keys.elementAtOrNull(newIndex)?.let { mode ->
                viewModel.dispatch(EditViewAction.ChangeDataInstallMode(mode))
            }
        }
    )
}

@Composable
fun MiuixDataPackageSourceWidget(viewModel: EditViewModel) {
    val stateAuthorizer = viewModel.state.data.authorizer
    val globalAuthorizer = viewModel.globalAuthorizer
    val enableCustomizePackageSource = viewModel.state.data.enableCustomizePackageSource
    val currentSource = viewModel.state.data.packageSource

    // Determine if the Dhizuku authorizer is active, which disables this feature.
    val isDhizuku = when (stateAuthorizer) {
        ConfigEntity.Authorizer.Dhizuku -> true
        ConfigEntity.Authorizer.Global -> globalAuthorizer == ConfigEntity.Authorizer.Dhizuku
        else -> false
    }

    // Display a different description when the feature is disabled by Dhizuku.
    val description =
        if (isDhizuku) stringResource(R.string.dhizuku_cannot_set_package_source_desc)
        else stringResource(id = R.string.config_customize_package_source_desc)

    Column {
        MiuixSwitchWidget(
            icon = AppIcons.InstallPackageSource,
            title = stringResource(id = R.string.config_customize_package_source),
            description = description,
            checked = enableCustomizePackageSource,
            enabled = !isDhizuku,
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

            // Convert the data Map to the List<SpinnerEntry> required by SuperSpinner.
            val spinnerEntries = remember(data) {
                data.values.map { sourceName -> SpinnerEntry(title = sourceName) }
            }

            // Find the index of the currently selected package source.
            val selectedIndex = remember(currentSource, data) {
                data.keys.toList().indexOf(currentSource).coerceAtLeast(0)
            }

            // Get the display name for the currently selected source, with a fallback.
            // val summary = data[currentSource]

            // This spinner allows the user to select the package source.
            SuperSpinner(
                mode = SpinnerMode.AlwaysOnRight,
                title = stringResource(R.string.config_package_source),
                // summary = summary,
                items = spinnerEntries,
                selectedIndex = selectedIndex,
                onSelectedIndexChange = { newIndex ->
                    // When a new source is selected, find the corresponding enum and dispatch an action.
                    data.keys.elementAtOrNull(newIndex)?.let { source ->
                        viewModel.dispatch(EditViewAction.ChangeDataPackageSource(source))
                    }
                }
            )
        }
    }
}

@Composable
fun MiuixDataDeclareInstallerWidget(viewModel: EditViewModel) {
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

    MiuixSwitchWidget(
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
    )

    AnimatedVisibility(
        visible = viewModel.state.data.declareInstaller,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        MiuixDataInstallerWidget(viewModel)
    }
}

@Composable
fun MiuixDataInstallerWidget(viewModel: EditViewModel) {
    val stateData = viewModel.state.data
    /*    viewModel.state.managedInstallerPackages*/
    val currentInstaller = stateData.installer

    /*    // Keep logic for calculating supporting text content.
        val matchingPackage = remember(currentInstaller, managedPackages) {
            managedPackages.find { it.packageName == currentInstaller }
        }*/

    AnimatedVisibility(
        visible = stateData.declareInstaller,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 16.dp)
                    .focusable(),
                value = currentInstaller,
                onValueChange = { viewModel.dispatch(EditViewAction.ChangeDataInstaller(it)) },
                label = stringResource(id = R.string.config_installer),
                useLabelAsPlaceholder = true,
                singleLine = true
            )
        }
    }
}

@Composable
fun MiuixDataUserWidget(viewModel: EditViewModel) {
    // Retrieve all necessary states from the ViewModel.
    val stateAuthorizer = viewModel.state.data.authorizer
    val globalAuthorizer = viewModel.globalAuthorizer
    val enableCustomizeUser = viewModel.state.data.enableCustomizeUser
    val targetUserId = viewModel.state.data.targetUserId
    val availableUsers = viewModel.state.availableUsers

    // Determine if the effective authorizer is Dhizuku to disable the widget.
    val isDhizuku = when (stateAuthorizer) {
        ConfigEntity.Authorizer.Dhizuku -> true
        ConfigEntity.Authorizer.Global -> globalAuthorizer == ConfigEntity.Authorizer.Dhizuku
        else -> false
    }

    // Determine the description text based on whether Dhizuku is active.
    val description =
        if (isDhizuku) stringResource(R.string.dhizuku_cannot_set_user_desc)
        else stringResource(id = R.string.config_customize_user_desc)

    Column {
        // This switch controls the visibility of the user selection spinner.
        MiuixSwitchWidget(
            icon = AppIcons.InstallUser,
            title = stringResource(id = R.string.config_customize_user),
            description = description,
            checked = enableCustomizeUser,
            enabled = !isDhizuku, // The switch is disabled if Dhizuku is the effective authorizer.
            onCheckedChange = {
                viewModel.dispatch(EditViewAction.ChangeDataCustomizeUser(it))
            }
        )

        // The user selection spinner is only visible when the switch is enabled.
        AnimatedVisibility(
            visible = enableCustomizeUser,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            // Convert the Map of available users into a List<SpinnerEntry> for the spinner.
            val spinnerEntries = remember(availableUsers) {
                availableUsers.values.map { userName -> SpinnerEntry(title = userName) }
            }

            // Find the index of the currently selected user ID.
            val selectedIndex = remember(targetUserId, availableUsers) {
                availableUsers.keys.toList().indexOf(targetUserId).coerceAtLeast(0)
            }

            // Get the display name for the currently selected user, with a fallback.
            // val summary = availableUsers[targetUserId] ?: stringResource(R.string.config_user_not_found)

            // This spinner allows the user to select the target user for installation.
            SuperSpinner(
                mode = SpinnerMode.AlwaysOnRight,
                title = stringResource(R.string.config_target_user),
                // summary = summary,
                items = spinnerEntries,
                selectedIndex = selectedIndex,
                onSelectedIndexChange = { newIndex ->
                    // When a new user is selected, find the corresponding user ID and dispatch an action.
                    availableUsers.keys.elementAtOrNull(newIndex)?.let { userId ->
                        viewModel.dispatch(EditViewAction.ChangeDataTargetUserId(userId))
                    }
                }
            )
        }
    }
}

@Composable
fun MiuixDataManualDexoptWidget(viewModel: EditViewModel) {
    val stateAuthorizer = viewModel.state.data.authorizer
    val globalAuthorizer = viewModel.globalAuthorizer

    val isDhizuku = when (stateAuthorizer) {
        ConfigEntity.Authorizer.Dhizuku -> true
        ConfigEntity.Authorizer.Global -> globalAuthorizer == ConfigEntity.Authorizer.Dhizuku
        else -> false
    }

    val description =
        if (isDhizuku) stringResource(R.string.dhizuku_cannot_set_dexopt_desc)
        else stringResource(R.string.config_manual_dexopt_desc)

    MiuixSwitchWidget(
        icon = Icons.TwoTone.Speed,
        title = stringResource(id = R.string.config_manual_dexopt),
        description = description,
        checked = viewModel.state.data.enableManualDexopt,
        enabled = !isDhizuku,
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
            MiuixSwitchWidget(
                //icon = AppIcons.Build,
                title = stringResource(id = R.string.config_force_dexopt),
                description = stringResource(id = R.string.config_force_dexopt_desc),
                checked = viewModel.state.data.forceDexopt,
                onCheckedChange = {
                    viewModel.dispatch(EditViewAction.ChangeDataForceDexopt(it))
                }
            )

            // Convert data Map to List<SpinnerEntry> required by SuperSpinner.
            // Since icons are not provided for dexopt modes in the original code,
            // we create SpinnerEntry with title only.
            val spinnerEntries = remember(data) {
                data.values.map { modeName ->
                    SpinnerEntry(title = modeName)
                }
            }

            // Calculate the currently selected index based on the currentMode enum.
            val selectedIndex = remember(currentMode, data) {
                data.keys.toList().indexOf(currentMode).coerceAtLeast(0)
            }

            // Replace DropDownMenuWidget with SuperSpinner.
            SuperSpinner(
                mode = SpinnerMode.AlwaysOnRight,
                title = stringResource(R.string.config_dexopt_mode),
                // Display the currently selected mode name as summary.
                // summary = data[currentMode] ?: spinnerEntries.firstOrNull()?.title ?: "",
                items = spinnerEntries,
                selectedIndex = selectedIndex,
                onSelectedIndexChange = { newIndex ->
                    // Convert the new index back to the corresponding DexoptMode enum.
                    data.keys.elementAtOrNull(newIndex)?.let { mode ->
                        viewModel.dispatch(EditViewAction.ChangeDataDexoptMode(mode))
                    }
                }
            )
        }
    }
}

@Composable
fun MiuixDataAutoDeleteWidget(viewModel: EditViewModel) {
    MiuixSwitchWidget(
        icon = AppIcons.Delete,
        title = stringResource(id = R.string.config_auto_delete),
        description = stringResource(id = R.string.config_auto_delete_desc),
        checked = viewModel.state.data.autoDelete,
        onCheckedChange = {
            viewModel.dispatch(EditViewAction.ChangeDataAutoDelete(it))
        }
    )
}

@Composable
fun MiuixDisplaySdkWidget(viewModel: EditViewModel) {
    MiuixSwitchWidget(
        icon = AppIcons.Info,
        title = stringResource(id = R.string.config_display_sdk_version),
        description = stringResource(id = R.string.config_display_sdk_version_desc),
        checked = viewModel.state.data.displaySdk,
        onCheckedChange = {
            viewModel.dispatch(EditViewAction.ChangeDisplaySdk(it))
        }
    )
}

@Composable
fun MiuixDataForAllUserWidget(viewModel: EditViewModel) {
    MiuixSwitchWidget(
        icon = AppIcons.InstallForAllUsers,
        title = stringResource(id = R.string.config_all_users),
        description = stringResource(id = R.string.config_all_users_desc),
        checked = viewModel.state.data.forAllUser,
        onCheckedChange = { viewModel.dispatch(EditViewAction.ChangeDataForAllUser(it)) }
    )
}

@Composable
fun MiuixDataAllowTestOnlyWidget(viewModel: EditViewModel) {
    MiuixSwitchWidget(
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
fun MiuixDataAllowDowngradeWidget(viewModel: EditViewModel) {
    MiuixSwitchWidget(
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
fun MiuixDataBypassLowTargetSdkWidget(viewModel: EditViewModel) {
    MiuixSwitchWidget(
        icon = AppIcons.InstallBypassLowTargetSdk,
        title = stringResource(id = R.string.config_bypass_low_target_sdk),
        description = stringResource(id = R.string.config_bypass_low_target_sdk_desc),
        checked = viewModel.state.data.bypassLowTargetSdk,
        onCheckedChange = { viewModel.dispatch(EditViewAction.ChangeDataBypassLowTargetSdk(it)) }
    )
}

@Composable
fun MiuixDataAllowRestrictedPermissionsWidget(viewModel: EditViewModel) {
    MiuixSwitchWidget(
        icon = AppIcons.InstallAllowRestrictedPermissions,
        title = stringResource(id = R.string.config_all_whitelist_restricted_permissions),
        description = stringResource(id = R.string.config_all_whitelist_restricted_permissions_desc),
        checked = viewModel.state.data.allowRestrictedPermissions,
        onCheckedChange = { viewModel.dispatch(EditViewAction.ChangeDataAllowRestrictedPermissions(it)) }
    )
}

@Composable
fun MiuixDataAllowAllRequestedPermissionsWidget(viewModel: EditViewModel) {
    MiuixSwitchWidget(
        icon = AppIcons.InstallAllowAllRequestedPermissions,
        title = stringResource(id = R.string.config_grant_all_permissions),
        description = stringResource(id = R.string.config_grant_all_permissions_desc),
        checked = viewModel.state.data.allowAllRequestedPermissions,
        onCheckedChange = { viewModel.dispatch(EditViewAction.ChangeDataAllowAllRequestedPermissions(it)) }
    )
}

