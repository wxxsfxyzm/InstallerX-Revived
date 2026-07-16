// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.settings.preferred.lab

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.core.env.AppConfig
import com.rosan.installer.domain.device.model.ShizukuMode
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.preferences.GithubUpdateChannel
import com.rosan.installer.domain.settings.model.preferences.HttpProfile
import com.rosan.installer.domain.settings.model.preferences.RootMode
import com.rosan.installer.domain.settings.model.preferences.SmartAuthorizerCandidate
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.main.settings.preferred.lab.LabSettingsAction
import com.rosan.installer.ui.page.main.settings.preferred.lab.LabSettingsViewModel
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import com.rosan.installer.ui.page.miuix.widgets.MiuixCustomGithubProxyUrlDialog
import com.rosan.installer.ui.page.miuix.widgets.MiuixDraggableList
import com.rosan.installer.ui.page.miuix.widgets.MiuixGithubUpdateChannelSelectionDialog
import com.rosan.installer.ui.page.miuix.widgets.MiuixInstallerTipCard
import com.rosan.installer.ui.page.miuix.widgets.MiuixRootImplementationDialog
import com.rosan.installer.ui.page.miuix.widgets.MiuixSettingsTipCard
import com.rosan.installer.ui.page.miuix.widgets.MiuixSwitchWidget
import com.rosan.installer.ui.theme.getMiuixAppBarColor
import com.rosan.installer.ui.theme.installerMiuixBlurEffect
import com.rosan.installer.ui.theme.rememberMiuixBlurBackdrop
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.CheckboxDefaults
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowBottomSheet

