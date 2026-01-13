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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rosan.installer.R
import com.rosan.installer.build.RsConfig
import com.rosan.installer.build.model.entity.Manufacturer
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewAction
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.page.main.widget.setting.AppBackButton
import com.rosan.installer.ui.page.main.widget.setting.AutoClearNotificationTimeWidget
import com.rosan.installer.ui.page.main.widget.setting.DataAuthorizerWidget
import com.rosan.installer.ui.page.main.widget.setting.DataInstallModeWidget
import com.rosan.installer.ui.page.main.widget.setting.IntNumberPickerWidget
import com.rosan.installer.ui.page.main.widget.setting.ManagedPackagesWidget
import com.rosan.installer.ui.page.main.widget.setting.ManagedUidsWidget
import com.rosan.installer.ui.page.main.widget.setting.SplicedColumnGroup
import com.rosan.installer.ui.page.main.widget.setting.SwitchWidget
import com.rosan.installer.ui.theme.none

@Immutable
private data class DynamicSettingItem(
    val visible: Boolean,
    val content: @Composable () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewInstallerGlobalSettingsPage(
    navController: NavController,
    viewModel: PreferredViewModel
) {
    val context = LocalContext.current
    val state = viewModel.state
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

    LaunchedEffect(Unit) {
        topAppBarState.heightOffset = topAppBarState.heightOffsetLimit
    }

    val isDialogMode = state.installMode == ConfigEntity.InstallMode.Dialog ||
            state.installMode == ConfigEntity.InstallMode.AutoDialog
    val isNotificationMode = state.installMode == ConfigEntity.InstallMode.Notification ||
            state.installMode == ConfigEntity.InstallMode.AutoNotification

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        contentWindowInsets = WindowInsets.none,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
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
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                            currentAuthorizer = state.authorizer,
                            changeAuthorizer = {
                                viewModel.dispatch(PreferredViewAction.ChangeGlobalAuthorizer(it))
                            }
                        ) {
                            // Nesting specific animations inside a widget is fine if the widget supports it
                            AnimatedVisibility(
                                visible = state.authorizer == ConfigEntity.Authorizer.Dhizuku,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                IntNumberPickerWidget(
                                    icon = AppIcons.Working,
                                    title = stringResource(R.string.set_countdown),
                                    description = stringResource(R.string.dhizuku_auto_close_countdown_desc),
                                    value = state.dhizukuAutoCloseCountDown,
                                    startInt = 1,
                                    endInt = 10,
                                    onValueChange = {
                                        viewModel.dispatch(PreferredViewAction.ChangeDhizukuAutoCloseCountDown(it))
                                    }
                                )
                            }
                        }
                    }

                    item {
                        DataInstallModeWidget(
                            currentInstallMode = state.installMode,
                            changeInstallMode = { viewModel.dispatch(PreferredViewAction.ChangeGlobalInstallMode(it)) }
                        )
                    }

                    // Using DSL 'visible' parameter for version check
                    item(visible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                        SwitchWidget(
                            icon = AppIcons.LiveActivity,
                            title = stringResource(R.string.theme_settings_use_live_activity),
                            description = stringResource(R.string.theme_settings_use_live_activity_desc),
                            checked = state.showLiveActivity,
                            onCheckedChange = { viewModel.dispatch(PreferredViewAction.ChangeShowLiveActivity(it)) }
                        )
                    }

                    // Using DSL 'visible' parameter for Biometric check
                    item(visible = biometricAvailable) {
                        SwitchWidget(
                            icon = AppIcons.BiometricAuth,
                            title = stringResource(R.string.installer_settings_require_biometric_auth),
                            description = stringResource(R.string.installer_settings_require_biometric_auth_desc),
                            checked = state.installerRequireBiometricAuth,
                            isM3E = true,
                            onCheckedChange = { viewModel.dispatch(PreferredViewAction.ChangeBiometricAuth(it, true)) }
                        )
                    }

                    item {
                        AutoClearNotificationTimeWidget(
                            currentValue = state.notificationSuccessAutoClearSeconds,
                            onValueChange = { seconds ->
                                viewModel.dispatch(PreferredViewAction.ChangeNotificationSuccessAutoClearSeconds(seconds))
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
                                checked = state.versionCompareInSingleLine,
                                onCheckedChange = { viewModel.dispatch(PreferredViewAction.ChangeVersionCompareInSingleLine(it)) }
                            )
                        }

                        // 2. SDK Compare (Dialog Mode)
                        item(visible = isDialogMode) {
                            SwitchWidget(
                                icon = AppIcons.MultiLineSettingIcon,
                                title = stringResource(id = R.string.sdk_compare_in_multi_line),
                                description = stringResource(id = R.string.sdk_compare_in_multi_line_desc),
                                checked = state.sdkCompareInMultiLine,
                                onCheckedChange = { viewModel.dispatch(PreferredViewAction.ChangeSdkCompareInMultiLine(it)) }
                            )
                        }

                        // 3. Extended Menu (Strictly Dialog Mode)
                        item(visible = state.installMode == ConfigEntity.InstallMode.Dialog) {
                            SwitchWidget(
                                icon = AppIcons.MenuOpen,
                                title = stringResource(id = R.string.show_dialog_install_extended_menu),
                                description = stringResource(id = R.string.show_dialog_install_extended_menu_desc),
                                checked = state.showDialogInstallExtendedMenu,
                                onCheckedChange = { viewModel.dispatch(PreferredViewAction.ChangeShowDialogInstallExtendedMenu(it)) }
                            )
                        }

                        // 4. Smart Suggestion (Dialog Mode)
                        item(visible = isDialogMode) {
                            SwitchWidget(
                                icon = AppIcons.Suggestion,
                                title = stringResource(id = R.string.show_intelligent_suggestion),
                                description = stringResource(id = R.string.show_intelligent_suggestion_desc),
                                checked = state.showSmartSuggestion,
                                onCheckedChange = { viewModel.dispatch(PreferredViewAction.ChangeShowSuggestion(it)) }
                            )
                        }

                        // 5. Show Dialog on Touch (Notification Mode)
                        item(visible = isNotificationMode) {
                            SwitchWidget(
                                icon = AppIcons.Dialog,
                                title = stringResource(id = R.string.show_dialog_when_pressing_notification),
                                description = stringResource(id = R.string.change_notification_touch_behavior),
                                checked = state.showDialogWhenPressingNotification,
                                onCheckedChange = {
                                    viewModel.dispatch(
                                        PreferredViewAction.ChangeShowDialogWhenPressingNotification(
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
                                checked = state.autoSilentInstall,
                                onCheckedChange = { viewModel.dispatch(PreferredViewAction.ChangeAutoSilentInstall(it)) }
                            )
                        }

                        // 7. Disable Notification (Dialog Mode)
                        item(visible = isDialogMode) {
                            SwitchWidget(
                                icon = AppIcons.NotificationDisabled,
                                title = stringResource(id = R.string.disable_notification),
                                description = stringResource(id = R.string.close_immediately_on_dialog_dismiss),
                                checked = state.disableNotificationForDialogInstall,
                                onCheckedChange = { viewModel.dispatch(PreferredViewAction.ChangeShowDisableNotification(it)) }
                            )
                        }

                        // 8. Disable Notification (Notification Mode + Show Dialog)
                        item(visible = isNotificationMode && state.showDialogWhenPressingNotification) {
                            SwitchWidget(
                                icon = AppIcons.NotificationDisabled,
                                title = stringResource(id = R.string.disable_notification_on_dismiss),
                                description = stringResource(id = R.string.close_notification_immediately_on_dialog_dismiss),
                                checked = state.disableNotificationForDialogInstall,
                                onCheckedChange = { viewModel.dispatch(PreferredViewAction.ChangeShowDisableNotification(it)) }
                            )
                        }
                    }
                }
            }

            // --- Group 3: OPPO Related ---
            if (RsConfig.currentManufacturer == Manufacturer.OPPO || RsConfig.currentManufacturer == Manufacturer.ONEPLUS) {
                item {
                    SplicedColumnGroup(
                        title = stringResource(R.string.installer_oppo_related)
                    ) {
                        item {
                            SwitchWidget(
                                icon = AppIcons.OEMSpecial,
                                title = stringResource(id = R.string.installer_show_oem_special),
                                description = stringResource(id = R.string.installer_show_oem_special_desc),
                                checked = state.showOPPOSpecial,
                                onCheckedChange = { viewModel.dispatch(PreferredViewAction.ChangeShowOPPOSpecial(it)) }
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
                            packages = state.managedInstallerPackages,
                            onAddPackage = { viewModel.dispatch(PreferredViewAction.AddManagedInstallerPackage(it)) },
                            onRemovePackage = { viewModel.dispatch(PreferredViewAction.RemoveManagedInstallerPackage(it)) }
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
                            packages = state.managedBlacklistPackages,
                            onAddPackage = { viewModel.dispatch(PreferredViewAction.AddManagedBlacklistPackage(it)) },
                            onRemovePackage = { viewModel.dispatch(PreferredViewAction.RemoveManagedBlacklistPackage(it)) }
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
                            uids = state.managedSharedUserIdBlacklist,
                            onAddUid = { viewModel.dispatch(PreferredViewAction.AddManagedSharedUserIdBlacklist(it)) },
                            onRemoveUid = { viewModel.dispatch(PreferredViewAction.RemoveManagedSharedUserIdBlacklist(it)) }
                        )
                    }

                    // Show exempted packages only if UID blacklist is not empty
                    item(visible = state.managedSharedUserIdBlacklist.isNotEmpty()) {
                        ManagedPackagesWidget(
                            noContentTitle = stringResource(R.string.config_no_managed_shared_user_id_exempted_packages),
                            noContentDescription = stringResource(R.string.config_shared_uid_prior_to_pkgname_desc),
                            packages = state.managedSharedUserIdExemptedPackages,
                            infoText = stringResource(R.string.config_no_managed_shared_user_id_exempted_packages),
                            isInfoVisible = state.managedSharedUserIdExemptedPackages.isNotEmpty(),
                            onAddPackage = { viewModel.dispatch(PreferredViewAction.AddManagedSharedUserIdExemptedPackages(it)) },
                            onRemovePackage = {
                                viewModel.dispatch(
                                    PreferredViewAction.RemoveManagedSharedUserIdExemptedPackages(
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