// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.settings.preferred.installer.dialog

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.itos.coui.layout.Divider
import com.itos.coui.layout.Group
import com.itos.coui.layout.Scaffold
import com.itos.coui.theme.CouiTheme
import com.itos.coui.topappbar.CouiTopAppBarDefaults
import com.itos.coui.topappbar.LargeTopAppBar
import com.rosan.installer.R
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.coui.widgets.CouiBackButton
import com.rosan.installer.ui.page.coui.widgets.CouiSpinnerItem
import com.rosan.installer.ui.page.coui.widgets.CouiSwitchItem
import com.rosan.installer.ui.page.main.settings.preferred.installer.dialog.DialogSettingsAction
import com.rosan.installer.ui.page.main.settings.preferred.installer.dialog.DialogSettingsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun CouiDialogSettingsPage(useBlur: Boolean, viewModel: DialogSettingsViewModel = koinViewModel()) {
    val navigator = LocalNavigator.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scrollBehavior = CouiTopAppBarDefaults.exitUntilCollapsedScrollBehavior {
        listState.canScrollForward || listState.canScrollBackward
    }
    val density = LocalDensity.current

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CouiTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(R.string.dialog_settings)) },
                navigationIcon = {
                    CouiBackButton(onClick = { navigator.pop() })
                },
                scrollBehavior = scrollBehavior,
                useCouiBlur = useBlur,
            )
        },
    ) { insets ->
        val contentPadding = remember(scrollBehavior.state, insets, density) {
            scrollBehavior.state.contentPadding(insets, density)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(insets),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + 4.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
            ),
        ) {
            item {
                Group(title = stringResource(id = R.string.installer_settings_dialog_mode_options)) {
                    val comparisonOptions = listOf(
                        stringResource(R.string.install_comparison_show_all),
                        stringResource(R.string.install_comparison_show_differences_only),
                    )
                    CouiSpinnerItem(
                        title = stringResource(id = R.string.install_comparison_display_behavior),
                        summary = stringResource(
                            id = if (uiState.hideIdenticalComparisons) {
                                R.string.install_comparison_show_differences_only_desc
                            } else {
                                R.string.install_comparison_show_all_desc
                            },
                        ),
                        items = comparisonOptions,
                        selectedIndex = if (uiState.hideIdenticalComparisons) 1 else 0,
                        onSelectedIndexChange = { index ->
                            viewModel.dispatch(DialogSettingsAction.ChangeHideIdenticalComparisons(index == 1))
                        },
                    )
                    Divider()
                    CouiSwitchItem(
                        title = stringResource(id = R.string.show_dialog_install_extended_menu),
                        description = stringResource(id = R.string.show_dialog_install_extended_menu_desc),
                        checked = uiState.showDialogInstallExtendedMenu,
                        onCheckedChange = {
                            viewModel.dispatch(DialogSettingsAction.ChangeShowDialogInstallExtendedMenu(it))
                        },
                    )
                    Divider()
                    CouiSwitchItem(
                        title = stringResource(id = R.string.show_intelligent_suggestion),
                        description = stringResource(id = R.string.show_intelligent_suggestion_desc),
                        checked = uiState.showSmartSuggestion,
                        onCheckedChange = {
                            viewModel.dispatch(DialogSettingsAction.ChangeShowSuggestion(it))
                        },
                    )
                    Divider()
                    CouiSwitchItem(
                        title = stringResource(id = R.string.disable_notification_on_dismiss),
                        description = stringResource(id = R.string.close_notification_immediately_on_dialog_dismiss),
                        checked = uiState.disableNotificationForDialogInstall,
                        onCheckedChange = {
                            viewModel.dispatch(DialogSettingsAction.ChangeShowDisableNotification(it))
                        },
                    )
                }
            }

            item {
                Group(title = stringResource(id = R.string.installer_settings_dialog_automation_options)) {
                    CouiSwitchItem(
                        title = stringResource(id = R.string.auto_background_install),
                        description = stringResource(id = R.string.auto_background_install_desc),
                        checked = uiState.autoSilentInstall,
                        onCheckedChange = {
                            viewModel.dispatch(DialogSettingsAction.ChangeAutoSilentInstall(it))
                        },
                    )
                    Divider()
                    CouiSwitchItem(
                        title = stringResource(id = R.string.long_click_background_install),
                        description = stringResource(id = R.string.long_click_background_install_desc),
                        checked = uiState.longClickBackgroundInstall,
                        onCheckedChange = {
                            viewModel.dispatch(DialogSettingsAction.ChangeLongClickBackgroundInstall(it))
                        },
                    )
                }
            }

            item {
                Group(title = stringResource(R.string.extras)) {
                    CouiSwitchItem(
                        title = stringResource(R.string.lab_tap_icon_to_share),
                        description = stringResource(R.string.lab_tap_icon_to_share_desc),
                        checked = uiState.tapIconToShare,
                        onCheckedChange = { viewModel.dispatch(DialogSettingsAction.ChangeTapIconToShare(it)) },
                    )
                    Divider()
                    CouiSwitchItem(
                        title = stringResource(R.string.lab_show_apk_path),
                        description = stringResource(R.string.lab_show_apk_path_desc),
                        checked = uiState.showFilePath,
                        onCheckedChange = { viewModel.dispatch(DialogSettingsAction.ChangeShowFilePath(it)) },
                    )
                    Divider()
                    CouiSwitchItem(
                        title = stringResource(R.string.lab_show_install_initiator),
                        description = stringResource(R.string.lab_show_install_initiator_desc),
                        checked = uiState.showInstallInitiator,
                        onCheckedChange = { viewModel.dispatch(DialogSettingsAction.ChangeShowInstallInitiator(it)) },
                    )
                }
            }
        }
    }
}