@Composable
fun MiuixLabPage(
    useBlur: Boolean,
    viewModel: LabSettingsViewModel = koinViewModel()
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val capabilityProvider = koinInject<DeviceCapabilityProvider>()
    val rootMode by capabilityProvider.rootModeFlow.collectAsStateWithLifecycle()
    val shizukuMode by capabilityProvider.shizukuModeFlow.collectAsStateWithLifecycle()
    val shizukuAuthorized by capabilityProvider.shizukuAuthorizedFlow.collectAsStateWithLifecycle()
    val dhizukuAvailable by capabilityProvider.dhizukuAvailableFlow.collectAsStateWithLifecycle()
    val dhizukuAuthorized by capabilityProvider.dhizukuAuthorizedFlow.collectAsStateWithLifecycle()
    val scrollBehavior = MiuixScrollBehavior()
    val showRootImplementationDialog = remember { mutableStateOf(false) }
    val showChannelDialog = remember { mutableStateOf(false) }
    val showCustomProxyDialog = remember { mutableStateOf(false) }
    val showSmartAuthorizerSheet = remember { mutableStateOf(false) }

    if (showChannelDialog.value)
        MiuixGithubUpdateChannelSelectionDialog(
            showState = showChannelDialog,
            currentSelection = uiState.githubUpdateChannel,
            onDismiss = { showChannelDialog.value = false },
            onConfirm = { channel ->
                showChannelDialog.value = false
                viewModel.dispatch(LabSettingsAction.LabChangeGithubUpdateChannel(channel))
                if (channel == GithubUpdateChannel.CUSTOM)
                    showCustomProxyDialog.value = true
            }
        )

    if (showCustomProxyDialog.value)
        MiuixCustomGithubProxyUrlDialog(
            showState = showCustomProxyDialog,
            initialUrl = uiState.customGithubProxyUrl,
            onDismiss = {
                showCustomProxyDialog.value = false
                if (uiState.customGithubProxyUrl.isEmpty())
                    viewModel.dispatch(LabSettingsAction.LabChangeGithubUpdateChannel(GithubUpdateChannel.OFFICIAL))
            },
            onConfirm = { url ->
                showCustomProxyDialog.value = false
                viewModel.dispatch(LabSettingsAction.LabChangeCustomGithubProxyUrl(url))
                if (url.isEmpty())
                    viewModel.dispatch(LabSettingsAction.LabChangeGithubUpdateChannel(GithubUpdateChannel.OFFICIAL))
            }
        )

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

    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()
    val smartAuthorizerSummary = uiState.smartAuthorizerCandidates
        .filter { it.enabled }
        .joinToString("->") {
            miuixSmartAuthorizerDisplayName(it.authorizer, context::getString)
        }

    val topBarBackdrop = rememberMiuixBlurBackdrop(useBlur)

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.installerMiuixBlurEffect(topBarBackdrop),
                color = topBarBackdrop.getMiuixAppBarColor(),
                title = stringResource(R.string.lab),
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
            item { MiuixSettingsTipCard(stringResource(R.string.lab_tip)) }
            item { Spacer(modifier = Modifier.size(12.dp)) }
            item { SmallTitle(stringResource(R.string.config_authorizer)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    MiuixSwitchWidget(
                        title = stringResource(R.string.config_try_multiple_authorizers_on_install),
                        description = stringResource(R.string.config_try_multiple_authorizers_on_install_desc),
                        checked = uiState.tryMultipleAuthorizersOnInstall,
                        onCheckedChange = {
                            viewModel.dispatch(
                                LabSettingsAction.LabChangeTryMultipleAuthorizersOnInstall(it)
                            )
                        }
                    )
                    AnimatedVisibility(
                        visible = uiState.tryMultipleAuthorizersOnInstall,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        BasicComponent(
                            title = stringResource(R.string.config_smart_authorizer_fallback_list),
                            summary = stringResource(
                                R.string.config_smart_authorizer_fallback_list_desc,
                                smartAuthorizerSummary
                            ),
                            onClick = {
                                capabilityProvider.refreshPrivilegeStatus()
                                showSmartAuthorizerSheet.value = true
                            }
                        )
                    }
                }
            }
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
                        val currentRootImpl = uiState.labRootMode
                        val data = remember {
                            mapOf(
                                RootMode.Magisk to "Magisk",
                                RootMode.KernelSU to "KernelSU",
                                RootMode.APatch to "APatch"
                            )
                        }

                        val spinnerEntries = remember(data) {
                            data.values.map { modeName ->
                                DropdownItem(title = modeName)
                            }
                        }

                        val selectedIndex = remember(currentRootImpl, data) {
                            data.keys.toList().indexOf(currentRootImpl).coerceAtLeast(0)
                        }

                        Column {
                            WindowSpinnerPreference(
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
                        }
                    }
                }
            }
            if (AppConfig.isRespectPlatformInstallPolicyAvailable) {
                // item { SmallTitle(stringResource(R.string.lab_ui_settings)) }
                item { SmallTitle(stringResource(R.string.lab_unstable_features)) }
                item {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        MiuixSwitchWidget(
                            title = stringResource(R.string.lab_respect_platform_install_policy),
                            description = stringResource(R.string.lab_respect_platform_install_policy_desc),
                            checked = uiState.labRespectPlatformInstallPolicy,
                            onCheckedChange = {
                                viewModel.dispatch(
                                    LabSettingsAction.LabChangeRespectPlatformInstallPolicy(it)
                                )
                            }
                        )
                    }
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
                                DropdownItem(title = name)
                            }
                        }

                        val profileIndex = remember(currentProfile, profileData) {
                            profileData.keys.toList().indexOf(currentProfile).coerceAtLeast(0)
                        }

                        WindowSpinnerPreference(
                            title = stringResource(R.string.lab_http_profile),
                            items = profileEntries,
                            selectedIndex = profileIndex,
                            onSelectedIndexChange = { newIndex ->
                                profileData.keys.elementAtOrNull(newIndex)?.let { profile ->
                                    viewModel.dispatch(LabSettingsAction.LabChangeHttpProfile(profile))
                                }
                            }
                        )

                        val currentChannel = uiState.githubUpdateChannel
                        val channelSummary = when (currentChannel) {
                            GithubUpdateChannel.OFFICIAL -> stringResource(R.string.lab_update_github_proxy_official)
                            GithubUpdateChannel.PROXY_7ED -> stringResource(R.string.lab_update_github_proxy_7ed)
                            GithubUpdateChannel.CUSTOM -> uiState.customGithubProxyUrl.ifBlank {
                                stringResource(R.string.lab_update_github_proxy_custom)
                            }
                        }

                        BasicComponent(
                            title = stringResource(R.string.lab_update_github_proxy),
                            summary = channelSummary,
                            onClick = { showChannelDialog.value = true }
                        )
                    }
                }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }

    WindowBottomSheet(
        show = showSmartAuthorizerSheet.value,
        title = stringResource(R.string.config_try_multiple_authorizers_on_install),
        insideMargin = DpSize(16.dp, 0.dp),
        onDismissRequest = { showSmartAuthorizerSheet.value = false }
    ) {
        MiuixSmartAuthorizerBottomSheet(
            candidates = uiState.smartAuthorizerCandidates,
            rootMode = rootMode,
            shizukuMode = shizukuMode,
            shizukuAuthorized = shizukuAuthorized,
            dhizukuAvailable = dhizukuAvailable,
            dhizukuAuthorized = dhizukuAuthorized,
            onCandidatesChange = {
                viewModel.dispatch(LabSettingsAction.LabChangeSmartAuthorizerCandidates(it))
            }
        )
    }
}

