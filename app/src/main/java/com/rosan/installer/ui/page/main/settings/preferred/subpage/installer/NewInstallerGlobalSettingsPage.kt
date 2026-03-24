package com.rosan.installer.ui.page.main.settings.preferred.subpage.installer

import android.os.Build
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import com.rosan.installer.ui.page.main.widget.setting.ManagedPackagesWidget
import com.rosan.installer.ui.page.main.widget.setting.ManagedUidsWidget
import com.rosan.installer.ui.page.main.widget.setting.SplicedColumnGroup
import com.rosan.installer.ui.page.main.widget.setting.SwitchWidget
import com.rosan.installer.ui.theme.getM3TopBarColor
import com.rosan.installer.ui.theme.installerHazeEffect
import com.rosan.installer.ui.theme.none
import com.rosan.installer.ui.theme.rememberMaterial3HazeStyle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewInstallerGlobalSettingsPage(
    navController: NavController,
    viewModel: InstallerSettingsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val topAppBarState = rememberTopAppBarState()
    val hazeState = if (uiState.useBlur) remember { HazeState() } else null
    val hazeStyle = rememberMaterial3HazeStyle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

    LaunchedEffect(Unit) {
        topAppBarState.heightOffset = topAppBarState.heightOffsetLimit
    }

    val isDialogMode = uiState.installMode == InstallMode.Dialog ||
            uiState.installMode == InstallMode.AutoDialog
    val isNotificationMode = uiState.installMode == InstallMode.Notification ||
            uiState.installMode == InstallMode.AutoNotification

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        contentWindowInsets = WindowInsets.none,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                modifier = Modifier.installerHazeEffect(hazeState, hazeStyle),
                windowInsets = TopAppBarDefaults.windowInsets.add(WindowInsets(left = 12.dp)),
                title = {
                    Text(stringResource(R.string.installer_settings))
                },
                navigationIcon = {
                    Row {
                        AppBackButton(
                            onClick = { navController.navigateUp() },
                            icon = Icons.AutoMirrored.TwoTone.ArrowBack,
                            modifier = Modifier.size(36.dp),
                            containerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        )
                        Spacer(modifier = Modifier.size(16.dp))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = hazeState.getM3TopBarColor(),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    scrolledContainerColor = hazeState.getM3TopBarColor()
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(hazeState?.let { Modifier.hazeSource(it) } ?: Modifier),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding()
            )
        ) {
            // --- Group 1: Global Installer Settings ---
            item {
                val biometricAvailable = remember {
                    BiometricManager.from(context)
                        .canAuthenticate(BIOMETRIC_WEAK or BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
                }

                SplicedColumnGroup(
                    title = stringResource(R.string.installer_settings_global_installer)
                ) {
                    item {
                        DataAuthorizerWidget(
                            currentAuthorizer = uiState.authorizer,
                            changeAuthorizer = {
                                viewModel.dispatch(InstallerSettingsAction.ChangeGlobalAuthorizer(it))
                            }
                        ) {
                            // Nesting specific animations inside a widget is fine if the widget supports it
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
                                    onValueChange = {
                                        viewModel.dispatch(InstallerSettingsAction.ChangeDhizukuAutoCloseCountDown(it))
                                    }
                                )
                            }
                        }
                    }

                    item {
                        DataInstallModeWidget(
                            currentInstallMode = uiState.installMode,
                            changeInstallMode = { viewModel.dispatch(InstallerSettingsAction.ChangeGlobalInstallMode(it)) }
                        )
                    }

                    // Using DSL 'visible' parameter for version check
                    item(visible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                        SwitchWidget(
                            icon = AppIcons.LiveActivity,
                            title = stringResource(R.string.theme_settings_use_live_activity),
                            description = stringResource(R.string.theme_settings_use_live_activity_desc),
                            checked = uiState.showLiveActivity,
                            onCheckedChange = { viewModel.dispatch(InstallerSettingsAction.ChangeShowLiveActivity(it)) }
                        )
                    }

                    // Using DSL 'visible' parameter for Biometric check
                    item(visible = biometricAvailable) {
                        SwitchWidget(
                            icon = AppIcons.BiometricAuth,
                            title = stringResource(R.string.installer_settings_require_biometric_auth),
                            description = stringResource(R.string.installer_settings_require_biometric_auth_desc),
                            checked = uiState.installerRequireBiometricAuth,
                            isM3E = true,
                            onCheckedChange = { viewModel.dispatch(InstallerSettingsAction.ChangeBiometricAuth(it)) }
                        )
                    }

                    item {
                        AutoClearNotificationTimeWidget(
                            currentValue = uiState.notificationSuccessAutoClearSeconds,
                            onValueChange = { seconds ->
                                viewModel.dispatch(InstallerSettingsAction.ChangeNotificationSuccessAutoClearSeconds(seconds))
                            }
                        )
                    }
                }
            }

            // --- Group 2: Dialog / Notification Mode Options (Refactored) ---
            item {
                AnimatedVisibility(
                    visible = isDialogMode || isNotificationMode,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    // Determine title based on mode
                    val optionsTitle = if (isDialogMode) {
                        stringResource(R.string.installer_settings_dialog_mode_options)
                    } else {
                        stringResource(R.string.installer_settings_notification_mode_options)
                    }

                    SplicedColumnGroup(
                        title = optionsTitle
                    ) {
                        // 1. Version Compare (Dialog Mode)
                        item(visible = isDialogMode) {
                            SwitchWidget(
                                icon = AppIcons.SingleLineSettingIcon,
                                title = stringResource(id = R.string.version_compare_in_single_line),
                                description = stringResource(id = R.string.version_compare_in_single_line_desc),
                                checked = uiState.versionCompareInSingleLine,
                                onCheckedChange = { viewModel.dispatch(InstallerSettingsAction.ChangeVersionCompareInSingleLine(it)) }
                            )
                        }

                        // 2. SDK Compare (Dialog Mode)
                        item(visible = isDialogMode) {
                            SwitchWidget(
                                icon = AppIcons.MultiLineSettingIcon,
                                title = stringResource(id = R.string.sdk_compare_in_multi_line),
                                description = stringResource(id = R.string.sdk_compare_in_multi_line_desc),
                                checked = uiState.sdkCompareInMultiLine,
                                onCheckedChange = { viewModel.dispatch(InstallerSettingsAction.ChangeSdkCompareInMultiLine(it)) }
                            )
                        }

                        // 3. Extended Menu (Strictly Dialog Mode)
                        item(visible = uiState.installMode == InstallMode.Dialog) {
                            SwitchWidget(
                                icon = AppIcons.MenuOpen,
                                title = stringResource(id = R.string.show_dialog_install_extended_menu),
                                description = stringResource(id = R.string.show_dialog_install_extended_menu_desc),
                                checked = uiState.showDialogInstallExtendedMenu,
                                onCheckedChange = { viewModel.dispatch(InstallerSettingsAction.ChangeShowDialogInstallExtendedMenu(it)) }
                            )
                        }

                        // 4. Smart Suggestion (Dialog Mode)
                        item(visible = isDialogMode) {
                            SwitchWidget(
                                icon = AppIcons.Suggestion,
                                title = stringResource(id = R.string.show_intelligent_suggestion),
                                description = stringResource(id = R.string.show_intelligent_suggestion_desc),
                                checked = uiState.showSmartSuggestion,
                                onCheckedChange = { viewModel.dispatch(InstallerSettingsAction.ChangeShowSuggestion(it)) }
                            )
                        }

                        // 5. Show Dialog on Touch (Notification Mode)
                        item(visible = isNotificationMode) {
                            SwitchWidget(
                                icon = AppIcons.Dialog,
                                title = stringResource(id = R.string.show_dialog_when_pressing_notification),
                                description = stringResource(id = R.string.change_notification_touch_behavior),
                                checked = uiState.showDialogWhenPressingNotification,
                                onCheckedChange = {
                                    viewModel.dispatch(
                                        InstallerSettingsAction.ChangeShowDialogWhenPressingNotification(
                                            it
                                        )
                                    )
                                }
                            )
                        }

                        // 6. Auto Silent Install (Dialog Mode)
                        item(visible = isDialogMode) {
                            SwitchWidget(
                                icon = AppIcons.Silent,
                                title = stringResource(id = R.string.auto_silent_install),
                                description = stringResource(id = R.string.auto_silent_install_desc),
                                checked = uiState.autoSilentInstall,
                                onCheckedChange = { viewModel.dispatch(InstallerSettingsAction.ChangeAutoSilentInstall(it)) }
                            )
                        }

                        // 7. Disable Notification
                        item(visible = isDialogMode || uiState.showDialogWhenPressingNotification) {
                            SwitchWidget(
                                icon = AppIcons.NotificationDisabled,
                                title = stringResource(id = R.string.disable_notification_on_dismiss),
                                description = stringResource(id = R.string.close_notification_immediately_on_dialog_dismiss),
                                checked = uiState.disableNotificationForDialogInstall,
                                onCheckedChange = { viewModel.dispatch(InstallerSettingsAction.ChangeShowDisableNotification(it)) }
                            )
                        }
                    }
                }
            }

            // --- Group 3: OPPO Related ---
            if (DeviceConfig.currentManufacturer == Manufacturer.OPPO || DeviceConfig.currentManufacturer == Manufacturer.ONEPLUS) {
                item {
                    SplicedColumnGroup(
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

            // --- Group 4: Managed Installer Packages ---
            item {
                SplicedColumnGroup(
                    title = stringResource(id = R.string.config_managed_installer_packages_title)
                ) {
                    item {
                        ManagedPackagesWidget(
                            noContentTitle = stringResource(R.string.config_no_preset_install_sources),
                            packages = uiState.managedInstallerPackages,
                            onAddPackage = { viewModel.dispatch(InstallerSettingsAction.AddManagedInstallerPackage(it)) },
                            onRemovePackage = { viewModel.dispatch(InstallerSettingsAction.RemoveManagedInstallerPackage(it)) }
                        )
                    }
                }
            }

            // --- Group 5: Managed Blacklist ---
            item {
                SplicedColumnGroup(
                    title = stringResource(id = R.string.config_managed_blacklist_by_package_name_title)
                ) {
                    item {
                        ManagedPackagesWidget(
                            noContentTitle = stringResource(R.string.config_no_managed_blacklist),
                            packages = uiState.managedBlacklistPackages,
                            onAddPackage = { viewModel.dispatch(InstallerSettingsAction.AddManagedBlacklistPackage(it)) },
                            onRemovePackage = { viewModel.dispatch(InstallerSettingsAction.RemoveManagedBlacklistPackage(it)) }
                        )
                    }
                }
            }

            // --- Group 6: Managed Shared User IDs ---
            item {
                SplicedColumnGroup(
                    title = stringResource(R.string.config_managed_blacklist_by_shared_user_id_title)
                ) {
                    item {
                        ManagedUidsWidget(
                            noContentTitle = stringResource(R.string.config_no_managed_shared_user_id_blacklist),
                            uids = uiState.managedSharedUserIdBlacklist,
                            onAddUid = { viewModel.dispatch(InstallerSettingsAction.AddManagedSharedUserIdBlacklist(it)) },
                            onRemoveUid = { viewModel.dispatch(InstallerSettingsAction.RemoveManagedSharedUserIdBlacklist(it)) }
                        )
                    }

                    // Show exempted packages only if UID blacklist is not empty
                    item(visible = uiState.managedSharedUserIdBlacklist.isNotEmpty()) {
                        ManagedPackagesWidget(
                            noContentTitle = stringResource(R.string.config_no_managed_shared_user_id_exempted_packages),
                            noContentDescription = stringResource(R.string.config_shared_uid_prior_to_pkgname_desc),
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
                            }
                        )
                    }
                }
            }

            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}
