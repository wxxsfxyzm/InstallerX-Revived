// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.settings.config.edit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.layout.Divider
import com.itos.coui.shape.CouiShape
import com.itos.coui.theme.CouiTheme
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
import com.rosan.installer.ui.page.coui.widgets.CouiHintTextField
import com.rosan.installer.ui.page.coui.widgets.CouiSpinnerItem
import com.rosan.installer.ui.page.coui.widgets.CouiSwitchItem
import com.rosan.installer.ui.page.main.settings.config.edit.EditViewAction
import com.rosan.installer.ui.page.main.settings.config.edit.EditViewState
import com.rosan.installer.ui.page.main.settings.config.edit.dhizukuAwareDescription
import com.rosan.installer.ui.util.isSystemPackageInstallerActive
import org.koin.compose.koinInject

@Composable
fun CouiDataNameWidget(
    state: EditViewState,
    dispatch: (EditViewAction) -> Unit,
    trailingContent: @Composable (() -> Unit) = {},
) {
    CouiHintTextField(
        value = state.data.name,
        onValueChange = { dispatch(EditViewAction.ChangeDataName(it)) },
        labelText = stringResource(id = R.string.config_name),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = CouiPageDefaults.horizontalMargin)
            .focusable(),
    )
    trailingContent()
}

@Composable
fun CouiDataDescriptionWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    CouiHintTextField(
        value = state.data.description,
        onValueChange = { dispatch(EditViewAction.ChangeDataDescription(it)) },
        labelText = stringResource(id = R.string.config_description),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = CouiPageDefaults.horizontalMargin)
            .focusable(),
    )
}

@Composable
private fun CouiInlineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isError: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 8,
    modifier: Modifier = Modifier,
) {
    val scheme = CouiTheme.colorScheme
    val dark = CouiTheme.isDark
    val errorColor = if (dark) Color(0xFFFF6C61) else Color(0xFFDB382C)
    val shape = remember { CouiShape(14.dp, 3f) }
    val bgColor = scheme.onSurface.copy(alpha = if (dark) 0.08f else 0.04f)
    val borderColor = if (isError) errorColor else scheme.outlineVariant

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        maxLines = maxLines,
        textStyle = TextStyle(color = scheme.onSurface, fontSize = 15.sp),
        cursorBrush = SolidColor(scheme.accent),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(bgColor, shape)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .focusable(),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = scheme.onSurfaceVariant,
                    fontSize = 15.sp,
                )
            }
            inner()
        },
    )
}

@Composable
fun CouiDataAuthorizerWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    val capabilityProvider = koinInject<DeviceCapabilityProvider>()
    val stateAuthorizer = state.data.authorizer
    val globalAuthorizer = state.globalAuthorizer

    val authorizers = remember(capabilityProvider.isSessionInstallSupported) {
        buildList {
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
                ),
            )
        }
    }

    val authorizerNames = authorizers.map { authorizer ->
        when (authorizer) {
            Authorizer.Global -> {
                val globalName = if (globalAuthorizer == Authorizer.None && capabilityProvider.isSystemApp) {
                    stringResource(R.string.working_status_system_installer)
                } else {
                    stringResource(globalAuthorizer.displayNameRes)
                }
                stringResource(R.string.config_authorizer_global_desc, globalName)
            }

            Authorizer.None -> {
                if (capabilityProvider.isSystemApp) {
                    stringResource(R.string.working_status_system_installer)
                } else {
                    stringResource(authorizer.displayNameRes)
                }
            }

            else -> stringResource(authorizer.displayNameRes)
        }
    }

    val selectedIndex = remember(stateAuthorizer, authorizers) {
        authorizers.indexOf(stateAuthorizer).coerceAtLeast(0)
    }

    CouiSpinnerItem(
        title = stringResource(R.string.config_authorizer),
        summary = stringResource(R.string.config_install_authorizer_desc),
        items = authorizerNames,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { newIndex ->
            authorizers.getOrNull(newIndex)?.let { authorizer ->
                dispatch(EditViewAction.ChangeDataAuthorizer(authorizer))
            }
        },
    )
}

@Composable
fun CouiDataCustomizeAuthorizerWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    if (!state.data.authorizerCustomize) return
    CouiInlineTextField(
        value = state.data.customizeAuthorizer,
        onValueChange = { dispatch(EditViewAction.ChangeDataCustomizeAuthorizer(it)) },
        placeholder = stringResource(id = R.string.config_authorizer_customize),
        singleLine = false,
        maxLines = 8,
    )
}

