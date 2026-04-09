// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.settings.config.edit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.DexoptMode
import com.rosan.installer.domain.settings.model.InstallMode
import com.rosan.installer.domain.settings.model.InstallReason
import com.rosan.installer.domain.settings.model.InstallerMode
import com.rosan.installer.domain.settings.model.PackageSource
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.config.edit.EditViewAction
import com.rosan.installer.ui.page.main.settings.config.edit.EditViewState
import com.rosan.installer.ui.page.miuix.widgets.MiuixHintTextField
import com.rosan.installer.ui.page.miuix.widgets.MiuixSwitchWidget
import com.rosan.installer.ui.util.isDhizukuActive
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.SpinnerEntry
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MiuixDataNameWidget(
    state: EditViewState,
    dispatch: (EditViewAction) -> Unit,
    trailingContent: @Composable (() -> Unit) = {}
) {
    MiuixHintTextField(
        value = state.data.name,
        onValueChange = {
            dispatch(EditViewAction.ChangeDataName(it))
        },
        labelText = stringResource(id = R.string.config_name),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 12.dp)
            .focusable()
    )
    trailingContent()
}

@Composable
fun MiuixDataDescriptionWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    MiuixHintTextField(
        value = state.data.description,
        onValueChange = { dispatch(EditViewAction.ChangeDataDescription(it)) },
        labelText = stringResource(id = R.string.config_description),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 12.dp)
            .focusable()
    )
}

@Composable
fun MiuixDataAuthorizerWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
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

    val spinnerEntries = remember(data) {
        data.values.map { authorizerName ->
            SpinnerEntry(title = authorizerName)
        }
    }

    val selectedIndex = remember(stateAuthorizer, data) {
        data.keys.toList().indexOf(stateAuthorizer).coerceAtLeast(0)
    }

    WindowSpinnerPreference(
        title = stringResource(R.string.config_authorizer),
        summary = stringResource(R.string.config_install_authorizer_desc),
        items = spinnerEntries,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { newIndex ->
            data.keys.elementAtOrNull(newIndex)?.let { authorizer ->
                dispatch(EditViewAction.ChangeDataAuthorizer(authorizer))
            }
        }
    )
}

@Composable
fun MiuixDataCustomizeAuthorizerWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    if (!state.data.authorizerCustomize) return
    val customizeAuthorizer = state.data.customizeAuthorizer
    TextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .focusable(),
        value = customizeAuthorizer,
        onValueChange = { dispatch(EditViewAction.ChangeDataCustomizeAuthorizer(it)) },
        label = stringResource(id = R.string.config_customize_authorizer),
        useLabelAsPlaceholder = true,
        singleLine = false,
        maxLines = 8
    )
}

@Composable
fun MiuixDataInstallModeWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
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

    val spinnerEntries = remember(data) {
        data.values.map { modeName ->
            SpinnerEntry(title = modeName)
        }
    }

    val selectedIndex = remember(stateInstallMode, data) {
        data.keys.toList().indexOf(stateInstallMode).coerceAtLeast(0)
    }

    WindowSpinnerPreference(
        title = stringResource(R.string.config_install_mode),
        items = spinnerEntries,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { newIndex ->
            data.keys.elementAtOrNull(newIndex)?.let { mode ->
                dispatch(EditViewAction.ChangeDataInstallMode(mode))
            }
        }
    )
}

@Composable
fun MiuixShowToastWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    MiuixSwitchWidget(
        title = stringResource(id = R.string.config_install_show_toast),
        description = stringResource(R.string.config_install_show_toast_desc),
        checked = state.data.showToast,
        onCheckedChange = {
            dispatch(EditViewAction.ChangeDataShowToast(it))
        }
    )
}

@Composable
fun MiuixInstallReasonWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    val enableCustomizeInstallReason = state.data.enableCustomizeInstallReason
    val currentInstallReason = state.data.installReason

    val description = stringResource(id = R.string.config_customize_install_reason_desc)

    Column {
        MiuixSwitchWidget(
            icon = AppIcons.InstallReason,
            title = stringResource(id = R.string.config_customize_install_reason),
            description = description,
            checked = enableCustomizeInstallReason,
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

            val spinnerEntries = remember(data) {
                data.values.map { sourceName -> SpinnerEntry(title = sourceName) }
            }

            val selectedIndex = remember(currentInstallReason, data) {
                data.keys.toList().indexOf(currentInstallReason).coerceAtLeast(0)
            }

            WindowSpinnerPreference(
                title = stringResource(R.string.config_install_reason),
                items = spinnerEntries,
                selectedIndex = selectedIndex,
                onSelectedIndexChange = { newIndex ->
                    data.keys.elementAtOrNull(newIndex)?.let { reason ->
                        dispatch(EditViewAction.ChangeDataInstallReason(reason))
                    }
                }
            )
        }
    }
}

