// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.settings.preferred.subpage.lab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.rosan.installer.R
import com.rosan.installer.core.env.AppConfig
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.settings.model.HttpProfile
import com.rosan.installer.domain.settings.model.RootImplementation
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.preferred.subpage.lab.LabSettingsAction
import com.rosan.installer.ui.page.main.settings.preferred.subpage.lab.LabSettingsViewModel
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import com.rosan.installer.ui.page.miuix.widgets.MiuixRootImplementationDialog
import com.rosan.installer.ui.page.miuix.widgets.MiuixSettingsTipCard
import com.rosan.installer.ui.page.miuix.widgets.MiuixSwitchWidget
import com.rosan.installer.ui.theme.getMiuixAppBarColor
import com.rosan.installer.ui.theme.installerHazeEffect
import com.rosan.installer.ui.theme.rememberMiuixHazeStyle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SpinnerEntry
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.extra.SuperSpinner
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun MiuixLabPage(
    navController: NavHostController,
    viewModel: LabSettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val capabilityProvider = koinInject<DeviceCapabilityProvider>()
    val scrollBehavior = MiuixScrollBehavior()
    val hazeState = if (uiState.useBlur) remember { HazeState() } else null
    val hazeStyle = rememberMiuixHazeStyle()
    val showRootImplementationDialog = remember { mutableStateOf(false) }
    val isMiIslandSupported = remember { capabilityProvider.isSupportMiIsland }

    MiuixRootImplementationDialog(
        showState = showRootImplementationDialog,
        onDismiss = { showRootImplementationDialog.value = false },
        onConfirm = { selectedImplementation ->
            // When the user confirms, dismiss the dialog.
            showRootImplementationDialog.value = false
            // Dispatch actions to update the root implementation AND enable the flashing feature.
            viewModel.dispatch(LabSettingsAction.LabChangeRootImplementation(selectedImplementation))
            viewModel.dispatch(LabSettingsAction.LabChangeRootModuleFlash(true))
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.installerHazeEffect(hazeState, hazeStyle),
                color = hazeState.getMiuixAppBarColor(),
                title = stringResource(R.string.lab),
                navigationIcon = {
                    MiuixBackButton(modifier = Modifier.padding(start = 16.dp), onClick = { navController.navigateUp() })
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Crossfade(
            targetState = uiState.isLoading,
            label = "MiuixLabPageContent",
            animationSpec = tween(durationMillis = 150)
        ) { isLoading ->
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(hazeState?.let { Modifier.hazeSource(it) } ?: Modifier)
                        .scrollEndHaptic()
                        .overScrollVertical()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = PaddingValues(top = paddingValues.calculateTopPadding()),
                    overscrollEffect = null
                ) {
                    item { MiuixSettingsTipCard(stringResource(R.string.lab_tip)) }
                    item { Spacer(modifier = Modifier.size(12.dp)) }
                    item { SmallTitle(stringResource(R.string.config_authorizer_root)) }
                    item {
                        Card(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp)
                        ) {
                            MiuixSwitchWidget(
                                title = stringResource(R.string.lab_module_flashing),
                                description = stringResource(R.string.lab_module_flashing_desc),
                                checked = uiState.labRootEnableModuleFlash,
                                onCheckedChange = { isEnabling ->
                                    if (isEnabling) {
                                        showRootImplementationDialog.value = true
                                    } else {
                                        viewModel.dispatch(LabSettingsAction.LabChangeRootModuleFlash(false))
                                    }
                                }
                            )
                            AnimatedVisibility(
                                visible = uiState.labRootEnableModuleFlash,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                val currentRootImpl = uiState.labRootImplementation
                                val data = remember {
                                    mapOf(
                                        RootImplementation.Magisk to "Magisk",
                                        RootImplementation.KernelSU to "KernelSU",
                                        RootImplementation.APatch to "APatch"
                                    )
                                }

                                val spinnerEntries = remember(data) {
                                    data.values.map { modeName ->
                                        SpinnerEntry(title = modeName)
                                    }
                                }

                                val selectedIndex = remember(currentRootImpl, data) {
                                    data.keys.toList().indexOf(currentRootImpl).coerceAtLeast(0)
                                }

                                Column {
                                    SuperSpinner(
                                        title = stringResource(R.string.lab_module_select_root_impl),
                                        items = spinnerEntries,
                                        selectedIndex = selectedIndex,
                                        onSelectedIndexChange = { newIndex ->
                                            data.keys.elementAtOrNull(newIndex)?.let { impl ->
                                                viewModel.dispatch(LabSettingsAction.LabChangeRootImplementation(impl))
                                            }
                                        }
                                    )
                                    MiuixSwitchWidget(
                                        title = stringResource(R.string.lab_module_flashing_show_art),
                                        description = stringResource(R.string.lab_module_flashing_show_art_desc),
                                        checked = uiState.labRootShowModuleArt,
                                        onCheckedChange = { viewModel.dispatch(LabSettingsAction.LabChangeRootShowModuleArt(it)) }
                                    )
                                    if (capabilityProvider.isSystemApp)
                                        MiuixSwitchWidget(
                                            title = stringResource(R.string.lab_module_always_use_root),
                                            description = stringResource(R.string.lab_module_always_use_root_desc),
                                            checked = uiState.labRootModuleAlwaysUseRoot,
                                            onCheckedChange = { viewModel.dispatch(LabSettingsAction.LabChangeRootModuleAlwaysUseRoot(it)) }
                                        )
                                }
                            }
                        }
                    }
                    item { SmallTitle(stringResource(R.string.lab_unstable_features)) }
                    item {
                        Card(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp)
                        ) {
                            if (isMiIslandSupported)
                                MiuixSwitchWidget(
                                    title = stringResource(R.string.lab_mi_island),
                                    description = stringResource(R.string.lab_mi_island_desc),
                                    checked = uiState.labUseMiIsland,
                                    onCheckedChange = { viewModel.dispatch(LabSettingsAction.LabChangeUseMiIsland(it)) }
                                )
                            MiuixSwitchWidget(
                                icon = AppIcons.Share,
                                title = stringResource(R.string.lab_tap_icon_to_share),
                                description = stringResource(R.string.lab_tap_icon_to_share_desc),
                                checked = uiState.labTapIconToShare,
                                onCheckedChange = { viewModel.dispatch(LabSettingsAction.LabChangeTapIconToShare(it)) }
                            )
                            MiuixSwitchWidget(
                                title = stringResource(R.string.lab_set_install_requester),
                                description = stringResource(R.string.lab_set_install_requester_desc),
                                checked = uiState.labSetInstallRequester,
                                onCheckedChange = { viewModel.dispatch(LabSettingsAction.LabChangeSetInstallRequester(it)) }
                            )
                        }
                    }
                    if (AppConfig.isInternetAccessEnabled) {
                        item { SmallTitle(stringResource(R.string.internet_access_enabled)) }
                        item {
                            Card(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .padding(bottom = 12.dp)
                            ) {
                                // TODO
                                /*MiuixSwitchWidget(
                                    title = stringResource(R.string.lab_http_save_file),
                                    description = stringResource(R.string.lab_http_save_file_desc),
                                    checked = uiState.labHttpSaveFile,
                                    onCheckedChange = { viewModel.dispatch(LabSettingsAction.LabChangeHttpSaveFile(it)) }
                                )*/
                                val currentProfile = uiState.labHttpProfile
                                val allowSecureString = stringResource(R.string.lab_http_profile_secure)
                                val allowLocalString = stringResource(R.string.lab_http_profile_local)
                                val allowAllString = stringResource(R.string.lab_http_profile_all)
                                val profileData = remember {
                                    mapOf(
                                        HttpProfile.ALLOW_SECURE to allowSecureString,
                                        HttpProfile.ALLOW_LOCAL to allowLocalString,
                                        HttpProfile.ALLOW_ALL to allowAllString
                                    )
                                }

                                val profileEntries = remember(profileData) {
                                    profileData.values.map { name ->
                                        SpinnerEntry(title = name)
                                    }
                                }

                                val profileIndex = remember(currentProfile, profileData) {
                                    profileData.keys.toList().indexOf(currentProfile).coerceAtLeast(0)
                                }

                                SuperSpinner(
                                    title = stringResource(R.string.lab_http_profile),
                                    items = profileEntries,
                                    selectedIndex = profileIndex,
                                    onSelectedIndexChange = { newIndex ->
                                        profileData.keys.elementAtOrNull(newIndex)?.let { profile ->
                                            viewModel.dispatch(LabSettingsAction.LabChangeHttpProfile(profile))
                                        }
                                    }
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.navigationBarsPadding()) }
                }
            }
        }
    }
}