@Composable
fun CouiDataInstallModeWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    val stateInstallMode = state.data.installMode
    val data = mapOf(
        InstallMode.Dialog to stringResource(R.string.config_install_mode_dialog),
        InstallMode.AutoDialog to stringResource(R.string.config_install_mode_auto_dialog),
        InstallMode.Notification to stringResource(R.string.config_install_mode_notification),
        InstallMode.AutoNotification to stringResource(R.string.config_install_mode_auto_notification),
        InstallMode.Ignore to stringResource(R.string.config_install_mode_ignore),
    )
    val keys = data.keys.toList()
    val selectedIndex = keys.indexOf(stateInstallMode).coerceAtLeast(0)

    CouiSpinnerItem(
        title = stringResource(R.string.config_install_mode),
        items = data.values.toList(),
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { newIndex ->
            keys.getOrNull(newIndex)?.let { mode ->
                dispatch(EditViewAction.ChangeDataInstallMode(mode))
            }
        },
    )
}

@Composable
fun CouiDataAutoApproveSessionWidget(
    state: EditViewState,
    dispatch: (EditViewAction) -> Unit,
    onEnableRequest: () -> Unit,
) {
    val isDhizuku = state.isDhizukuAuthorizerActive
    val enabled = !isDhizuku && !state.labRespectPlatformInstallPolicy
    CouiSwitchItem(
        title = stringResource(id = R.string.config_auto_approve_session),
        description = stringResource(
            id = state.dhizukuAwareDescriptionRes(
                if (state.labRespectPlatformInstallPolicy) {
                    R.string.config_auto_approve_session_disabled_desc
                } else {
                    R.string.config_auto_approve_session_desc
                },
            ),
        ),
        enabled = enabled,
        checked = state.data.autoApproveSession,
        onCheckedChange = { checked ->
            if (checked) {
                onEnableRequest()
            } else {
                dispatch(EditViewAction.ChangeDataAutoApproveSession(false))
            }
        },
    )
}

@Composable
fun CouiToastModeWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    val currentMode = state.data.toastMode
    val data = mapOf(
        ToastMode.Disable to stringResource(R.string.config_toast_mode_disable),
        ToastMode.BackgroundOnly to stringResource(R.string.config_toast_mode_background_only),
        ToastMode.Always to stringResource(R.string.config_toast_mode_always),
    )
    val keys = data.keys.toList()
    val selectedIndex = keys.indexOf(currentMode).coerceAtLeast(0)

    CouiSpinnerItem(
        title = stringResource(R.string.config_install_show_toast),
        summary = stringResource(R.string.config_install_show_toast_desc),
        items = data.values.toList(),
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { newIndex ->
            keys.getOrNull(newIndex)?.let { mode ->
                dispatch(EditViewAction.ChangeDataToastMode(mode))
            }
        },
    )
}

@Composable
fun CouiInstallReasonWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    val enableCustomizeInstallReason = state.data.enableCustomizeInstallReason
    val enabled = !state.labRespectPlatformInstallPolicy
    val expanded = enabled && enableCustomizeInstallReason
    val currentInstallReason = if (state.labRespectPlatformInstallPolicy) {
        InstallReason.USER
    } else {
        state.data.installReason
    }

    val description = stringResource(
        id = if (enabled) {
            R.string.config_customize_install_reason_desc
        } else {
            R.string.config_customize_install_reason_disabled_desc
        },
    )

    Column {
        CouiSwitchItem(
            title = stringResource(id = R.string.config_customize_install_reason),
            description = description,
            enabled = enabled,
            checked = expanded,
            onCheckedChange = { dispatch(EditViewAction.ChangeDataEnableCustomizeInstallReason(it)) },
        )

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            val data = mapOf(
                InstallReason.UNKNOWN to stringResource(R.string.config_install_reason_unknown),
                InstallReason.POLICY to stringResource(R.string.config_install_reason_policy),
                InstallReason.DEVICE_RESTORE to stringResource(R.string.config_install_reason_device_restore),
                InstallReason.DEVICE_SETUP to stringResource(R.string.config_install_reason_device_setup),
                InstallReason.USER to stringResource(R.string.config_install_reason_user),
            )
            val keys = data.keys.toList()
            val selectedIndex = keys.indexOf(currentInstallReason).coerceAtLeast(0)

            Column {
                Divider()
                CouiSpinnerItem(
                    title = stringResource(R.string.config_install_reason),
                    items = data.values.toList(),
                    enabled = enabled,
                    selectedIndex = selectedIndex,
                    onSelectedIndexChange = { newIndex ->
                        keys.getOrNull(newIndex)?.let { reason ->
                            dispatch(EditViewAction.ChangeDataInstallReason(reason))
                        }
                    },
                )
            }
        }
    }
}

