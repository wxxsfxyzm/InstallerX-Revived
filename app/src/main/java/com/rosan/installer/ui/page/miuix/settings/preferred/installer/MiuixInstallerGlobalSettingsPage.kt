// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.settings.preferred.installer

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.core.env.DeviceConfig
import com.rosan.installer.domain.device.model.Manufacturer
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.InstallMode
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.navigation.Route
import com.rosan.installer.ui.page.main.settings.preferred.installer.InstallerSettingsAction
import com.rosan.installer.ui.page.main.settings.preferred.installer.InstallerSettingsViewModel
import com.rosan.installer.ui.page.miuix.settings.preferred.MiuixDataAuthorizerWidget
import com.rosan.installer.ui.page.miuix.settings.preferred.MiuixDataInstallModeWidget
import com.rosan.installer.ui.page.miuix.settings.preferred.MiuixManagedPackagesWidget
import com.rosan.installer.ui.page.miuix.settings.preferred.MiuixManagedUidsWidget
import com.rosan.installer.ui.page.miuix.settings.preferred.MiuixNavigationItemWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import com.rosan.installer.ui.page.miuix.widgets.MiuixIntNumberPickerWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixSwitchWidget
import com.rosan.installer.ui.theme.getMiuixAppBarColor
import com.rosan.installer.ui.theme.installerHazeEffect
import com.rosan.installer.ui.theme.rememberMiuixHazeStyle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun MiuixInstallerGlobalSettingsPage(
    viewModel: InstallerSettingsViewModel = koinViewModel(),
) {
    val navigator = LocalNavigator.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val capabilityProvider = koinInject<DeviceCapabilityProvider>()
    val scrollBehavior = MiuixScrollBehavior()
    val hazeState = if (uiState.useBlur) remember { HazeState() } else null
    val hazeStyle = rememberMiuixHazeStyle()

    val isDialogMode = uiState.installMode == InstallMode.Dialog ||
            uiState.installMode == InstallMode.AutoDialog
    val isNotificationMode = uiState.installMode == InstallMode.Notification ||
            uiState.installMode == InstallMode.AutoNotification

    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.installerHazeEffect(hazeState, hazeStyle),
                color = hazeState.getMiuixAppBarColor(),
                title = stringResource(R.string.installer_settings),
                navigationIcon = {
                    MiuixBackButton(onClick = { navigator.pop() })
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(hazeState?.let { Modifier.hazeSource(it) } ?: Modifier)
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                start = horizontalSafeInsets.calculateStartPadding(layoutDirection),
                top = paddingValues.calculateTopPadding(),
                end = horizontalSafeInsets.calculateEndPadding(layoutDirection)
            ),
            overscrollEffect = null
        ) {
            item { Spacer(modifier = Modifier.size(12.dp)) }
            item { SmallTitle(stringResource(R.string.installer_settings_global_installer)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    MiuixDataAuthorizerWidget(
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
                            MiuixSwitchWidget(
                                title = stringResource(R.string.config_always_use_root_in_system),
                                description = stringResource(R.string.config_always_use_root_in_system_desc),
                                checked = uiState.alwaysUseRootInSystem,
                                onCheckedChange = { viewModel.dispatch(InstallerSettingsAction.ChangeAlwaysUseRootInSystem(it)) }
                            )
                        }
                        AnimatedVisibility(
                            visible = uiState.authorizer == Authorizer.Dhizuku,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            MiuixIntNumberPickerWidget(
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
                    MiuixDataInstallModeWidget(
                        currentInstallMode = uiState.installMode,
                        changeInstallMode = { newMode ->
                            viewModel.dispatch(InstallerSettingsAction.ChangeGlobalInstallMode(newMode))
                        }
                    ) {
                        AnimatedVisibility(
                            visible = isNotificationMode,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            MiuixNavigationItemWidget(
                                title = stringResource(R.string.notification_settings),
                                description = stringResource(R.string.notification_settings_desc),
                                onClick = { navigator.push(Route.NotificationSettings) }
                            )
                        }

                        if (BiometricManager
                                .from(LocalContext.current)
                                .canAuthenticate(BIOMETRIC_WEAK or BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
                        ) {
                            MiuixSwitchWidget(
                                title = stringResource(R.string.installer_settings_require_biometric_auth),
                                description = stringResource(R.string.installer_settings_require_biometric_auth_desc),
                                checked = uiState.installerRequireBiometricAuth,
                                onCheckedChange = {
                                    viewModel.dispatch(InstallerSettingsAction.ChangeBiometricAuth(it))
                                }
                            )
                        }
                    }
                }
            }

            item {
                AnimatedVisibility(
                    visible = isDialogMode,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        SmallTitle(stringResource(id = R.string.installer_settings_dialog_mode_options))
                        Card(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp)
                        ) {
                            MiuixSwitchWidget(
                                title = stringResource(id = R.string.version_compare_in_single_line),
                                description = stringResource(id = R.string.version_compare_in_single_line_desc),
                                checked = uiState.versionCompareInSingleLine,
                                onCheckedChange = {
                                    viewModel.dispatch(InstallerSettingsAction.ChangeVersionCompareInSingleLine(it))
                                }
                            )

                            MiuixSwitchWidget(
                                title = stringResource(id = R.string.sdk_compare_in_multi_line),
                                description = stringResource(id = R.string.sdk_compare_in_multi_line_desc),
                                checked = uiState.sdkCompareInMultiLine,
                                onCheckedChange = {
                                    viewModel.dispatch(InstallerSettingsAction.ChangeSdkCompareInMultiLine(it))
                                }
                            )

                            AnimatedVisibility(
                                visible = uiState.installMode == InstallMode.Dialog,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                MiuixSwitchWidget(
                                    title = stringResource(id = R.string.show_dialog_install_extended_menu),
                                    description = stringResource(id = R.string.show_dialog_install_extended_menu_desc),
                                    checked = uiState.showDialogInstallExtendedMenu,
                                    onCheckedChange = {
                                        viewModel.dispatch(InstallerSettingsAction.ChangeShowDialogInstallExtendedMenu(it))
                                    }
                                )
                            }

                            MiuixSwitchWidget(
                                title = stringResource(id = R.string.show_intelligent_suggestion),
                                description = stringResource(id = R.string.show_intelligent_suggestion_desc),
                                checked = uiState.showSmartSuggestion,
                                onCheckedChange = {
                                    viewModel.dispatch(InstallerSettingsAction.ChangeShowSuggestion(it))
                                }
                            )

                            MiuixSwitchWidget(
                                title = stringResource(id = R.string.auto_silent_install),
                                description = stringResource(id = R.string.auto_silent_install_desc),
                                checked = uiState.autoSilentInstall,
                                onCheckedChange = {
                                    viewModel.dispatch(InstallerSettingsAction.ChangeAutoSilentInstall(it))
                                }
                            )

                            MiuixSwitchWidget(
                                title = stringResource(id = R.string.disable_notification_on_dismiss),
                                description = stringResource(id = R.string.close_notification_immediately_on_dialog_dismiss),
                                checked = uiState.disableNotificationForDialogInstall,
                                onCheckedChange = {
                                    viewModel.dispatch(InstallerSettingsAction.ChangeShowDisableNotification(it))
                                }
                            )
                        }
                    }
                }
            }

            if (DeviceConfig.currentManufacturer == Manufacturer.OPPO || DeviceConfig.currentManufacturer == Manufacturer.ONEPLUS) {
                item { SmallTitle(stringResource(R.string.installer_oppo_related)) }
                item {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        MiuixSwitchWidget(
                            title = stringResource(id = R.string.installer_show_oem_special),
                            description = stringResource(id = R.string.installer_show_oem_special_desc),
                            checked = uiState.showOPPOSpecial,
                            onCheckedChange = { viewModel.dispatch(InstallerSettingsAction.ChangeShowOPPOSpecial(it)) }
                        )
                    }
                }
            }

            item { SmallTitle(stringResource(R.string.config_managed_installer_packages_title)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    MiuixManagedPackagesWidget(
                        noContentTitle = stringResource(R.string.config_no_preset_install_sources),
                        packages = uiState.managedInstallerPackages,
                        onAddPackage = { viewModel.dispatch(InstallerSettingsAction.AddManagedInstallerPackage(it)) },
                        onRemovePackage = {
                            viewModel.dispatch(
                                InstallerSettingsAction.RemoveManagedInstallerPackage(it)
                            )
                        }
                    )
                }
            }

            item { SmallTitle(stringResource(R.string.config_managed_blacklist_by_package_name_title)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    MiuixManagedPackagesWidget(
                        noContentTitle = stringResource(R.string.config_no_managed_blacklist),
                        packages = uiState.managedBlacklistPackages,
                        onAddPackage = { viewModel.dispatch(InstallerSettingsAction.AddManagedBlacklistPackage(it)) },
                        onRemovePackage = {
                            viewModel.dispatch(
                                InstallerSettingsAction.RemoveManagedBlacklistPackage(it)
                            )
                        }
                    )
                }
            }

            item { SmallTitle(stringResource(R.string.config_managed_blacklist_by_shared_user_id_title)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 16.dp)
                ) {
                    MiuixManagedUidsWidget(
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
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline
                        )
                        MiuixManagedPackagesWidget(
                            noContentTitle = stringResource(R.string.config_no_managed_shared_user_id_exempted_packages),
                            noContentDescription = stringResource(R.string.config_shared_uid_prior_to_pkgname_desc),
                            packages = uiState.managedSharedUserIdExemptedPackages,
                            infoText = stringResource(R.string.config_no_managed_shared_user_id_exempted_packages),
                            isInfoVisible = uiState.managedSharedUserIdExemptedPackages.isNotEmpty(),
                            onAddPackage = {
                                viewModel.dispatch(
                                    InstallerSettingsAction.AddManagedSharedUserIdExemptedPackages(it)
                                )
                            },
                            onRemovePackage = {
                                viewModel.dispatch(
                                    InstallerSettingsAction.RemoveManagedSharedUserIdExemptedPackages(it)
                                )
                            }
                        )
                    }
                }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}