@Composable
fun MiuixDataPackageSourceWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    val stateAuthorizer = state.data.authorizer
    val globalAuthorizer = state.globalAuthorizer
    val enableCustomizePackageSource = state.data.enableCustomizePackageSource
    val currentSource = state.data.packageSource

    val description =
        if (isDhizukuActive(stateAuthorizer, globalAuthorizer)) stringResource(R.string.dhizuku_cannot_set_package_source_desc)
        else stringResource(id = R.string.config_customize_package_source_desc)

    Column {
        MiuixSwitchWidget(
            title = stringResource(id = R.string.config_customize_package_source),
            description = description,
            checked = enableCustomizePackageSource,
            enabled = !isDhizukuActive(stateAuthorizer, globalAuthorizer),
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

            val spinnerEntries = remember(data) {
                data.values.map { sourceName -> SpinnerEntry(title = sourceName) }
            }

            val selectedIndex = remember(currentSource, data) {
                data.keys.toList().indexOf(currentSource).coerceAtLeast(0)
            }

            WindowSpinnerPreference(
                title = stringResource(R.string.config_package_source),
                items = spinnerEntries,
                selectedIndex = selectedIndex,
                onSelectedIndexChange = { newIndex ->
                    data.keys.elementAtOrNull(newIndex)?.let { source ->
                        dispatch(EditViewAction.ChangeDataPackageSource(source))
                    }
                }
            )
        }
    }
}

@Composable
fun MiuixDataInstallRequesterWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    val stateData = state.data
    val enableCustomize = stateData.enableCustomizeInstallRequester
    val packageName = stateData.installRequester
    val uid = stateData.installRequesterUid
    val isError = stateData.errorInstallRequester

    val description =
        if (isError) stringResource(R.string.config_declare_install_requester_error_desc)
        else stringResource(R.string.config_declare_install_requester_desc)

    MiuixSwitchWidget(
        title = stringResource(id = R.string.config_declare_install_requester),
        description = description,
        checked = enableCustomize,
        onCheckedChange = {
            dispatch(EditViewAction.ChangeDataEnableCustomizeInstallRequester(it))
        }
    )

    AnimatedVisibility(
        visible = enableCustomize,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 16.dp)
                    .focusable(),
                value = packageName,
                onValueChange = {
                    dispatch(EditViewAction.ChangeDataInstallRequester(it))
                },
                borderColor = if (isError) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.primary,
                label = stringResource(id = R.string.config_install_requester),
                useLabelAsPlaceholder = true,
                singleLine = true
            )

            val displayText = if (packageName.isNotEmpty()) {
                if (uid != null) "UID: $uid" else stringResource(R.string.config_error_package_not_found)
            } else stringResource(R.string.config_error_package_name_empty)

            val textColor = if (packageName.isNotEmpty() && uid == null) {
                MiuixTheme.colorScheme.error
            } else {
                MiuixTheme.colorScheme.onBackgroundVariant
            }

            Text(
                text = displayText,
                fontSize = MiuixTheme.textStyles.subtitle.fontSize,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@Composable
fun MiuixDataDeclareInstallerWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    val stateAuthorizer = state.data.authorizer
    val globalAuthorizer = state.globalAuthorizer
    val currentMode = state.data.installerMode

    val isDhizuku = isDhizukuActive(stateAuthorizer, globalAuthorizer)

    val description = if (isDhizuku) {
        stringResource(R.string.dhizuku_cannot_set_installer_desc)
    } else {
        stringResource(id = R.string.config_declare_installer_desc)
    }

    Column {
        val data = mapOf(
            InstallerMode.Self to stringResource(R.string.config_installer_mode_self),
            InstallerMode.Initiator to stringResource(R.string.config_installer_mode_initiator),
            InstallerMode.Custom to stringResource(R.string.config_installer_mode_custom)
        )

        val spinnerEntries = remember(data) {
            data.values.map { modeName -> SpinnerEntry(title = modeName) }
        }

        val selectedIndex = remember(currentMode, data) {
            data.keys.toList().indexOf(currentMode).coerceAtLeast(0)
        }

        WindowSpinnerPreference(
            title = stringResource(id = R.string.config_declare_installer),
            summary = description,
            items = spinnerEntries,
            selectedIndex = selectedIndex,
            onSelectedIndexChange = { newIndex ->
                if (!isDhizuku) {
                    data.keys.elementAtOrNull(newIndex)?.let { mode ->
                        dispatch(EditViewAction.ChangeDataInstallerMode(mode))
                    }
                }
            }
        )

        AnimatedVisibility(
            visible = currentMode == InstallerMode.Custom && !isDhizuku,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            MiuixDataInstallerWidget(state, dispatch)
        }
    }
}