@Composable
fun CouiDataPackageSourceWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    val enableCustomizePackageSource = state.data.enableCustomizePackageSource
    val currentSource = state.data.packageSource
    val enabled = !state.labRespectPlatformInstallPolicy

    val description = if (state.labRespectPlatformInstallPolicy) {
        stringResource(R.string.config_customize_package_source_disabled_desc)
    } else {
        stringResource(id = R.string.config_customize_package_source_desc)
    }

    Column {
        CouiSwitchItem(
            title = stringResource(id = R.string.config_customize_package_source),
            description = description,
            checked = enableCustomizePackageSource,
            enabled = enabled,
            onCheckedChange = { dispatch(EditViewAction.ChangeDataEnableCustomizePackageSource(it)) },
        )

        AnimatedVisibility(
            visible = enabled && enableCustomizePackageSource,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            val data = mapOf(
                PackageSource.UNSPECIFIED to stringResource(R.string.config_package_source_unspecified),
                PackageSource.OTHER to stringResource(R.string.config_package_source_other),
                PackageSource.STORE to stringResource(R.string.config_package_source_store),
                PackageSource.LOCAL_FILE to stringResource(R.string.config_package_source_local_file),
                PackageSource.DOWNLOADED_FILE to stringResource(R.string.config_package_source_downloaded_file),
            )
            val keys = data.keys.toList()
            val selectedIndex = keys.indexOf(currentSource).coerceAtLeast(0)

            Column {
                Divider()
                CouiSpinnerItem(
                    title = stringResource(R.string.config_package_source),
                    items = data.values.toList(),
                    enabled = enabled,
                    selectedIndex = selectedIndex,
                    onSelectedIndexChange = { newIndex ->
                        keys.getOrNull(newIndex)?.let { source ->
                            dispatch(EditViewAction.ChangeDataPackageSource(source))
                        }
                    },
                )
            }
        }
    }
}

@Composable
fun CouiDataInstallRequesterWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    val stateData = state.data
    val currentMode = stateData.installRequesterMode
    val packageName = stateData.installRequester
    val uid = stateData.installRequesterUid
    val isError = stateData.errorInstallRequester
    val errorColor = if (CouiTheme.isDark) Color(0xFFFF6C61) else Color(0xFFDB382C)

    val data = mapOf(
        InstallRequesterMode.Disable to stringResource(R.string.config_install_requester_mode_disable),
        InstallRequesterMode.Initiator to stringResource(R.string.config_installer_mode_initiator),
        InstallRequesterMode.Custom to stringResource(R.string.config_installer_mode_custom),
    )
    val keys = data.keys.toList()
    val selectedIndex = keys.indexOf(currentMode).coerceAtLeast(0)

    val description = if (currentMode == InstallRequesterMode.Custom) {
        if (isError) {
            stringResource(R.string.config_declare_install_requester_error_desc)
        } else {
            stringResource(R.string.config_declare_install_requester_desc)
        }
    } else {
        data[currentMode]
    }

    CouiSpinnerItem(
        title = stringResource(id = R.string.config_declare_install_requester),
        summary = description,
        items = data.values.toList(),
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { newIndex ->
            keys.getOrNull(newIndex)?.let { mode ->
                dispatch(EditViewAction.ChangeDataInstallRequesterMode(mode))
            }
        },
    )

    AnimatedVisibility(
        visible = currentMode == InstallRequesterMode.Custom,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        ) {
            CouiInlineTextField(
                value = packageName,
                onValueChange = { dispatch(EditViewAction.ChangeDataInstallRequester(it)) },
                placeholder = stringResource(id = R.string.config_install_requester),
                isError = isError,
            )

            val displayText = if (packageName.isNotEmpty()) {
                if (uid != null) "UID: $uid" else stringResource(R.string.config_error_package_not_found)
            } else {
                stringResource(R.string.config_error_package_name_empty)
            }

            val textColor = if (packageName.isNotEmpty() && uid == null) {
                errorColor
            } else {
                CouiTheme.colorScheme.onSurfaceVariant
            }

            Text(
                text = displayText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
    }
}

