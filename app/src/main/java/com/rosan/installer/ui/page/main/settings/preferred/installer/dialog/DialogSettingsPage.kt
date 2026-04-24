// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.installer.dialog

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.main.widget.setting.AppBackButton
import com.rosan.installer.ui.page.main.widget.setting.SegmentedColumn
import com.rosan.installer.ui.page.main.widget.setting.SwitchWidget
import com.rosan.installer.ui.theme.getMaterial3AppBarColor
import com.rosan.installer.ui.theme.installerMaterial3BlurEffect
import com.rosan.installer.ui.theme.rememberMaterial3BlurBackdrop
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.blur.layerBackdrop

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DialogSettingsPage(
    useBlur: Boolean,
    viewModel: DialogSettingsViewModel = koinViewModel()
) {
    val navigator = LocalNavigator.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

    // Expand the top app bar by default
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
                    Text(stringResource(R.string.dialog_settings))
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
            item {
                SegmentedColumn(
                    title = stringResource(R.string.installer_settings_dialog_mode_options)
                ) {
                    // 1. Version Compare
                    item {
                        SwitchWidget(
                            icon = AppIcons.SingleLineSettingIcon,
                            title = stringResource(id = R.string.version_compare_in_single_line),
                            description = stringResource(id = R.string.version_compare_in_single_line_desc),
                            checked = uiState.versionCompareInSingleLine,
                            onCheckedChange = {
                                viewModel.dispatch(DialogSettingsAction.ChangeVersionCompareInSingleLine(it))
                            }
                        )
                    }

                    // 2. SDK Compare
                    item {
                        SwitchWidget(
                            icon = AppIcons.MultiLineSettingIcon,
                            title = stringResource(id = R.string.sdk_compare_in_multi_line),
                            description = stringResource(id = R.string.sdk_compare_in_multi_line_desc),
                            checked = uiState.sdkCompareInMultiLine,
                            onCheckedChange = {
                                viewModel.dispatch(DialogSettingsAction.ChangeSdkCompareInMultiLine(it))
                            }
                        )
                    }

                    // 3. Extended Menu
                    item {
                        SwitchWidget(
                            icon = AppIcons.MenuOpen,
                            title = stringResource(id = R.string.show_dialog_install_extended_menu),
                            description = stringResource(id = R.string.show_dialog_install_extended_menu_desc),
                            checked = uiState.showDialogInstallExtendedMenu,
                            onCheckedChange = {
                                viewModel.dispatch(DialogSettingsAction.ChangeShowDialogInstallExtendedMenu(it))
                            }
                        )
                    }

                    // 4. Smart Suggestion
                    item {
                        SwitchWidget(
                            icon = AppIcons.Suggestion,
                            title = stringResource(id = R.string.show_intelligent_suggestion),
                            description = stringResource(id = R.string.show_intelligent_suggestion_desc),
                            checked = uiState.showSmartSuggestion,
                            onCheckedChange = {
                                viewModel.dispatch(DialogSettingsAction.ChangeShowSuggestion(it))
                            }
                        )
                    }

                    // 5. Auto Silent Install
                    item {
                        SwitchWidget(
                            icon = AppIcons.Silent,
                            title = stringResource(id = R.string.auto_silent_install),
                            description = stringResource(id = R.string.auto_silent_install_desc),
                            checked = uiState.autoSilentInstall,
                            onCheckedChange = {
                                viewModel.dispatch(DialogSettingsAction.ChangeAutoSilentInstall(it))
                            }
                        )
                    }

                    // 6. Disable Notification
                    item {
                        SwitchWidget(
                            icon = AppIcons.NotificationDisabled,
                            title = stringResource(id = R.string.disable_notification_on_dismiss),
                            description = stringResource(id = R.string.close_notification_immediately_on_dialog_dismiss),
                            checked = uiState.disableNotificationForDialogInstall,
                            onCheckedChange = {
                                viewModel.dispatch(DialogSettingsAction.ChangeShowDisableNotification(it))
                            }
                        )
                    }
                }
            }

            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}
