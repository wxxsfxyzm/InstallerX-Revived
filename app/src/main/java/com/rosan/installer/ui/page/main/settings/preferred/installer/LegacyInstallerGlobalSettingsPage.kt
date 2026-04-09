// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.installer

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.core.env.DeviceConfig
import com.rosan.installer.domain.device.model.Manufacturer
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.InstallMode
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.navigation.Route
import com.rosan.installer.ui.page.main.settings.preferred.DataAuthorizerWidget
import com.rosan.installer.ui.page.main.settings.preferred.DataInstallModeWidget
import com.rosan.installer.ui.page.main.settings.preferred.ManagedPackagesWidget
import com.rosan.installer.ui.page.main.settings.preferred.ManagedUidsWidget
import com.rosan.installer.ui.page.main.settings.preferred.SettingsNavigationItemWidget
import com.rosan.installer.ui.page.main.widget.setting.AppBackButton
import com.rosan.installer.ui.page.main.widget.setting.IntNumberPickerWidget
import com.rosan.installer.ui.page.main.widget.setting.LabelWidget
import com.rosan.installer.ui.page.main.widget.setting.SwitchWidget
import com.rosan.installer.ui.theme.none
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegacyInstallerGlobalSettingsPage(
    viewModel: InstallerSettingsViewModel = koinViewModel(),
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val capabilityProvider = koinInject<DeviceCapabilityProvider>()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.none,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.installer_settings)) },
                navigationIcon = {
                    AppBackButton(onClick = { navigator.pop() })
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = horizontalSafeInsets.calculateStartPadding(layoutDirection),
                top = paddingValues.calculateTopPadding(),
                end = horizontalSafeInsets.calculateEndPadding(layoutDirection),
                bottom = paddingValues.calculateBottomPadding()
            )
        ) {
            item { LabelWidget(stringResource(R.string.installer_settings_global_installer)) }
            item {
                DataAuthorizerWidget(
                    currentAuthorizer = uiState.authorizer,
                    changeAuthorizer = { newAuthorizer ->
                        viewModel.dispatch(InstallerSettingsAction.ChangeGlobalAuthorizer(newAuthorizer))
                    }
                ) {
                    AnimatedVisibility(
                        visible = uiState.authorizer == Authorizer.None && capabilityProvider.isSystemApp,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        SwitchWidget(
                            icon = AppIcons.FlashPreferRoot,
                            title = stringResource(R.string.config_always_use_root_in_system),
                            description = stringResource(R.string.config_always_use_root_in_system_desc),
                            isM3E = false,
                            checked = uiState.alwaysUseRootInSystem,
                            onCheckedChange = { viewModel.dispatch(InstallerSettingsAction.ChangeAlwaysUseRootInSystem(it)) }
                        )
                    }
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
                            endInt = 10,
                            showTooltip = false
                        ) {
                            viewModel.dispatch(
                                InstallerSettingsAction.ChangeDhizukuAutoCloseCountDown(it)
                            )
                        }
                    }
                }
            }
            item {
                DataInstallModeWidget(
                    currentInstallMode = uiState.installMode,
                    changeInstallMode = {
                        viewModel.dispatch(InstallerSettingsAction.ChangeGlobalInstallMode(it))
                    }
                )
            }
            val isNotificationMode = uiState.installMode == InstallMode.Notification || uiState.installMode == InstallMode.AutoNotification
            item {
                AnimatedVisibility(
                    visible = isNotificationMode,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    SettingsNavigationItemWidget(
                        icon = AppIcons.Notification,
                        title = stringResource(R.string.notification_settings),
                        description = stringResource(R.string.notification_settings_desc),
                        onClick = { navigator.push(Route.NotificationSettings) }
                    )
                }
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

                AnimatedVisibility(
                    visible = isDialogMode,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(modifier = Modifier.animateContentSize()) {
                        LabelWidget(label = stringResource(id = R.string.installer_settings_dialog_mode_options))

                        SwitchWidget(
                            icon = AppIcons.MultiLineSettingIcon,
                            title = stringResource(id = R.string.version_compare_in_single_line),
                            description = stringResource(id = R.string.version_compare_in_single_line_desc),
                            checked = uiState.versionCompareInSingleLine,
                            isM3E = false,
                            onCheckedChange = {
                                viewModel.dispatch(
                                    InstallerSettingsAction.ChangeVersionCompareInSingleLine(it)
                                )
                            }
                        )

                        SwitchWidget(
                            icon = AppIcons.SingleLineSettingIcon,
                            title = stringResource(id = R.string.sdk_compare_in_multi_line),
                            description = stringResource(id = R.string.sdk_compare_in_multi_line_desc),
                            checked = uiState.sdkCompareInMultiLine,
                            isM3E = false,
                            onCheckedChange = {
                                viewModel.dispatch(
                                    InstallerSettingsAction.ChangeSdkCompareInMultiLine(it)
                                )
                            }
                        )

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

                        SwitchWidget(
                            icon = AppIcons.Silent,
                            title = stringResource(id = R.string.auto_silent_install),
                            description = stringResource(id = R.string.auto_silent_install_desc),
                            checked = uiState.autoSilentInstall,
                            onCheckedChange = {
                                viewModel.dispatch(InstallerSettingsAction.ChangeAutoSilentInstall(it))
                            }
                        )

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