@Composable
fun CouiDataDeclareInstallerWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    val stateAuthorizer = state.data.authorizer
    val globalAuthorizer = state.globalAuthorizer
    val currentMode = state.data.installerMode
    val capabilityProvider = koinInject<DeviceCapabilityProvider>()
    val isDhizuku = state.isDhizukuAuthorizerActive

    val description = state.dhizukuAwareDescription(
        when (currentMode) {
            InstallerMode.Self -> {
                val descRes = if (
                    isSystemPackageInstallerActive(
                        stateAuthorizer = stateAuthorizer,
                        globalAuthorizer = globalAuthorizer,
                        isSystemApp = capabilityProvider.isSystemApp,
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
        },
    )

    Column {
        val data = mapOf(
            InstallerMode.Self to stringResource(R.string.config_installer_mode_self),
            InstallerMode.Initiator to stringResource(R.string.config_installer_mode_initiator),
            InstallerMode.Custom to stringResource(R.string.config_installer_mode_custom),
        )
        val keys = data.keys.toList()
        val selectedIndex = keys.indexOf(currentMode).coerceAtLeast(0)

        CouiSpinnerItem(
            title = stringResource(id = R.string.config_declare_installer),
            summary = description,
            items = data.values.toList(),
            enabled = !isDhizuku,
            selectedIndex = selectedIndex,
            onSelectedIndexChange = { newIndex ->
                keys.getOrNull(newIndex)?.let { mode ->
                    dispatch(EditViewAction.ChangeDataInstallerMode(mode))
                }
            },
        )

        AnimatedVisibility(
            visible = currentMode == InstallerMode.Custom && !isDhizuku,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            CouiDataInstallerWidget(state, dispatch)
        }
    }
}

@Composable
fun CouiDataInstallerWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    val stateData = state.data
    val currentInstaller = stateData.installer
    val isError = stateData.errorInstaller
    val errorColor = if (CouiTheme.isDark) Color(0xFFFF6C61) else Color(0xFFDB382C)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
    ) {
        CouiInlineTextField(
            value = currentInstaller,
            onValueChange = { dispatch(EditViewAction.ChangeDataInstaller(it)) },
            placeholder = stringResource(id = R.string.config_installer),
            isError = isError,
        )

        if (isError) {
            Text(
                text = stringResource(R.string.config_error_installer),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = errorColor,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
    }
}

@Composable
fun CouiDataUserWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    val enableCustomizeUser = state.data.enableCustomizeUser
    val targetUserId = state.data.targetUserId
    val availableUsers = state.availableUsers
    val isDhizuku = state.isDhizukuAuthorizerActive

    Column {
        CouiSwitchItem(
            title = stringResource(id = R.string.config_customize_user),
            description = stringResource(
                id = state.dhizukuAwareDescriptionRes(R.string.config_customize_user_desc),
            ),
            checked = enableCustomizeUser,
            enabled = !isDhizuku,
            onCheckedChange = { dispatch(EditViewAction.ChangeDataCustomizeUser(it)) },
        )

        AnimatedVisibility(
            visible = enableCustomizeUser && !isDhizuku,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            val keys = availableUsers.keys.toList()
            val selectedIndex = keys.indexOf(targetUserId).coerceAtLeast(0)

            Column {
                Divider()
                CouiSpinnerItem(
                    title = stringResource(R.string.config_target_user),
                    items = availableUsers.values.toList(),
                    selectedIndex = selectedIndex,
                    onSelectedIndexChange = { newIndex ->
                        keys.getOrNull(newIndex)?.let { userId ->
                            dispatch(EditViewAction.ChangeDataTargetUserId(userId))
                        }
                    },
                )
            }
        }
    }
}

@Composable
fun CouiDataManualDexoptWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    val isDhizuku = state.isDhizukuAuthorizerActive
    val showDexoptOptions = state.data.enableManualDexopt && !isDhizuku

    CouiSwitchItem(
        title = stringResource(id = R.string.config_manual_dexopt),
        description = stringResource(
            id = state.dhizukuAwareDescriptionRes(R.string.config_manual_dexopt_desc),
        ),
        checked = state.data.enableManualDexopt,
        enabled = !isDhizuku,
        onCheckedChange = { dispatch(EditViewAction.ChangeDataEnableManualDexopt(it)) },
    )

    AnimatedVisibility(
        visible = showDexoptOptions,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        val currentMode = state.data.dexoptMode
        val data = mapOf(
            DexoptMode.Verify to stringResource(R.string.config_dexopt_mode_verify),
            DexoptMode.SpeedProfile to stringResource(R.string.config_dexopt_mode_speed_profile),
            DexoptMode.Speed to stringResource(R.string.config_dexopt_mode_speed),
            DexoptMode.Everything to stringResource(R.string.config_dexopt_mode_everything),
        )
        val keys = data.keys.toList()
        val selectedIndex = keys.indexOf(currentMode).coerceAtLeast(0)

        Column {
            Divider()
            CouiSwitchItem(
                title = stringResource(id = R.string.config_force_dexopt),
                description = stringResource(id = R.string.config_force_dexopt_desc),
                checked = state.data.forceDexopt,
                onCheckedChange = { dispatch(EditViewAction.ChangeDataForceDexopt(it)) },
            )
            Divider()
            CouiSpinnerItem(
                title = stringResource(R.string.config_dexopt_mode),
                items = data.values.toList(),
                selectedIndex = selectedIndex,
                onSelectedIndexChange = { newIndex ->
                    keys.getOrNull(newIndex)?.let { mode ->
                        dispatch(EditViewAction.ChangeDataDexoptMode(mode))
                    }
                },
            )
        }
    }
}

