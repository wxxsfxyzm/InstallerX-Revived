package com.rosan.installer.ui.page.main.settings.preferred.subpage.installer

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.rosan.installer.R
import com.rosan.installer.build.Manufacturer
import com.rosan.installer.build.RsConfig
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewAction
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.page.main.widget.setting.AppBackButton
import com.rosan.installer.ui.page.main.widget.setting.DataAuthorizerWidget
import com.rosan.installer.ui.page.main.widget.setting.DataInstallModeWidget
import com.rosan.installer.ui.page.main.widget.setting.IntNumberPickerWidget
import com.rosan.installer.ui.page.main.widget.setting.LabelWidget
import com.rosan.installer.ui.page.main.widget.setting.ManagedPackagesWidget
import com.rosan.installer.ui.page.main.widget.setting.ManagedUidsWidget
import com.rosan.installer.ui.page.main.widget.setting.SwitchWidget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegacyInstallerGlobalSettingsPage(
    navController: NavController,
    viewModel: PreferredViewModel,
) {
    val state = viewModel.state
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
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
                    currentAuthorizer = state.authorizer,
                    changeAuthorizer = { newAuthorizer ->
                        viewModel.dispatch(PreferredViewAction.ChangeGlobalAuthorizer(newAuthorizer))
                    },
                    trailingContent = {
                        AnimatedVisibility(
                            visible = state.authorizer == ConfigEntity.Authorizer.Dhizuku,
                            enter = fadeIn() + expandVertically(), // 进入动画：淡入 + 垂直展开
                            exit = fadeOut() + shrinkVertically()  // 退出动画：淡出 + 垂直收起
                        ) {
                            IntNumberPickerWidget(
                                icon = AppIcons.Working,
                                title = stringResource(R.string.set_countdown),
                                description = stringResource(R.string.dhizuku_auto_close_countdown_desc),
                                value = state.dhizukuAutoCloseCountDown,
                                startInt = 1,
                                endInt = 10
                            ) {
                                viewModel.dispatch(
                                    PreferredViewAction.ChangeDhizukuAutoCloseCountDown(it)
                                )
                            }
                        }
                    }
                )
            }
            item {
                DataInstallModeWidget(
                    currentInstallMode = state.installMode,
                    changeInstallMode = {
                        viewModel.dispatch(PreferredViewAction.ChangeGlobalInstallMode(it))
                    }
                )
            }
            item {
                AnimatedVisibility(
                    visible = state.installMode == ConfigEntity.InstallMode.Dialog || state.installMode == ConfigEntity.InstallMode.AutoDialog,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(modifier = Modifier.animateContentSize()) {
                        LabelWidget(label = stringResource(id = R.string.installer_settings_dialog_mode_options))
                        SwitchWidget(
                            icon = AppIcons.MultiLineSettingIcon,
                            title = stringResource(id = R.string.version_compare_in_single_line),
                            description = stringResource(id = R.string.version_compare_in_single_line_desc),
                            checked = state.versionCompareInSingleLine,
                            isM3E = false,
                            onCheckedChange = {
                                viewModel.dispatch(PreferredViewAction.ChangeVersionCompareInSingleLine(it))
                            }
                        )
                        SwitchWidget(
                            icon = AppIcons.SingleLineSettingIcon,
                            title = stringResource(id = R.string.sdk_compare_in_multi_line),
                            description = stringResource(id = R.string.sdk_compare_in_multi_line_desc),
                            checked = state.sdkCompareInMultiLine,
                            isM3E = false,
                            onCheckedChange = {
                                viewModel.dispatch(PreferredViewAction.ChangeSdkCompareInMultiLine(it))
                            }
                        )
                        AnimatedVisibility(
                            visible = state.installMode == ConfigEntity.InstallMode.Dialog,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            SwitchWidget(
                                icon = AppIcons.MenuOpen,
                                title = stringResource(id = R.string.show_dialog_install_extended_menu),
                                description = stringResource(id = R.string.show_dialog_install_extended_menu_desc),
                                checked = viewModel.state.showDialogInstallExtendedMenu,
                                isM3E = false,
                                onCheckedChange = {
                                    viewModel.dispatch(
                                        PreferredViewAction.ChangeShowDialogInstallExtendedMenu(it)
                                    )
                                }
                            )
                        }
                        SwitchWidget(
                            icon = AppIcons.Suggestion,
                            title = stringResource(id = R.string.show_intelligent_suggestion),
                            description = stringResource(id = R.string.show_intelligent_suggestion_desc),
                            checked = viewModel.state.showSmartSuggestion,
                            isM3E = false,
                            onCheckedChange = {
                                viewModel.dispatch(
                                    PreferredViewAction.ChangeShowSuggestion(it)
                                )
                            }
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA)
                            SwitchWidget(
                                icon = AppIcons.LiveActivity,
                                title = stringResource(R.string.theme_settings_use_live_activity),
                                description = stringResource(R.string.theme_settings_use_live_activity_desc),
                                checked = state.showLiveActivity,
                                isM3E = false,
                                onCheckedChange = {
                                    viewModel.dispatch(
                                        PreferredViewAction.ChangeShowLiveActivity(it)
                                    )
                                }
                            )
                        SwitchWidget(
                            icon = AppIcons.NotificationDisabled,
                            title = stringResource(id = R.string.disable_notification),
                            description = stringResource(id = R.string.close_immediately_on_dialog_dismiss),
                            checked = viewModel.state.disableNotificationForDialogInstall,
                            isM3E = false,
                            onCheckedChange = {
                                viewModel.dispatch(
                                    PreferredViewAction.ChangeShowDisableNotification(it)
                                )
                            }
                        )
                    }
                }
            }
            item {
                AnimatedVisibility(
                    visible = state.installMode == ConfigEntity.InstallMode.Notification ||
                            state.installMode == ConfigEntity.InstallMode.AutoNotification,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(modifier = Modifier.animateContentSize()) {
                        LabelWidget(label = stringResource(id = R.string.installer_settings_notification_mode_options))
                        SwitchWidget(
                            icon = AppIcons.Dialog,
                            title = stringResource(id = R.string.show_dialog_when_pressing_notification),
                            description = stringResource(id = R.string.change_notification_touch_behavior),
                            checked = viewModel.state.showDialogWhenPressingNotification,
                            isM3E = false,
                            onCheckedChange = {
                                viewModel.dispatch(
                                    PreferredViewAction.ChangeShowDialogWhenPressingNotification(it)
                                )
                            }
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA)
                            SwitchWidget(
                                icon = AppIcons.LiveActivity,
                                title = stringResource(R.string.theme_settings_use_live_activity),
                                description = stringResource(R.string.theme_settings_use_live_activity_desc),
                                checked = state.showLiveActivity,
                                isM3E = false,
                                onCheckedChange = {
                                    viewModel.dispatch(
                                        PreferredViewAction.ChangeShowLiveActivity(it)
                                    )
                                }
                            )
                        AnimatedVisibility(
                            visible = viewModel.state.showDialogWhenPressingNotification,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            SwitchWidget(
                                icon = AppIcons.NotificationDisabled,
                                title = stringResource(id = R.string.disable_notification_on_dismiss),
                                description = stringResource(id = R.string.close_notification_immediately_on_dialog_dismiss),
                                checked = viewModel.state.disableNotificationForDialogInstall,
                                isM3E = false,
                                onCheckedChange = {
                                    viewModel.dispatch(
                                        PreferredViewAction.ChangeShowDisableNotification(it)
                                    )
                                }
                            )
                        }
                    }
                }
            }
            if (RsConfig.currentManufacturer == Manufacturer.OPPO || RsConfig.currentManufacturer == Manufacturer.ONEPLUS) {
                item { LabelWidget(stringResource(R.string.installer_oppo_related)) }
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
            item { LabelWidget(label = stringResource(id = R.string.config_managed_installer_packages_title)) }
            item {
                ManagedPackagesWidget(
                    noContentTitle = stringResource(R.string.config_no_preset_install_sources),
                    packages = state.managedInstallerPackages,
                    onAddPackage = { viewModel.dispatch(PreferredViewAction.AddManagedInstallerPackage(it)) },
                    onRemovePackage = {
                        viewModel.dispatch(
                            PreferredViewAction.RemoveManagedInstallerPackage(it)
                        )
                    })
            }
            item { LabelWidget(label = stringResource(id = R.string.config_managed_blacklist_by_package_name_title)) }
            item {
                ManagedPackagesWidget(
                    noContentTitle = stringResource(R.string.config_no_managed_blacklist),
                    packages = state.managedBlacklistPackages,
                    onAddPackage = { viewModel.dispatch(PreferredViewAction.AddManagedBlacklistPackage(it)) },
                    onRemovePackage = {
                        viewModel.dispatch(
                            PreferredViewAction.RemoveManagedBlacklistPackage(it)
                        )
                    })
            }
            item { LabelWidget(label = stringResource(id = R.string.config_managed_blacklist_by_shared_user_id_title)) }
            item {
                ManagedUidsWidget(
                    noContentTitle = stringResource(R.string.config_no_managed_shared_user_id_blacklist),
                    uids = state.managedSharedUserIdBlacklist,
                    onAddUid = {
                        viewModel.dispatch(PreferredViewAction.AddManagedSharedUserIdBlacklist(it))
                    },
                    onRemoveUid = {
                        viewModel.dispatch(PreferredViewAction.RemoveManagedSharedUserIdBlacklist(it))
                    }
                )
                AnimatedVisibility(
                    // Only show exempted packages if there are any blacklisted UIDs
                    visible = state.managedSharedUserIdBlacklist.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    ManagedPackagesWidget(
                        noContentTitle = stringResource(R.string.config_no_managed_shared_user_id_exempted_packages),
                        noContentDescription = stringResource(R.string.config_shared_uid_prior_to_pkgname_desc),
                        packages = state.managedSharedUserIdExemptedPackages,
                        infoText = stringResource(R.string.config_no_managed_shared_user_id_exempted_packages),
                        isInfoVisible = state.managedSharedUserIdExemptedPackages.isNotEmpty(),
                        onAddPackage = {
                            viewModel.dispatch(
                                PreferredViewAction.AddManagedSharedUserIdExemptedPackages(
                                    it
                                )
                            )
                        },
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
    }
}