@Composable
fun MiuixDataInstallerWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    val stateData = state.data
    val currentInstaller = stateData.installer
    val isError = stateData.errorInstaller

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp)
                .focusable(),
            value = currentInstaller,
            onValueChange = { dispatch(EditViewAction.ChangeDataInstaller(it)) },
            borderColor = if (isError) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.primary,
            label = stringResource(id = R.string.config_installer),
            useLabelAsPlaceholder = true,
            singleLine = true
        )

        if (isError) {
            Text(
                text = stringResource(R.string.config_error_installer),
                fontSize = MiuixTheme.textStyles.subtitle.fontSize,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@Composable
fun MiuixDataUserWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    val stateAuthorizer = state.data.authorizer
    val globalAuthorizer = state.globalAuthorizer
    val enableCustomizeUser = state.data.enableCustomizeUser
    val targetUserId = state.data.targetUserId
    val availableUsers = state.availableUsers

    val description =
        if (isDhizukuActive(stateAuthorizer, globalAuthorizer)) stringResource(R.string.dhizuku_cannot_set_user_desc)
        else stringResource(id = R.string.config_customize_user_desc)

    Column {
        MiuixSwitchWidget(
            title = stringResource(id = R.string.config_customize_user),
            description = description,
            checked = enableCustomizeUser,
            enabled = !isDhizukuActive(stateAuthorizer, globalAuthorizer),
            onCheckedChange = {
                dispatch(EditViewAction.ChangeDataCustomizeUser(it))
            }
        )

        AnimatedVisibility(
            visible = enableCustomizeUser,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            val spinnerEntries = remember(availableUsers) {
                availableUsers.values.map { userName -> SpinnerEntry(title = userName) }
            }

            val selectedIndex = remember(targetUserId, availableUsers) {
                availableUsers.keys.toList().indexOf(targetUserId).coerceAtLeast(0)
            }

            WindowSpinnerPreference(
                title = stringResource(R.string.config_target_user),
                items = spinnerEntries,
                selectedIndex = selectedIndex,
                onSelectedIndexChange = { newIndex ->
                    availableUsers.keys.elementAtOrNull(newIndex)?.let { userId ->
                        dispatch(EditViewAction.ChangeDataTargetUserId(userId))
                    }
                }
            )
        }
    }
}

@Composable
fun MiuixDataManualDexoptWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    val stateAuthorizer = state.data.authorizer
    val globalAuthorizer = state.globalAuthorizer

    val description =
        if (isDhizukuActive(stateAuthorizer, globalAuthorizer)) stringResource(R.string.dhizuku_cannot_set_dexopt_desc)
        else stringResource(R.string.config_manual_dexopt_desc)

    MiuixSwitchWidget(
        title = stringResource(id = R.string.config_manual_dexopt),
        description = description,
        checked = state.data.enableManualDexopt,
        enabled = !isDhizukuActive(stateAuthorizer, globalAuthorizer),
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
            MiuixSwitchWidget(
                title = stringResource(id = R.string.config_force_dexopt),
                description = stringResource(id = R.string.config_force_dexopt_desc),
                checked = state.data.forceDexopt,
                onCheckedChange = {
                    dispatch(EditViewAction.ChangeDataForceDexopt(it))
                }
            )

            val spinnerEntries = remember(data) {
                data.values.map { modeName ->
                    SpinnerEntry(title = modeName)
                }
            }

            val selectedIndex = remember(currentMode, data) {
                data.keys.toList().indexOf(currentMode).coerceAtLeast(0)
            }

            WindowSpinnerPreference(
                title = stringResource(R.string.config_dexopt_mode),
                items = spinnerEntries,
                selectedIndex = selectedIndex,
                onSelectedIndexChange = { newIndex ->
                    data.keys.elementAtOrNull(newIndex)?.let { mode ->
                        dispatch(EditViewAction.ChangeDataDexoptMode(mode))
                    }
                }
            )
        }
    }
}