@Composable
fun CouiDataAutoDeleteWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    CouiSwitchItem(
        title = stringResource(id = R.string.config_auto_delete),
        description = stringResource(id = R.string.config_auto_delete_desc),
        checked = state.data.autoDelete,
        onCheckedChange = { dispatch(EditViewAction.ChangeDataAutoDelete(it)) },
    )

    AnimatedVisibility(
        visible = state.data.autoDelete,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Column {
            Divider()
            CouiSwitchItem(
                title = stringResource(id = R.string.config_auto_delete_zip),
                description = stringResource(id = R.string.config_auto_delete_zip_desc),
                checked = state.data.autoDeleteZip,
                onCheckedChange = { dispatch(EditViewAction.ChangeDataZipAutoDelete(it)) },
            )
        }
    }
}

@Composable
fun CouiDisplaySdkWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    CouiSwitchItem(
        title = stringResource(id = R.string.config_display_sdk_version),
        description = stringResource(
            id = R.string.combined_description_format,
            stringResource(id = R.string.config_display_sdk_version_desc),
            stringResource(id = R.string.config_display_module_extra_info_desc),
        ),
        checked = state.data.displaySdk,
        onCheckedChange = { dispatch(EditViewAction.ChangeDisplaySdk(it)) },
    )
}

@Composable
fun CouiDisplaySizeWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    CouiSwitchItem(
        title = stringResource(id = R.string.config_display_size),
        description = stringResource(id = R.string.config_display_size_desc),
        checked = state.data.displaySize,
        onCheckedChange = { dispatch(EditViewAction.ChangeDisplaySize(it)) },
    )
}

@Composable
fun CouiDataForAllUserWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    CouiSwitchItem(
        title = stringResource(id = R.string.config_all_users),
        description = stringResource(id = R.string.config_all_users_desc),
        checked = state.data.forAllUser,
        onCheckedChange = { dispatch(EditViewAction.ChangeDataForAllUser(it)) },
    )
}

@Composable
fun CouiDataAllowTestOnlyWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    val isDhizuku = state.isDhizukuAuthorizerActive
    CouiSwitchItem(
        title = stringResource(id = R.string.config_allow_test),
        description = stringResource(id = state.dhizukuAwareDescriptionRes(R.string.config_allow_test_desc)),
        enabled = !isDhizuku,
        checked = state.data.allowTestOnly,
        onCheckedChange = { dispatch(EditViewAction.ChangeDataAllowTestOnly(it)) },
    )
}

