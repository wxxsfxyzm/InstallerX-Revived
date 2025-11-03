package com.rosan.installer.ui.page.miuix.settings.preferred.subpage.installer

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rosan.installer.R
import com.rosan.installer.build.Manufacturer
import com.rosan.installer.build.RsConfig
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewAction
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataAuthorizerWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataInstallModeWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixIntNumberPickerWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixSwitchWidget
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun MiuixInstallerGlobalSettingsPage(
    navController: NavController,
    viewModel: PreferredViewModel,
) {
    val state = viewModel.state
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = stringResource(R.string.installer_settings),
                navigationIcon = {
                    MiuixBackButton(modifier = Modifier.padding(start = 16.dp), onClick = { navController.navigateUp() })
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .scrollEndHaptic()
                .overScrollVertical()
                .padding(top = paddingValues.calculateTopPadding() + 12.dp),
            overscrollEffect = null
        ) {
            item { SmallTitle(stringResource(R.string.installer_settings_global_installer)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    MiuixDataAuthorizerWidget(
                        currentAuthorizer = state.authorizer,
                        changeAuthorizer = { newAuthorizer ->
                            viewModel.dispatch(PreferredViewAction.ChangeGlobalAuthorizer(newAuthorizer))
                        },
                        trailingContent = {
                            AnimatedVisibility(
                                visible = state.authorizer == ConfigEntity.Authorizer.Dhizuku,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                MiuixIntNumberPickerWidget(
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
                    MiuixDataInstallModeWidget(
                        currentInstallMode = state.installMode,
                        changeInstallMode = { newMode ->
                            viewModel.dispatch(PreferredViewAction.ChangeGlobalInstallMode(newMode))
                        }
                    )
                }
            }

            val isDialogMode = state.installMode == ConfigEntity.InstallMode.Dialog ||
                    state.installMode == ConfigEntity.InstallMode.AutoDialog
            val isNotificationMode = state.installMode == ConfigEntity.InstallMode.Notification ||
                    state.installMode == ConfigEntity.InstallMode.AutoNotification
            item {
                AnimatedContent(
                    targetState = if (isDialogMode) {
                        R.string.installer_settings_dialog_mode_options
                    } else {
                        R.string.installer_settings_notification_mode_options
                    },
                    label = "OptionsTitleAnimation"
                ) { targetTitleRes ->
                    SmallTitle(stringResource(id = targetTitleRes))
                }
            }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    AnimatedVisibility(
                        visible = state.installMode == ConfigEntity.InstallMode.Dialog,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        MiuixSwitchWidget(
                            icon = AppIcons.MenuOpen,
                            title = stringResource(id = R.string.show_dialog_install_extended_menu),
                            description = stringResource(id = R.string.show_dialog_install_extended_menu_desc),
                            checked = viewModel.state.showDialogInstallExtendedMenu,
                            onCheckedChange = {
                                viewModel.dispatch(
                                    PreferredViewAction.ChangeShowDialogInstallExtendedMenu(it)
                                )
                            }
                        )
                    }

                    AnimatedVisibility(
                        visible = isNotificationMode,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        MiuixSwitchWidget(
                            icon = AppIcons.Dialog,
                            title = stringResource(id = R.string.show_dialog_when_pressing_notification),
                            description = stringResource(id = R.string.change_notification_touch_behavior),
                            checked = viewModel.state.showDialogWhenPressingNotification,
                            onCheckedChange = {
                                viewModel.dispatch(
                                    PreferredViewAction.ChangeShowDialogWhenPressingNotification(it)
                                )
                            }
                        )
                    }

                    AnimatedVisibility(
                        visible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        MiuixSwitchWidget(
                            title = stringResource(R.string.theme_settings_use_live_activity),
                            description = stringResource(R.string.theme_settings_use_live_activity_desc),
                            checked = state.showLiveActivity,
                            onCheckedChange = {
                                viewModel.dispatch(
                                    PreferredViewAction.ChangeShowLiveActivity(it)
                                )
                            }
                        )
                    }

                    AnimatedVisibility(
                        visible = isDialogMode,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        MiuixSwitchWidget(
                            icon = AppIcons.NotificationDisabled,
                            title = stringResource(id = R.string.disable_notification),
                            description = stringResource(id = R.string.close_immediately_on_dialog_dismiss),
                            checked = viewModel.state.disableNotificationForDialogInstall,
                            onCheckedChange = {
                                viewModel.dispatch(
                                    PreferredViewAction.ChangeShowDisableNotification(it)
                                )
                            }
                        )
                    }

                    AnimatedVisibility(
                        visible = isNotificationMode && viewModel.state.showDialogWhenPressingNotification,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        MiuixSwitchWidget(
                            icon = AppIcons.NotificationDisabled,
                            title = stringResource(id = R.string.disable_notification_on_dismiss),
                            description = stringResource(id = R.string.close_notification_immediately_on_dialog_dismiss),
                            checked = viewModel.state.disableNotificationForDialogInstall,
                            onCheckedChange = {
                                viewModel.dispatch(
                                    PreferredViewAction.ChangeShowDisableNotification(it)
                                )
                            }
                        )
                    }
                }
            }

            if (RsConfig.currentManufacturer == Manufacturer.OPPO || RsConfig.currentManufacturer == Manufacturer.ONEPLUS) {
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
                            checked = state.showOPPOSpecial,
                            onCheckedChange = { viewModel.dispatch(PreferredViewAction.ChangeShowOPPOSpecial(it)) }
                        )
                    }
                }
            }
        }
    }
}