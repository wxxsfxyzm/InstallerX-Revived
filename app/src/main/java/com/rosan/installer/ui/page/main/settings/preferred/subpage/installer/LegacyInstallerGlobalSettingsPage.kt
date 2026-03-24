// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.subpage.installer

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rosan.installer.R
import com.rosan.installer.core.env.DeviceConfig
import com.rosan.installer.domain.device.model.Manufacturer
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.InstallMode
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.widget.setting.AppBackButton
import com.rosan.installer.ui.page.main.widget.setting.AutoClearNotificationTimeWidget
import com.rosan.installer.ui.page.main.widget.setting.DataAuthorizerWidget
import com.rosan.installer.ui.page.main.widget.setting.DataInstallModeWidget
import com.rosan.installer.ui.page.main.widget.setting.IntNumberPickerWidget
import com.rosan.installer.ui.page.main.widget.setting.LabelWidget
import com.rosan.installer.ui.page.main.widget.setting.ManagedPackagesWidget
import com.rosan.installer.ui.page.main.widget.setting.ManagedUidsWidget
import com.rosan.installer.ui.page.main.widget.setting.SwitchWidget
import com.rosan.installer.ui.theme.none
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegacyInstallerGlobalSettingsPage(
    navController: NavController,
    viewModel: InstallerSettingsViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.none,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.installer_settings)) },
                navigationIcon = {
                    AppBackButton(onClick = { navController.navigateUp() })
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item { LabelWidget(stringResource(R.string.installer_settings_global_installer)) }
            item {
                DataAuthorizerWidget(
                    currentAuthorizer = uiState.authorizer,
                    changeAuthorizer = { newAuthorizer ->
                        viewModel.dispatch(InstallerSettingsAction.ChangeGlobalAuthorizer(newAuthorizer))
                    },
                    trailingContent = {
                        AnimatedVisibility(
                            visible = uiState.authorizer == Authorizer.Dhizuku,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            IntNumberPickerWidget(
                                icon = AppIcons.Working,
                                title = stringResource(R.string.set_countdown),
                                description = stringResource(R.string.dhizuku_auto_close_countdown_desc),
                                value = uiState.dhizukuAutoCloseCountDown,
                                startInt = 1,
                                endInt = 10
                            ) {
                                viewModel.dispatch(
                                    InstallerSettingsAction.ChangeDhizukuAutoCloseCountDown(it)
                                )
                            }
                        }
                    }
                )
            }
            item {
                DataInstallModeWidget(
                    currentInstallMode = uiState.installMode,
                    changeInstallMode = {
                        viewModel.dispatch(InstallerSettingsAction.ChangeGlobalInstallMode(it))
                    }
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA)
                item {
                    SwitchWidget(
                        icon = AppIcons.LiveActivity,
                        title = stringResource(R.string.theme_settings_use_live_activity),
                        description = stringResource(R.string.theme_settings_use_live_activity_desc),
                        checked = uiState.showLiveActivity,
                        isM3E = false,
                        onCheckedChange = {
                            viewModel.dispatch(InstallerSettingsAction.ChangeShowLiveActivity(it))
                        }
                    )
                }
            item {
                AutoClearNotificationTimeWidget(
                    currentValue = uiState.notificationSuccessAutoClearSeconds,
                    onValueChange = { seconds ->
                        viewModel.dispatch(
                            InstallerSettingsAction.ChangeNotificationSuccessAutoClearSeconds(
                                seconds
                            )
                        )
                    }
                )
            }
            if (BiometricManager
                    .from(context)
                    .canAuthenticate(BIOMETRIC_WEAK or BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
            ) {
                item {
                    SwitchWidget(
                        icon = AppIcons.BiometricAuth,
                        title = stringResource(R.string.installer_settings_require_biometric_auth),
                        description = stringResource(R.string.installer_settings_require_biometric_auth_desc),
                        checked = uiState.installerRequireBiometricAuth,
                        isM3E = false,
                        onCheckedChange = {
                            viewModel.dispatch(InstallerSettingsAction.ChangeBiometricAuth(it))
                        }
                    )
                }
            }
            item {
                val isDialogMode =
                    uiState.installMode == InstallMode.Dialog || uiState.installMode == InstallMode.AutoDialog
                val isNotificationMode =
                    uiState.installMode == InstallMode.Notification || uiState.installMode == InstallMode.AutoNotification

                AnimatedVisibility(
                    visible = isDialogMode || isNotificationMode,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(modifier = Modifier.animateContentSize()) {
                        AnimatedContent(
                            targetState = if (isDialogMode) {
                                R.string.installer_settings_dialog_mode_options
                            } else {
                                R.string.installer_settings_notification_mode_options
                            },
                            label = "OptionsTitleAnimation"
                        ) { targetTitleRes ->
                            LabelWidget(label = stringResource(id = targetTitleRes))
                        }
                        AnimatedVisibility(visible = isDialogMode) {
                            SwitchWidget(
                                icon = AppIcons.MultiLineSettingIcon,
                                title = stringResource(id = R.string.version_compare_in_single_line),
                                description = stringResource(id = R.string.version_compare_in_single_line_desc),
                                checked = uiState.versionCompareInSingleLine,
                                isM3E = false,
                                onCheckedChange = {
                                    viewModel.dispatch(
                                        InstallerSettingsAction.ChangeVersionCompareInSingleLine(
                                            it
                                        )
                                    )
                                }
                            )
                        }
                        AnimatedVisibility(visible = isDialogMode) {
                            SwitchWidget(
                                icon = AppIcons.SingleLineSettingIcon,
                                title = stringResource(id = R.string.sdk_compare_in_multi_line),
                                description = stringResource(id = R.string.sdk_compare_in_multi_line_desc),
                                checked = uiState.sdkCompareInMultiLine,
                                isM3E = false,
                                onCheckedChange = {
                                    viewModel.dispatch(
                                        InstallerSettingsAction.ChangeSdkCompareInMultiLine(
                                            it
                                        )
                                    )
                                }
                            )
                        }
                        AnimatedVisibility(
                            visible = uiState.installMode == InstallMode.Dialog,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            SwitchWidget(
                                icon = AppIcons.MenuOpen,
                                title = stringResource(id = R.string.show_dialog_install_extended_menu),
                                description = stringResource(id = R.string.show_dialog_install_extended_menu_desc),
                                checked = uiState.showDialogInstallExtendedMenu,
                                isM3E = false,
                                onCheckedChange = {
                                    viewModel.dispatch(
                                        InstallerSettingsAction.ChangeShowDialogInstallExtendedMenu(it)
                                    )
                                }
                            )
                        }
                        AnimatedVisibility(visible = isDialogMode) {
                            SwitchWidget(
                                icon = AppIcons.Suggestion,
                                title = stringResource(id = R.string.show_intelligent_suggestion),
                                description = stringResource(id = R.string.show_intelligent_suggestion_desc),
                                checked = uiState.showSmartSuggestion,
                                isM3E = false,
                                onCheckedChange = {
                                    viewModel.dispatch(
                                        InstallerSettingsAction.ChangeShowSuggestion(it)
                                    )
                                }
                            )
                        }
                        AnimatedVisibility(visible = isNotificationMode) {
                            SwitchWidget(
                                icon = AppIcons.Dialog,
                                title = stringResource(id = R.string.show_dialog_when_pressing_notification),
                                description = stringResource(id = R.string.change_notification_touch_behavior),
                                checked = uiState.showDialogWhenPressingNotification,
                                isM3E = false,
                                onCheckedChange = {
                                    viewModel.dispatch(
                                        InstallerSettingsAction.ChangeShowDialogWhenPressingNotification(it)
                                    )
                                }
                            )
                        }
                        AnimatedVisibility(isDialogMode) {
                            SwitchWidget(
                                icon = AppIcons.Silent,
                                title = stringResource(id = R.string.auto_silent_install),
                                description = stringResource(id = R.string.auto_silent_install_desc),
                                checked = uiState.autoSilentInstall,
                                onCheckedChange = {
                                    viewModel.dispatch(InstallerSettingsAction.ChangeAutoSilentInstall(it))
                                }
                            )
                        }
                        AnimatedVisibility(
                            visible = isDialogMode || uiState.showDialogWhenPressingNotification,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            SwitchWidget(
                                icon = AppIcons.NotificationDisabled,
                                title = stringResource(id = R.string.disable_notification_on_dismiss),
                                description = stringResource(id = R.string.close_notification_immediately_on_dialog_dismiss),
                                checked = uiState.disableNotificationForDialogInstall,
                                isM3E = false,
                                onCheckedChange = {
                                    viewModel.dispatch(
                                        InstallerSettingsAction.ChangeShowDisableNotification(it)
                                    )
                                }
                            )
                        }
                    }
                }
            }
            if (DeviceConfig.currentManufacturer == Manufacturer.OPPO || DeviceConfig.currentManufacturer == Manufacturer.ONEPLUS) {
                item { LabelWidget(stringResource(R.string.installer_oppo_related)) }
                item {
                    SwitchWidget(
                        icon = AppIcons.OEMSpecial,
                        title = stringResource(id = R.string.installer_show_oem_special),
                        description = stringResource(id = R.string.installer_show_oem_special_desc),
                        checked = uiState.showOPPOSpecial,
                        onCheckedChange = { viewModel.dispatch(InstallerSettingsAction.ChangeShowOPPOSpecial(it)) }
                    )
                }
            }
            item { LabelWidget(label = stringResource(id = R.string.config_managed_installer_packages_title)) }
            item {
                ManagedPackagesWidget(
                    noContentTitle = stringResource(R.string.config_no_preset_install_sources),
                    packages = uiState.managedInstallerPackages,
                    onAddPackage = { viewModel.dispatch(InstallerSettingsAction.AddManagedInstallerPackage(it)) },
                    onRemovePackage = {
                        viewModel.dispatch(
                            InstallerSettingsAction.RemoveManagedInstallerPackage(it)
                        )
                    })
            }
            item { LabelWidget(label = stringResource(id = R.string.config_managed_blacklist_by_package_name_title)) }
            item {
                ManagedPackagesWidget(
                    noContentTitle = stringResource(R.string.config_no_managed_blacklist),
                    packages = uiState.managedBlacklistPackages,
                    onAddPackage = { viewModel.dispatch(InstallerSettingsAction.AddManagedBlacklistPackage(it)) },
                    onRemovePackage = {
                        viewModel.dispatch(
                            InstallerSettingsAction.RemoveManagedBlacklistPackage(it)
                        )
                    })
            }
            item { LabelWidget(label = stringResource(id = R.string.config_managed_blacklist_by_shared_user_id_title)) }
            item {
                ManagedUidsWidget(
                    noContentTitle = stringResource(R.string.config_no_managed_shared_user_id_blacklist),
                    uids = uiState.managedSharedUserIdBlacklist,
                    onAddUid = {
                        viewModel.dispatch(InstallerSettingsAction.AddManagedSharedUserIdBlacklist(it))
                    },
                    onRemoveUid = {
                        viewModel.dispatch(InstallerSettingsAction.RemoveManagedSharedUserIdBlacklist(it))
                    }
                )
                AnimatedVisibility(
                    visible = uiState.managedSharedUserIdBlacklist.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    ManagedPackagesWidget(
                        noContentTitle = stringResource(R.string.config_no_managed_shared_user_id_exempted_packages),
                        noContentDescription = stringResource(R.string.config_shared_uid_prior_to_pkgname_desc),
                        packages = uiState.managedSharedUserIdExemptedPackages,
                        infoText = stringResource(R.string.config_no_managed_shared_user_id_exempted_packages),
                        isInfoVisible = uiState.managedSharedUserIdExemptedPackages.isNotEmpty(),
                        onAddPackage = {
                            viewModel.dispatch(
                                InstallerSettingsAction.AddManagedSharedUserIdExemptedPackages(
                                    it
                                )
                            )
                        },
                        onRemovePackage = {
                            viewModel.dispatch(
                                InstallerSettingsAction.RemoveManagedSharedUserIdExemptedPackages(
                                    it
                                )
                            )
                        }
                    )
                }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}
