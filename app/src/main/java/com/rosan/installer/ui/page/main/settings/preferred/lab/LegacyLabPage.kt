// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.lab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.core.env.AppConfig
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.main.settings.preferred.LabHttpProfileWidget
import com.rosan.installer.ui.page.main.settings.preferred.LabRootImplementationWidget
import com.rosan.installer.ui.page.main.widget.card.InfoTipCard
import com.rosan.installer.ui.page.main.widget.dialog.RootImplementationSelectionDialog
import com.rosan.installer.ui.page.main.widget.setting.AppBackButton
import com.rosan.installer.ui.page.main.widget.setting.LabelWidget
import com.rosan.installer.ui.page.main.widget.setting.SwitchWidget
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegacyLabPage(
    viewModel: LabSettingsViewModel = koinViewModel()
) {
    val navigator = LocalNavigator.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val showRootImplementationDialog = remember { mutableStateOf(false) }

    if (showRootImplementationDialog.value) {
        RootImplementationSelectionDialog(
            currentSelection = uiState.labRootMode,
            onDismiss = { showRootImplementationDialog.value = false },
            onConfirm = { selectedImplementation ->
                showRootImplementationDialog.value = false
                // 1. Save the selected implementation
                viewModel.dispatch(LabSettingsAction.LabChangeRootImplementation(selectedImplementation))
                // 2. Enable the flash module feature
                viewModel.dispatch(LabSettingsAction.LabChangeRootModuleFlash(true))
            }
        )
    }

    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.installer_settings)) },
                navigationIcon = {
                    AppBackButton(onClick = { navigator.pop() })
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = horizontalSafeInsets.calculateStartPadding(layoutDirection),
                top = paddingValues.calculateTopPadding(),
                end = horizontalSafeInsets.calculateEndPadding(layoutDirection),
                bottom = paddingValues.calculateBottomPadding()
            )
        ) {
            item { InfoTipCard(text = stringResource(R.string.lab_tip)) }
            // --- Root Section (Module Flashing) ---
            item { LabelWidget(stringResource(R.string.config_authorizer_root)) }
            item {
                SwitchWidget(
                    icon = AppIcons.Root,
                    title = stringResource(R.string.lab_module_flashing),
                    description = stringResource(R.string.lab_module_flashing_desc),
                    checked = uiState.labRootEnableModuleFlash,
                    isM3E = false,
                    onCheckedChange = { isChecking ->
                        if (isChecking) {
                            // If turning ON, show the dialog first (don't enable yet)
                            showRootImplementationDialog.value = true
                        } else {
                            // If turning OFF, disable immediately
                            viewModel.dispatch(LabSettingsAction.LabChangeRootModuleFlash(false))
                        }
                    }
                )
            }
            // DropDownMenuWidget appears when enabled to allow changing the setting later
            item {
                AnimatedVisibility(
                    visible = uiState.labRootEnableModuleFlash,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        LabRootImplementationWidget(viewModel)
                        SwitchWidget(
                            icon = AppIcons.Terminal,
                            title = stringResource(R.string.lab_module_flashing_show_art),
                            description = stringResource(R.string.lab_module_flashing_show_art_desc),
                            isM3E = false,
                            checked = uiState.labRootShowModuleArt,
                            onCheckedChange = { viewModel.dispatch(LabSettingsAction.LabChangeRootShowModuleArt(it)) }
                        )
                    }
                }
            }
            // --- Unstable Features Section ---
            item { LabelWidget(stringResource(R.string.lab_unstable_features)) }
            item {
                SwitchWidget(
                    icon = AppIcons.Share,
                    title = stringResource(R.string.lab_tap_icon_to_share),
                    description = stringResource(R.string.lab_tap_icon_to_share_desc),
                    checked = uiState.labTapIconToShare,
                    isM3E = false,
                    onCheckedChange = { viewModel.dispatch(LabSettingsAction.LabChangeTapIconToShare(it)) }
                )
            }
            item {
                SwitchWidget(
                    icon = AppIcons.InstallRequester,
                    title = stringResource(R.string.lab_set_install_requester),
                    description = stringResource(R.string.lab_set_install_requester_desc),
                    checked = uiState.labSetInstallRequester,
                    isM3E = false,
                    onCheckedChange = { viewModel.dispatch(LabSettingsAction.LabChangeSetInstallRequester(it)) }
                )
            }
            // --- Internet Access Section ---
            if (AppConfig.isInternetAccessEnabled) {
                item { LabelWidget(stringResource(R.string.internet_access_enabled)) }
                // TODO
                /*item {
                    SwitchWidget(
                        icon = Icons.Default.Download,
                        title = stringResource(R.string.lab_http_save_file),
                        description = stringResource(R.string.lab_http_save_file_desc),
                        checked = uiState.labHttpSaveFile,
                        isM3E = false,
                        onCheckedChange = { viewModel.dispatch(LabSettingsAction.LabChangeHttpSaveFile(it)) }
                    )
                }*/

                // HTTP Profile DropDown
                item { LabHttpProfileWidget(viewModel) }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}
