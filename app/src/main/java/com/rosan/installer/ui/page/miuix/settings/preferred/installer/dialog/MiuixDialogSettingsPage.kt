// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.settings.preferred.installer.dialog

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.main.settings.preferred.installer.dialog.DialogSettingsAction
import com.rosan.installer.ui.page.main.settings.preferred.installer.dialog.DialogSettingsViewModel
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import com.rosan.installer.ui.page.miuix.widgets.MiuixSwitchWidget
import com.rosan.installer.ui.theme.getMiuixAppBarColor
import com.rosan.installer.ui.theme.installerMiuixBlurEffect
import com.rosan.installer.ui.theme.rememberMiuixBlurBackdrop
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun MiuixDialogSettingsPage(
    useBlur: Boolean,
    viewModel: DialogSettingsViewModel = koinViewModel()
) {
    val navigator = LocalNavigator.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = MiuixScrollBehavior()

    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    val topBarBackdrop = rememberMiuixBlurBackdrop(useBlur)

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.installerMiuixBlurEffect(topBarBackdrop),
                color = topBarBackdrop.getMiuixAppBarColor(),
                title = stringResource(R.string.dialog_settings),
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
            item { SmallTitle(stringResource(id = R.string.installer_settings_dialog_mode_options)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    val comparisonOptions = listOf(
                        stringResource(R.string.install_comparison_show_all),
                        stringResource(R.string.install_comparison_show_differences_only)
                    )
                    WindowSpinnerPreference(
                        title = stringResource(id = R.string.install_comparison_display_behavior),
                        summary = stringResource(
                            id = if (uiState.hideIdenticalComparisons) {
                                R.string.install_comparison_show_differences_only_desc
                            } else {
                                R.string.install_comparison_show_all_desc
                            }
                        ),
                        items = comparisonOptions.map { DropdownItem(title = it) },
                        selectedIndex = if (uiState.hideIdenticalComparisons) 1 else 0,
                        onSelectedIndexChange = { index ->
                            viewModel.dispatch(
                                DialogSettingsAction.ChangeHideIdenticalComparisons(index == 1)
                            )
                        }
                    )

                    /* MiuixSwitchWidget(
                         title = stringResource(id = R.string.version_compare_in_single_line),
                         description = stringResource(id = R.string.version_compare_in_single_line_desc),
                         checked = uiState.versionCompareInSingleLine,
                         onCheckedChange = {
                             viewModel.dispatch(DialogSettingsAction.ChangeVersionCompareInSingleLine(it))
                         }
                     )

                     MiuixSwitchWidget(
                         title = stringResource(id = R.string.sdk_compare_in_multi_line),
                         description = stringResource(id = R.string.sdk_compare_in_multi_line_desc),
                         checked = uiState.sdkCompareInMultiLine,
                         onCheckedChange = {
                             viewModel.dispatch(DialogSettingsAction.ChangeSdkCompareInMultiLine(it))
                         }
                     )*/

                    MiuixSwitchWidget(
                        title = stringResource(id = R.string.show_dialog_install_extended_menu),
                        description = stringResource(id = R.string.show_dialog_install_extended_menu_desc),
                        checked = uiState.showDialogInstallExtendedMenu,
                        onCheckedChange = {
                            viewModel.dispatch(DialogSettingsAction.ChangeShowDialogInstallExtendedMenu(it))
                        }
                    )

                    MiuixSwitchWidget(
                        title = stringResource(id = R.string.show_intelligent_suggestion),
                        description = stringResource(id = R.string.show_intelligent_suggestion_desc),
                        checked = uiState.showSmartSuggestion,
                        onCheckedChange = {
                            viewModel.dispatch(DialogSettingsAction.ChangeShowSuggestion(it))
                        }
                    )

                    MiuixSwitchWidget(
                        title = stringResource(id = R.string.disable_notification_on_dismiss),
                        description = stringResource(id = R.string.close_notification_immediately_on_dialog_dismiss),
                        checked = uiState.disableNotificationForDialogInstall,
                        onCheckedChange = {
                            viewModel.dispatch(DialogSettingsAction.ChangeShowDisableNotification(it))
                        }
                    )
                }
            }

            item { SmallTitle(stringResource(id = R.string.installer_settings_dialog_automation_options)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    MiuixSwitchWidget(
                        title = stringResource(id = R.string.auto_background_install),
                        description = stringResource(id = R.string.auto_background_install_desc),
                        checked = uiState.autoSilentInstall,
                        onCheckedChange = {
                            viewModel.dispatch(DialogSettingsAction.ChangeAutoSilentInstall(it))
                        }
                    )

                    MiuixSwitchWidget(
                        title = stringResource(id = R.string.long_click_background_install),
                        description = stringResource(id = R.string.long_click_background_install_desc),
                        checked = uiState.longClickBackgroundInstall,
                        onCheckedChange = {
                            viewModel.dispatch(DialogSettingsAction.ChangeLongClickBackgroundInstall(it))
                        }
                    )
                }
            }

            item { SmallTitle(stringResource(R.string.extras)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    MiuixSwitchWidget(
                        title = stringResource(R.string.lab_tap_icon_to_share),
                        description = stringResource(R.string.lab_tap_icon_to_share_desc),
                        checked = uiState.tapIconToShare,
                        onCheckedChange = { viewModel.dispatch(DialogSettingsAction.ChangeTapIconToShare(it)) }
                    )
                    MiuixSwitchWidget(
                        title = stringResource(R.string.lab_show_apk_path),
                        description = stringResource(R.string.lab_show_apk_path_desc),
                        checked = uiState.showFilePath,
                        onCheckedChange = { viewModel.dispatch(DialogSettingsAction.ChangeShowFilePath(it)) }
                    )
                    MiuixSwitchWidget(
                        title = stringResource(R.string.lab_show_install_initiator),
                        description = stringResource(R.string.lab_show_install_initiator_desc),
                        checked = uiState.showInstallInitiator,
                        onCheckedChange = { viewModel.dispatch(DialogSettingsAction.ChangeShowInstallInitiator(it)) }
                    )
                }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}
