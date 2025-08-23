package com.rosan.installer.ui.page.settings.preferred.subpage.installer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.WindowInsets
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
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.settings.preferred.PreferredViewAction
import com.rosan.installer.ui.page.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.theme.none
import com.rosan.installer.ui.widget.setting.AppBackButton
import com.rosan.installer.ui.widget.setting.DataAuthorizerWidget
import com.rosan.installer.ui.widget.setting.DataInstallModeWidget
import com.rosan.installer.ui.widget.setting.IntNumberPickerWidget
import com.rosan.installer.ui.widget.setting.ManagedPackagesWidget
import com.rosan.installer.ui.widget.setting.SplicedColumnGroup
import com.rosan.installer.ui.widget.setting.SwitchWidget


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewInstallerGlobalSettingsPage(
    navController: NavController,
    viewModel: PreferredViewModel
) {
    val state = viewModel.state
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        contentWindowInsets = WindowInsets.none,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.installer_settings)) },
                navigationIcon = { AppBackButton(onClick = { navController.navigateUp() }) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                SplicedColumnGroup(
                    title = stringResource(R.string.installer_settings_global_installer),
                    content = listOf(
                        {
                            DataAuthorizerWidget(
                                currentAuthorizer = state.authorizer,
                                changeAuthorizer = {
                                    viewModel.dispatch(PreferredViewAction.ChangeGlobalAuthorizer(it))
                                },
                                trailingContent = {
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
                                                viewModel.dispatch(
                                                    PreferredViewAction.ChangeDhizukuAutoCloseCountDown(
                                                        it
                                                    )
                                                )
                                            }
                                        )
                                    }
                                }
                            )
                        },
                        {
                            DataInstallModeWidget(
                                currentInstallMode = state.installMode,
                                changeInstallMode = {
                                    viewModel.dispatch(PreferredViewAction.ChangeGlobalInstallMode(it))
                                },
                                onClick = {}
                            )
                        }
                    )
                )
            }
            item {
                AnimatedVisibility(
                    visible = state.installMode == ConfigEntity.InstallMode.Dialog || state.installMode == ConfigEntity.InstallMode.AutoDialog,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    SplicedColumnGroup(
                        title = stringResource(R.string.installer_settings_dialog_mode_options),
                        content = buildList {
                            add {
                                SwitchWidget(
                                    icon = AppIcons.SingleLineSettingIcon,
                                    title = stringResource(id = R.string.version_compare_in_single_line),
                                    description = stringResource(id = R.string.version_compare_in_single_line_desc),
                                    checked = state.versionCompareInSingleLine,
                                    onCheckedChange = {
                                        viewModel.dispatch(PreferredViewAction.ChangeVersionCompareInSingleLine(it))
                                    }
                                )
                            }
                            if (state.installMode == ConfigEntity.InstallMode.Dialog) {
                                add {
                                    SwitchWidget(
                                        icon = AppIcons.MenuOpen,
                                        title = stringResource(id = R.string.show_dialog_install_extended_menu),
                                        description = stringResource(id = R.string.show_dialog_install_extended_menu_desc),
                                        checked = state.showDialogInstallExtendedMenu,
                                        onCheckedChange = {
                                            viewModel.dispatch(
                                                PreferredViewAction.ChangeShowDialogInstallExtendedMenu(it)
                                            )
                                        }
                                    )
                                }
                            }
                            // Add the other SwitchWidgets unconditionally
                            add {
                                SwitchWidget(
                                    icon = AppIcons.Suggestion,
                                    title = stringResource(id = R.string.show_intelligent_suggestion),
                                    description = stringResource(id = R.string.show_intelligent_suggestion_desc),
                                    checked = state.showIntelligentSuggestion,
                                    onCheckedChange = {
                                        viewModel.dispatch(PreferredViewAction.ChangeShowSuggestion(it))
                                    }
                                )
                            }
                            add {
                                SwitchWidget(
                                    icon = AppIcons.NotificationDisabled,
                                    title = stringResource(id = R.string.disable_notification),
                                    description = stringResource(id = R.string.close_immediately_on_dialog_dismiss),
                                    checked = state.disableNotificationForDialogInstall,
                                    onCheckedChange = {
                                        viewModel.dispatch(
                                            PreferredViewAction.ChangeShowDisableNotification(it)
                                        )
                                    }
                                )
                            }
                        }
                    )
                }
            }
            item {
                AnimatedVisibility(
                    visible = state.installMode == ConfigEntity.InstallMode.Notification || state.installMode == ConfigEntity.InstallMode.AutoNotification,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    SplicedColumnGroup(
                        title = stringResource(R.string.installer_settings_notification_mode_options),
                        content = listOf(
                            {
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
                            },
                            {
                                AnimatedVisibility(visible = state.showDialogWhenPressingNotification) {
                                    SwitchWidget(
                                        icon = AppIcons.NotificationDisabled,
                                        title = stringResource(id = R.string.disable_notification_on_dismiss),
                                        description = stringResource(id = R.string.close_notification_immediately_on_dialog_dismiss),
                                        checked = state.disableNotificationForDialogInstall,
                                        onCheckedChange = {
                                            viewModel.dispatch(
                                                PreferredViewAction.ChangeShowDisableNotification(
                                                    it
                                                )
                                            )
                                        }
                                    )
                                }
                            }
                        )
                    )
                }
            }
            item {
                SplicedColumnGroup(
                    title = stringResource(R.string.config_managed_installer_packages_title),
                    content = listOf {
                        ManagedPackagesWidget(
                            noContentTitle = stringResource(R.string.config_no_managed_installer_packages),
                            packages = state.managedInstallerPackages,
                            onAddPackage = {
                                viewModel.dispatch(PreferredViewAction.AddManagedInstallerPackage(it))
                            },
                            onRemovePackage = {
                                viewModel.dispatch(PreferredViewAction.RemoveManagedInstallerPackage(it))
                            }
                        )
                    }
                )
            }
            item {
                SplicedColumnGroup(
                    title = stringResource(id = R.string.config_managed_blacklist_title),
                    content = listOf {
                        ManagedPackagesWidget(
                            noContentTitle = stringResource(R.string.config_no_managed_blacklist),
                            packages = state.managedBlacklistPackages,
                            onAddPackage = {
                                viewModel.dispatch(PreferredViewAction.AddManagedBlacklistPackage(it))
                            },
                            onRemovePackage = {
                                viewModel.dispatch(PreferredViewAction.RemoveManagedBlacklistPackage(it))
                            }
                        )
                    }
                )
            }
        }
    }
}