@Composable
fun CouiDataAllowDowngradeWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    val isDhizuku = state.isDhizukuAuthorizerActive
    CouiSwitchItem(
        title = stringResource(id = R.string.config_allow_downgrade),
        description = stringResource(id = state.dhizukuAwareDescriptionRes(R.string.config_allow_downgrade_desc)),
        enabled = !isDhizuku,
        checked = state.data.allowDowngrade,
        onCheckedChange = { dispatch(EditViewAction.ChangeDataAllowDowngrade(it)) },
    )
}

@Composable
fun CouiDataBypassLowTargetSdkWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    val isDhizuku = state.isDhizukuAuthorizerActive
    CouiSwitchItem(
        title = stringResource(id = R.string.config_bypass_low_target_sdk),
        description = stringResource(id = state.dhizukuAwareDescriptionRes(R.string.config_bypass_low_target_sdk_desc)),
        enabled = !isDhizuku,
        checked = state.data.bypassLowTargetSdk,
        onCheckedChange = { dispatch(EditViewAction.ChangeDataBypassLowTargetSdk(it)) },
    )
}

@Composable
fun CouiDataAllowSigMismatchWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    CouiSwitchItem(
        title = stringResource(id = R.string.config_allow_sig_mismatch),
        description = stringResource(id = R.string.config_allow_sig_mismatch_desc),
        checked = !state.data.allowSigMismatch,
        onCheckedChange = { dispatch(EditViewAction.ChangeDataAllowSigMismatch(!it)) },
    )
}

@Composable
fun CouiDataAllowSigUnknownWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    val description = buildString {
        append(stringResource(id = R.string.config_allow_sig_unknown_desc))
        if (state.allowInternetAccess) {
            append('\n')
            append(stringResource(id = R.string.config_allow_sig_unknown_http_lightweight_desc))
        }
    }
    CouiSwitchItem(
        title = stringResource(id = R.string.config_allow_sig_unknown),
        description = description,
        checked = !state.data.allowSigUnknown,
        onCheckedChange = { dispatch(EditViewAction.ChangeDataAllowSigUnknown(!it)) },
    )
}

@Composable
fun CouiDataAllowAllRequestedPermissionsWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    val isDhizuku = state.isDhizukuAuthorizerActive
    CouiSwitchItem(
        title = stringResource(id = R.string.config_grant_all_permissions),
        description = stringResource(id = state.dhizukuAwareDescriptionRes(R.string.config_grant_all_permissions_desc)),
        enabled = !isDhizuku,
        checked = state.data.allowAllRequestedPermissions,
        onCheckedChange = { dispatch(EditViewAction.ChangeDataAllowAllRequestedPermissions(it)) },
    )
}

@Composable
fun CouiRequestUpdateOwnershipWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    val isDhizuku = state.isDhizukuAuthorizerActive
    CouiSwitchItem(
        title = stringResource(id = R.string.config_request_update_ownership),
        description = stringResource(id = state.dhizukuAwareDescriptionRes(R.string.config_request_update_ownership_desc)),
        enabled = !isDhizuku,
        checked = state.data.requestUpdateOwnership,
        onCheckedChange = { dispatch(EditViewAction.ChangeDataRequestUpdateOwnership(it)) },
    )
}

@Composable
fun CouiDataSplitChooseAllWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    CouiSwitchItem(
        title = stringResource(id = R.string.config_split_choose_all),
        description = stringResource(id = R.string.config_split_choose_all_desc),
        checked = state.data.splitChooseAll,
        onCheckedChange = { dispatch(EditViewAction.ChangeSplitChooseAll(it)) },
    )
}

@Composable
fun CouiDataApkChooseAllWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    CouiSwitchItem(
        title = stringResource(id = R.string.config_apk_choose_all),
        description = stringResource(id = R.string.config_apk_choose_all_desc),
        checked = state.data.apkChooseAll,
        onCheckedChange = { dispatch(EditViewAction.ChangeApkChooseAll(it)) },
    )
}

@Composable
fun CouiDataRequireBiometricAuthWidget(state: EditViewState, dispatch: (EditViewAction) -> Unit) {
    CouiSwitchItem(
        title = stringResource(id = R.string.installer_settings_require_biometric_auth),
        description = stringResource(id = R.string.installer_settings_require_biometric_auth_desc),
        checked = state.data.requireBiometricAuth,
        onCheckedChange = { dispatch(EditViewAction.ChangeRequireBiometricAuth(it)) },
    )
}