@Composable
fun MiuixDataAutoDeleteWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    MiuixSwitchWidget(
        title = stringResource(id = R.string.config_auto_delete),
        description = stringResource(id = R.string.config_auto_delete_desc),
        checked = state.data.autoDelete,
        onCheckedChange = {
            dispatch(EditViewAction.ChangeDataAutoDelete(it))
        }
    )

    AnimatedVisibility(
        visible = state.data.autoDelete,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        MiuixSwitchWidget(
            title = stringResource(id = R.string.config_auto_delete_zip),
            description = stringResource(id = R.string.config_auto_delete_zip_desc),
            checked = state.data.autoDeleteZip,
            onCheckedChange = {
                dispatch(EditViewAction.ChangeDataZipAutoDelete(it))
            }
        )
    }
}

@Composable
fun MiuixDisplaySdkWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    MiuixSwitchWidget(
        title = stringResource(id = R.string.config_display_sdk_version),
        description = stringResource(
            id = R.string.combined_description_format,
            stringResource(id = R.string.config_display_sdk_version_desc),
            stringResource(id = R.string.config_display_module_extra_info_desc)
        ),
        checked = state.data.displaySdk,
        onCheckedChange = {
            dispatch(EditViewAction.ChangeDisplaySdk(it))
        }
    )
}

@Composable
fun MiuixDisplaySizeWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    MiuixSwitchWidget(
        title = stringResource(id = R.string.config_display_size),
        description = stringResource(id = R.string.config_display_size_desc),
        checked = state.data.displaySize,
        onCheckedChange = {
            dispatch(EditViewAction.ChangeDisplaySize(it))
        }
    )
}

@Composable
fun MiuixDataForAllUserWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    MiuixSwitchWidget(
        title = stringResource(id = R.string.config_all_users),
        description = stringResource(id = R.string.config_all_users_desc),
        checked = state.data.forAllUser,
        onCheckedChange = { dispatch(EditViewAction.ChangeDataForAllUser(it)) }
    )
}

@Composable
fun MiuixDataAllowTestOnlyWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    MiuixSwitchWidget(
        title = stringResource(id = R.string.config_allow_test),
        description = stringResource(id = R.string.config_allow_test_desc),
        checked = state.data.allowTestOnly,
        onCheckedChange = {
            dispatch(EditViewAction.ChangeDataAllowTestOnly(it))
        }
    )
}

@Composable
fun MiuixDataAllowDowngradeWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    MiuixSwitchWidget(
        title = stringResource(id = R.string.config_allow_downgrade),
        description = stringResource(id = R.string.config_allow_downgrade_desc),
        checked = state.data.allowDowngrade,
        onCheckedChange = {
            dispatch(EditViewAction.ChangeDataAllowDowngrade(it))
        }
    )
}

@Composable
fun MiuixDataBypassLowTargetSdkWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    MiuixSwitchWidget(
        icon = AppIcons.InstallBypassLowTargetSdk,
        title = stringResource(id = R.string.config_bypass_low_target_sdk),
        description = stringResource(id = R.string.config_bypass_low_target_sdk_desc),
        checked = state.data.bypassLowTargetSdk,
        onCheckedChange = { dispatch(EditViewAction.ChangeDataBypassLowTargetSdk(it)) }
    )
}

@Composable
fun MiuixDataAllowAllRequestedPermissionsWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    MiuixSwitchWidget(
        title = stringResource(id = R.string.config_grant_all_permissions),
        description = stringResource(id = R.string.config_grant_all_permissions_desc),
        checked = state.data.allowAllRequestedPermissions,
        onCheckedChange = { dispatch(EditViewAction.ChangeDataAllowAllRequestedPermissions(it)) }
    )
}

@Composable
fun MiuixRequestUpdateOwnershipWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    MiuixSwitchWidget(
        title = stringResource(id = R.string.config_request_update_ownership),
        description = stringResource(id = R.string.config_request_update_ownership_desc),
        checked = state.data.requestUpdateOwnership,
        onCheckedChange = { dispatch(EditViewAction.ChangeDataRequestUpdateOwnership(it)) }
    )
}

@Composable
fun MiuixDataSplitChooseAllWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    MiuixSwitchWidget(
        title = stringResource(id = R.string.config_split_choose_all),
        description = stringResource(id = R.string.config_split_choose_all_desc),
        checked = state.data.splitChooseAll,
        onCheckedChange = { dispatch(EditViewAction.ChangeSplitChooseAll(it)) }
    )
}

@Composable
fun MiuixDataApkChooseAllWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    MiuixSwitchWidget(
        title = stringResource(id = R.string.config_apk_choose_all),
        description = stringResource(id = R.string.config_apk_choose_all_desc),
        checked = state.data.apkChooseAll,
        onCheckedChange = { dispatch(EditViewAction.ChangeApkChooseAll(it)) }
    )
}
