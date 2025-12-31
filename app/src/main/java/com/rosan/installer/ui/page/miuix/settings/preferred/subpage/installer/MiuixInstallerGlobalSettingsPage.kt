package com.rosan.installer.ui.page.miuix.settings.preferred.subpage.installer

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import com.rosan.installer.ui.page.miuix.widgets.MiuixAutoClearNotificationTimeWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataAuthorizerWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixDataInstallModeWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixIntNumberPickerWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixManagedPackagesWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixManagedUidsWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixSwitchWidget
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun MiuixInstallerGlobalSettingsPage(
    navController: NavController,
    viewModel: PreferredViewModel,
) {
    val state = viewModel.state
    val scrollBehavior = MiuixScrollBehavior()
    val hazeState = remember { HazeState() }
    val hazeStyle = HazeStyle(
        backgroundColor = MiuixTheme.colorScheme.surface,
        tint = HazeTint(MiuixTheme.colorScheme.surface.copy(0.8f))
    )

    val isDialogMode = state.installMode == ConfigEntity.InstallMode.Dialog ||
            state.installMode == ConfigEntity.InstallMode.AutoDialog
    val isNotificationMode = state.installMode == ConfigEntity.InstallMode.Notification ||
            state.installMode == ConfigEntity.InstallMode.AutoNotification

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.hazeEffect(hazeState) {
                    style = hazeStyle
                    blurRadius = 30.dp
                    noiseFactor = 0f
                },
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
                .hazeSource(hazeState)
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(top = paddingValues.calculateTopPadding()),
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
                        currentAuthorizer = state.authorizer,
                        changeAuthorizer = { newAuthorizer ->
                            viewModel.dispatch(PreferredViewAction.ChangeGlobalAuthorizer(newAuthorizer))
                        }
                    ) {
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
                    MiuixDataInstallModeWidget(
                        currentInstallMode = state.installMode,
                        changeInstallMode = { newMode ->
                            viewModel.dispatch(PreferredViewAction.ChangeGlobalInstallMode(newMode))
                        }
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA)
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
                        if (BiometricManager
                                .from(LocalContext.current)
                                .canAuthenticate(BIOMETRIC_WEAK or BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS)
                        {
                            MiuixSwitchWidget(
                                icon = AppIcons.BiometricAuth,
                                title = stringResource(R.string.installer_settings_require_biometric_auth),
                                description = stringResource(R.string.installer_settings_require_biometric_auth_desc),
                                checked = state.installerRequireBiometricAuth,
                                onCheckedChange = {
                                    viewModel.dispatch(PreferredViewAction.ChangeBiometricAuth(it, true))
                                }
                            )
                        }
                        MiuixAutoClearNotificationTimeWidget(
                            currentValue = state.notificationSuccessAutoClearSeconds,
                            onValueChange = { seconds ->
                                viewModel.dispatch(
                                    PreferredViewAction.ChangeNotificationSuccessAutoClearSeconds(
                                        seconds
                                    )
                                )
                            }
                        )
                    }
                }
            }

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

                    AnimatedVisibility(visible = isDialogMode) {
                        MiuixSwitchWidget(
                            icon = AppIcons.Suggestion,
                            title = stringResource(id = R.string.show_intelligent_suggestion),
                            description = stringResource(id = R.string.show_intelligent_suggestion_desc),
                            checked = viewModel.state.showSmartSuggestion,
                            onCheckedChange = {
                                viewModel.dispatch(
                                    PreferredViewAction.ChangeShowSuggestion(it)
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
                        visible = isDialogMode,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        MiuixSwitchWidget(
                            title = stringResource(id = R.string.auto_silent_install),
                            description = stringResource(id = R.string.auto_silent_install_desc),
                            checked = state.autoSilentInstall,
                            onCheckedChange = {
                                viewModel.dispatch(PreferredViewAction.ChangeAutoSilentInstall(it))
                            }
                        )
                    }

                    AnimatedVisibility(
                        visible = isDialogMode,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        MiuixSwitchWidget(
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

            item { SmallTitle(stringResource(R.string.config_managed_installer_packages_title)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    MiuixManagedPackagesWidget(
                        noContentTitle = stringResource(R.string.config_no_preset_install_sources),
                        packages = state.managedInstallerPackages,
                        onAddPackage = { viewModel.dispatch(PreferredViewAction.AddManagedInstallerPackage(it)) },
                        onRemovePackage = {
                            viewModel.dispatch(
                                PreferredViewAction.RemoveManagedInstallerPackage(it)
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
                        packages = state.managedBlacklistPackages,
                        onAddPackage = { viewModel.dispatch(PreferredViewAction.AddManagedBlacklistPackage(it)) },
                        onRemovePackage = {
                            viewModel.dispatch(
                                PreferredViewAction.RemoveManagedBlacklistPackage(it)
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
                        uids = state.managedSharedUserIdBlacklist,
                        onAddUid = {
                            viewModel.dispatch(PreferredViewAction.AddManagedSharedUserIdBlacklist(it))
                        },
                        onRemoveUid = {
                            viewModel.dispatch(PreferredViewAction.RemoveManagedSharedUserIdBlacklist(it))
                        }
                    )
                    AnimatedVisibility(
                        visible = state.managedSharedUserIdBlacklist.isNotEmpty(),
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
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}