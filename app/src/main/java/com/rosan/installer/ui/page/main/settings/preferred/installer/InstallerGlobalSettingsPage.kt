// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.installer

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.navigation.Route
import com.rosan.installer.ui.page.main.settings.preferred.DataInstallerBiometricAuthWidget
import com.rosan.installer.ui.page.main.settings.preferred.ManagedPackagesWidget
import com.rosan.installer.ui.page.main.settings.preferred.ManagedUidsWidget
import com.rosan.installer.ui.page.main.settings.preferred.SettingsNavigationItemWidget
import com.rosan.installer.ui.page.main.widget.setting.AppBackButton
import com.rosan.installer.ui.page.main.widget.setting.IntNumberPickerWidget
import com.rosan.installer.ui.page.main.widget.setting.SegmentedColumn
import com.rosan.installer.ui.page.main.widget.setting.SwitchWidget
import com.rosan.installer.ui.theme.getMaterial3AppBarColor
import com.rosan.installer.ui.theme.installerMaterial3BlurEffect
import com.rosan.installer.ui.theme.rememberMaterial3BlurBackdrop
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.blur.layerBackdrop

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InstallerGlobalSettingsPage(
    useBlur: Boolean,
    viewModel: InstallerSettingsViewModel = koinViewModel()
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val capabilityProvider = koinInject<DeviceCapabilityProvider>()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

    LaunchedEffect(Unit) {
        topAppBarState.heightOffset = topAppBarState.heightOffsetLimit
    }

    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    val backdrop = rememberMaterial3BlurBackdrop(useBlur)

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                modifier = Modifier.installerMaterial3BlurEffect(backdrop),
                windowInsets = TopAppBarDefaults.windowInsets.add(WindowInsets(left = 12.dp)),
                title = {
                    Text(stringResource(R.string.installer_settings))
                },
                navigationIcon = {
                    Row {
                        AppBackButton(
                            onClick = { navigator.pop() },
                            icon = Icons.AutoMirrored.TwoTone.ArrowBack,
                            modifier = Modifier.size(36.dp),
                            containerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        )
                        Spacer(modifier = Modifier.size(16.dp))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backdrop.getMaterial3AppBarColor(),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    scrolledContainerColor = backdrop.getMaterial3AppBarColor()
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
            contentPadding = PaddingValues(
                start = horizontalSafeInsets.calculateStartPadding(layoutDirection),
                top = paddingValues.calculateTopPadding(),
                end = horizontalSafeInsets.calculateEndPadding(layoutDirection),
                bottom = paddingValues.calculateBottomPadding()
            )
        ) {
            // --- Group 1: Global Installer Settings ---
            item {
                val biometricAvailable = remember {
                    BiometricManager.from(context)
                        .canAuthenticate(BIOMETRIC_WEAK or BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
                }

                SegmentedColumn(
                    title = stringResource(R.string.installer_settings_global_installer)
                ) {
                    item(visible = uiState.authorizer == Authorizer.None && capabilityProvider.isSystemApp) {
                        SwitchWidget(
                            icon = AppIcons.FlashPreferRoot,
                            title = stringResource(R.string.config_always_use_root_in_system),
                            description = stringResource(R.string.config_always_use_root_in_system_desc),
                            checked = uiState.alwaysUseRootInSystem,
                            onCheckedChange = { viewModel.dispatch(InstallerSettingsAction.ChangeAlwaysUseRootInSystem(it)) }
                        )
                    }

                    item(visible = uiState.authorizer == Authorizer.Dhizuku) {
                        IntNumberPickerWidget(
                            icon = AppIcons.Working,
                            title = stringResource(R.string.set_countdown),
                            description = stringResource(R.string.dhizuku_auto_close_countdown_desc),
                            value = uiState.dhizukuAutoCloseCountDown,
                            startInt = 1,
                            endInt = 10,
                            stepSize = 1,
                            onValueChange = {
                                viewModel.dispatch(InstallerSettingsAction.ChangeDhizukuAutoCloseCountDown(it))
                            }
                        )
                    }

                    item {
                        SettingsNavigationItemWidget(
                            icon = AppIcons.Dialog,
                            title = stringResource(R.string.dialog_settings),
                            description = stringResource(R.string.dialog_settings_desc),
                            onClick = { navigator.push(Route.DialogSettings) }
                        )
                    }

                    item {
                        SettingsNavigationItemWidget(
                            icon = AppIcons.Notification,
                            title = stringResource(R.string.notification_settings),
                            description = stringResource(R.string.notification_settings_desc),
                            onClick = { navigator.push(Route.NotificationSettings) }
                        )
                    }

                    item(visible = biometricAvailable) {
                        DataInstallerBiometricAuthWidget(
                            currentMode = uiState.installerRequireBiometricAuth,
                            onModeChange = {
                                viewModel.dispatch(InstallerSettingsAction.ChangeBiometricAuth(it))
                            }
                        )
                    }
                }
            }

            // --- Group 2: OPPO Related ---
            if (DeviceConfig.currentManufacturer == Manufacturer.OPPO || DeviceConfig.currentManufacturer == Manufacturer.ONEPLUS) {
                item {
                    SegmentedColumn(
                        title = stringResource(R.string.installer_oppo_related)
                    ) {
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
                }
            }

            // --- Group 3: Preset Installer Packages ---
            item {
                SegmentedColumn(
                    modifier = Modifier.padding(top = 8.dp),
                    title = stringResource(id = R.string.config_managed_installer_packages_title),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    item {
                        ManagedPackagesWidget(
                            noContentTitle = stringResource(R.string.config_no_preset_install_sources),
                            packages = uiState.managedInstallerPackages,
                            onAddPackage = { viewModel.dispatch(InstallerSettingsAction.AddManagedInstallerPackage(it)) },
                            onRemovePackage = { viewModel.dispatch(InstallerSettingsAction.RemoveManagedInstallerPackage(it)) },
                            onMovePackage = { fromIndex, toIndex ->
                                viewModel.dispatch(InstallerSettingsAction.MoveManagedInstallerPackage(fromIndex, toIndex))
                            }
                        )
                    }
                }
            }

            // --- Group 4: Managed Blacklist ---
            item {
                SegmentedColumn(
                    title = stringResource(id = R.string.config_managed_blacklist_by_package_name_title),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    item {
                        ManagedPackagesWidget(
                            noContentTitle = stringResource(R.string.config_no_managed_blacklist),
                            packages = uiState.managedBlacklistPackages,
                            onAddPackage = { viewModel.dispatch(InstallerSettingsAction.AddManagedBlacklistPackage(it)) },
                            onRemovePackage = { viewModel.dispatch(InstallerSettingsAction.RemoveManagedBlacklistPackage(it)) },
                            onMovePackage = { fromIndex, toIndex ->
                                viewModel.dispatch(InstallerSettingsAction.MoveManagedBlacklistPackage(fromIndex, toIndex))
                            }
                        )
                    }
                }
            }

            // --- Group 5: Managed Shared User IDs ---
            item {
                SegmentedColumn(
                    title = stringResource(R.string.config_managed_blacklist_by_shared_user_id_title),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    item {
                        ManagedUidsWidget(
                            noContentTitle = stringResource(R.string.config_no_managed_shared_user_id_blacklist),
                            uids = uiState.managedSharedUserIdBlacklist,
                            onAddUid = { viewModel.dispatch(InstallerSettingsAction.AddManagedSharedUserIdBlacklist(it)) },
                            onRemoveUid = { viewModel.dispatch(InstallerSettingsAction.RemoveManagedSharedUserIdBlacklist(it)) },
                            onMoveUid = { from, to ->
                                viewModel.dispatch(InstallerSettingsAction.MoveManagedSharedUserIdBlacklist(from, to))
                            }
                        )
                    }

                    // Show exempted packages only if UID blacklist is not empty
                    item(visible = uiState.managedSharedUserIdBlacklist.isNotEmpty()) {
                        ManagedPackagesWidget(
                            noContentTitle = stringResource(R.string.config_no_managed_shared_user_id_exempted_packages),
                            packages = uiState.managedSharedUserIdExemptedPackages,
                            infoText = stringResource(R.string.config_no_managed_shared_user_id_exempted_packages),
                            isInfoVisible = uiState.managedSharedUserIdExemptedPackages.isNotEmpty(),
                            onAddPackage = { viewModel.dispatch(InstallerSettingsAction.AddManagedSharedUserIdExemptedPackages(it)) },
                            onRemovePackage = {
                                viewModel.dispatch(
                                    InstallerSettingsAction.RemoveManagedSharedUserIdExemptedPackages(
                                        it
                                    )
                                )
                            },
                            onMovePackage = { fromIndex, toIndex ->
                                viewModel.dispatch(InstallerSettingsAction.MoveManagedSharedUserIdExemptedPackages(fromIndex, toIndex))
                            }
                        )
                    }
                }
            }

            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}