@Composable
private fun MiuixSmartAuthorizerBottomSheet(
    candidates: List<SmartAuthorizerCandidate>,
    rootMode: RootMode,
    shizukuMode: ShizukuMode,
    shizukuAuthorized: Boolean,
    dhizukuAvailable: Boolean,
    dhizukuAuthorized: Boolean,
    onCandidatesChange: (List<SmartAuthorizerCandidate>) -> Unit
) {
    val context = LocalContext.current
    var sheetCandidates by remember { mutableStateOf(candidates) }

    LaunchedEffect(candidates) {
        if (sheetCandidates != candidates) {
            sheetCandidates = candidates
        }
    }

    fun updateCandidates(next: List<SmartAuthorizerCandidate>) {
        sheetCandidates = next
        onCandidatesChange(next)
    }

    fun toggleCandidate(candidate: SmartAuthorizerCandidate, enabled: Boolean) {
        if (!enabled && candidate.enabled && sheetCandidates.count { it.enabled } <= 1) {
            Toast.makeText(
                context,
                context.getString(R.string.config_smart_authorizer_must_choose_one),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        updateCandidates(sheetCandidates.toggle(candidate.authorizer, enabled))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MiuixInstallerTipCard(stringResource(R.string.config_smart_authorizer_fallback_list_tip))
        MiuixDraggableList(
            items = sheetCandidates,
            itemKey = { it.authorizer.value },
            itemName = { miuixSmartAuthorizerDisplayName(it.authorizer, context::getString) },
            itemDescription = {
                miuixSmartAuthorizerAvailabilityDescription(
                    authorizer = it.authorizer,
                    rootMode = rootMode,
                    shizukuMode = shizukuMode,
                    shizukuAuthorized = shizukuAuthorized,
                    dhizukuAvailable = dhizukuAvailable,
                    dhizukuAuthorized = dhizukuAuthorized,
                    getString = context::getString
                )
            },
            onMove = { from, to ->
                updateCandidates(sheetCandidates.move(from, to))
            },
            onItemClick = { candidate ->
                toggleCandidate(candidate, !candidate.enabled)
            },
            cardColors = CardDefaults.defaultColors(
                color = MiuixTheme.colorScheme.secondaryContainer
            ),
            trailingContent = { candidate ->
                Checkbox(
                    state = ToggleableState(value = candidate.enabled),
                    onClick = { toggleCandidate(candidate, !candidate.enabled) },
                    colors = CheckboxDefaults.checkboxColors()
                )
            }
        )
        Spacer(Modifier.navigationBarsPadding())
    }
}

private fun miuixSmartAuthorizerDisplayName(
    authorizer: Authorizer,
    getString: (Int) -> String
): String =
    if (authorizer == Authorizer.None) {
        getString(R.string.working_status_system_installer)
    } else {
        getString(authorizer.displayNameRes)
    }

private fun miuixSmartAuthorizerAvailabilityDescription(
    authorizer: Authorizer,
    rootMode: RootMode,
    shizukuMode: ShizukuMode,
    shizukuAuthorized: Boolean,
    dhizukuAvailable: Boolean,
    dhizukuAuthorized: Boolean,
    getString: (Int) -> String
): String =
    when (authorizer) {
        Authorizer.Root -> if (rootMode != RootMode.None) {
            "${getString(R.string.available)} (${rootMode.name})"
        } else {
            getString(R.string.unavailable)
        }

        Authorizer.Shizuku -> when {
            shizukuAuthorized -> "${getString(R.string.activate)} (${shizukuMode.desc})"
            shizukuMode != ShizukuMode.NONE -> getString(R.string.shizuku_not_authorized)
            else -> getString(R.string.shizuku_not_available)
        }

        Authorizer.Dhizuku -> when {
            dhizukuAuthorized -> getString(R.string.activate)
            dhizukuAvailable -> getString(R.string.dhizuku_not_authorized)
            else -> getString(R.string.dhizuku_not_available)
        }

        Authorizer.None -> getString(R.string.working_status_system_installer_desc)
        else -> authorizer.value
    }

private fun List<SmartAuthorizerCandidate>.move(
    from: Int,
    to: Int
): List<SmartAuthorizerCandidate> {
    if (from !in indices || to !in indices || from == to) return this
    return toMutableList().apply {
        val item = removeAt(from)
        add(to, item)
    }
}

private fun List<SmartAuthorizerCandidate>.toggle(
    authorizer: Authorizer,
    enabled: Boolean
): List<SmartAuthorizerCandidate> =
    map { candidate ->
        if (candidate.authorizer == authorizer) {
            candidate.copy(enabled = enabled)
        } else {
            candidate
        }
    }
