// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.settings.preferred.installer

import android.annotation.SuppressLint
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import com.rosan.installer.domain.settings.model.BiometricAuthMode
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.navigation.Route
import com.rosan.installer.ui.page.main.settings.preferred.installer.InstallerSettingsAction
import com.rosan.installer.ui.page.main.settings.preferred.installer.InstallerSettingsViewModel
import com.rosan.installer.ui.page.miuix.settings.preferred.MiuixManagedPackagesWidget
import com.rosan.installer.ui.page.miuix.settings.preferred.MiuixManagedUidsWidget
import com.rosan.installer.ui.page.miuix.settings.preferred.MiuixNavigationItemWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import com.rosan.installer.ui.page.miuix.widgets.MiuixIntNumberPickerWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixSwitchWidget
import com.rosan.installer.ui.theme.getMiuixAppBarColor
import com.rosan.installer.ui.theme.installerMiuixBlurEffect
import com.rosan.installer.ui.theme.rememberMiuixBlurBackdrop
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun MiuixInstallerGlobalSettingsPage(
    useBlur: Boolean,
    viewModel: InstallerSettingsViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val capabilityProvider = koinInject<DeviceCapabilityProvider>()
    val scrollBehavior = MiuixScrollBehavior()

    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    val topBarBackdrop = rememberMiuixBlurBackdrop(useBlur)

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.installerMiuixBlurEffect(topBarBackdrop),
                color = topBarBackdrop.getMiuixAppBarColor(),
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
                .then(topBarBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)
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
                    MiuixNavigationItemWidget(
                        title = stringResource(R.string.dialog_settings),
                        description = stringResource(R.string.dialog_settings_desc),
                        onClick = { navigator.push(Route.DialogSettings) }
                    )
                    MiuixNavigationItemWidget(
                        title = stringResource(R.string.notification_settings),
                        description = stringResource(R.string.notification_settings_desc),
                        onClick = { navigator.push(Route.NotificationSettings) }
                    )

                    if (BiometricManager
                            .from(context)
                            .canAuthenticate(BIOMETRIC_WEAK or BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
                    ) {
                        val biometricModes = remember {
                            listOf(
                                BiometricAuthMode.Disable,
                                BiometricAuthMode.Enable,
                                BiometricAuthMode.FollowConfig
                            )
                        }

                        val entries = remember(biometricModes) {
                            biometricModes.map { mode ->
                                val text = when (mode) {
                                    BiometricAuthMode.Disable -> context.getString(R.string.installer_biometric_auth_mode_disable)
                                    BiometricAuthMode.Enable -> context.getString(R.string.installer_biometric_auth_mode_enable)
                                    BiometricAuthMode.FollowConfig -> context.getString(R.string.installer_biometric_auth_mode_follow_config)
                                }
                                DropdownItem(title = text)
                            }
                        }

                        val selectedIndex = remember(uiState.installerRequireBiometricAuth, biometricModes) {
                            biometricModes.indexOf(uiState.installerRequireBiometricAuth).coerceAtLeast(0)
                        }

                        val dynamicSummary = when (biometricModes[selectedIndex]) {
                            BiometricAuthMode.Disable -> stringResource(R.string.installer_biometric_auth_mode_disable_desc)
                            BiometricAuthMode.Enable -> stringResource(R.string.installer_biometric_auth_mode_enable_desc)
                            BiometricAuthMode.FollowConfig -> stringResource(R.string.installer_biometric_auth_mode_follow_config_desc)
                        }

                        WindowSpinnerPreference(
                            title = stringResource(R.string.installer_settings_require_biometric_auth),
                            summary = dynamicSummary,
                            items = entries,
                            selectedIndex = selectedIndex,
                            onSelectedIndexChange = { index ->
                                viewModel.dispatch(
                                    InstallerSettingsAction.ChangeBiometricAuth(biometricModes[index])
                                )
                            }
                        )
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
