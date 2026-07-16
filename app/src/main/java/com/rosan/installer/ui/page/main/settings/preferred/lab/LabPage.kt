// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
@file:OptIn(ExperimentalMaterial3Api::class)

package com.rosan.installer.ui.page.main.settings.preferred.lab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.main.widget.card.InfoTipCard
import com.rosan.installer.ui.page.main.widget.dialog.CustomGithubProxyUrlDialog
import com.rosan.installer.ui.page.main.widget.dialog.GithubUpdateChannelSelectionDialog
import com.rosan.installer.ui.page.main.widget.dialog.RootImplementationSelectionDialog
import com.rosan.installer.ui.page.main.widget.setting.BaseWidget
import com.rosan.installer.ui.page.main.widget.setting.DraggableList
import com.rosan.installer.ui.page.main.widget.setting.DropDownMenuWidget
import com.rosan.installer.ui.page.main.widget.setting.ExpressiveBackButton
import com.rosan.installer.ui.page.main.widget.setting.SegmentedColumn
import com.rosan.installer.ui.page.main.widget.setting.SwitchWidget
import com.rosan.installer.ui.theme.getMaterial3AppBarColor
import com.rosan.installer.ui.theme.installerMaterial3BlurEffect
import com.rosan.installer.ui.theme.rememberMaterial3BlurBackdrop
import com.rosan.installer.util.toast
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.blur.layerBackdrop

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LabPage(
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
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val showRootImplementationDialog = remember { mutableStateOf(false) }
    val showChannelDialog = remember { mutableStateOf(false) }
    val showCustomProxyDialog = remember { mutableStateOf(false) }
    val showSmartAuthorizerSheet = remember { mutableStateOf(false) }

    if (showChannelDialog.value)
        GithubUpdateChannelSelectionDialog(
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
        CustomGithubProxyUrlDialog(
            initialUrl = uiState.customGithubProxyUrl,
            onDismiss = {
                showCustomProxyDialog.value = false
                if (uiState.customGithubProxyUrl.isEmpty())
                    viewModel.dispatch(
                        LabSettingsAction.LabChangeGithubUpdateChannel(
                            GithubUpdateChannel.OFFICIAL
                        )
                    )
            },
            onConfirm = { url ->
                showCustomProxyDialog.value = false
                viewModel.dispatch(LabSettingsAction.LabChangeCustomGithubProxyUrl(url))
                if (url.isEmpty())
                    viewModel.dispatch(
                        LabSettingsAction.LabChangeGithubUpdateChannel(
                            GithubUpdateChannel.OFFICIAL
                        )
                    )
            }
        )

    if (showRootImplementationDialog.value) {
        RootImplementationSelectionDialog(
            currentSelection = uiState.labRootMode,
            onDismiss = { showRootImplementationDialog.value = false },
            onConfirm = { selectedImplementation ->
                showRootImplementationDialog.value = false
                // 1. Save the selected implementation
                viewModel.dispatch(
                    LabSettingsAction.LabChangeRootImplementation(
                        selectedImplementation
                    )
                )
                // 2. Enable the flash module feature
                viewModel.dispatch(LabSettingsAction.LabChangeRootModuleFlash(true))
            }
        )
    }

    val smartAuthorizerSummary = uiState.smartAuthorizerCandidates
        .filter { it.enabled }
        .joinToString("->") {
            smartAuthorizerDisplayName(it.authorizer, context::getString)
        }

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
                    Text(stringResource(R.string.lab))
                },
                navigationIcon = {
                    Row {
                        ExpressiveBackButton { navigator.pop() }
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
            contentPadding = paddingValues
        ) {
            item { InfoTipCard(text = stringResource(R.string.lab_tip)) }
            item {
                SegmentedColumn(
                    title = stringResource(R.string.config_authorizer)
                ) {
                    item {
                        SwitchWidget(
                            icon = AppIcons.Authorizer,
                            title = stringResource(R.string.config_try_multiple_authorizers_on_install),
                            description = stringResource(R.string.config_try_multiple_authorizers_on_install_desc),
                            checked = uiState.tryMultipleAuthorizersOnInstall,
                            onCheckedChange = {
                                viewModel.dispatch(
                                    LabSettingsAction.LabChangeTryMultipleAuthorizersOnInstall(it)
                                )
                            }
                        )
                    }
                    item(animatedVisibility = uiState.tryMultipleAuthorizersOnInstall) {
                        BaseWidget(
                            icon = AppIcons.Authorizer,
                            title = stringResource(R.string.config_smart_authorizer_fallback_list),
                            description = stringResource(
                                R.string.config_smart_authorizer_fallback_list_desc,
                                smartAuthorizerSummary
                            ),
                            onClick = {
                                capabilityProvider.refreshPrivilegeStatus()
                                showSmartAuthorizerSheet.value = true
                            }
                        ) {}
                    }
                }
            }
            item {
                SegmentedColumn(
                    title = stringResource(R.string.config_authorizer_root)
                ) {
                    item {
                        SwitchWidget(
                            icon = AppIcons.Root,
                            title = stringResource(R.string.lab_module_flashing),
                            description = stringResource(R.string.lab_module_flashing_desc),
                            checked = uiState.labRootEnableModuleFlash,
                            onCheckedChange = { isChecking ->
                                if (isChecking) {
                                    showRootImplementationDialog.value = true
                                } else {
                                    viewModel.dispatch(
                                        LabSettingsAction.LabChangeRootModuleFlash(
                                            false
                                        )
                                    )
                                }
                            }
                        )
                    }
                    item(animatedVisibility = uiState.labRootEnableModuleFlash) {
                        LabRootImplementationWidget(viewModel)
                    }
                    item(animatedVisibility = uiState.labRootEnableModuleFlash) {
                        SwitchWidget(
                            icon = AppIcons.Terminal,
                            title = stringResource(R.string.lab_module_flashing_show_art),
                            description = stringResource(R.string.lab_module_flashing_show_art_desc),
                            checked = uiState.labRootShowModuleArt,
                            onCheckedChange = {
                                viewModel.dispatch(LabSettingsAction.LabChangeRootShowModuleArt(it))
                            }
                        )
                    }
                }
            }
            if (AppConfig.isRespectPlatformInstallPolicyAvailable) {
                item {
                    SegmentedColumn(
                        title = stringResource(R.string.lab_unstable_features)
                    ) {
                        item {
                            SwitchWidget(
                                icon = AppIcons.InstallRequester,
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
            }

            if (AppConfig.isInternetAccessEnabled)
                item {
                    SegmentedColumn(
                        title = stringResource(R.string.internet_access_enabled)
                    ) {
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
                        item { LabHttpProfileWidget(viewModel) }

                        val currentChannel = uiState.githubUpdateChannel
                        item {
                            val channelSummary = when (currentChannel) {
                                GithubUpdateChannel.OFFICIAL -> stringResource(R.string.lab_update_github_proxy_official)
                                GithubUpdateChannel.PROXY_7ED -> stringResource(R.string.lab_update_github_proxy_7ed)
                                GithubUpdateChannel.CUSTOM -> uiState.customGithubProxyUrl.ifBlank {
                                    stringResource(R.string.lab_update_github_proxy_custom)
                                }
                            }
                            BaseWidget(
                                title = stringResource(R.string.lab_update_github_proxy),
                                description = channelSummary,
                                onClick = { showChannelDialog.value = true }
                            ) {}
                        }
                    }
                }
        }
    }

    if (showSmartAuthorizerSheet.value) {
        ModalBottomSheet(
            onDismissRequest = { showSmartAuthorizerSheet.value = false }
        ) {
            SmartAuthorizerBottomSheet(
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
}

@Composable
private fun SmartAuthorizerBottomSheet(
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
            context.toast(R.string.config_smart_authorizer_must_choose_one)
            return
        }
        updateCandidates(sheetCandidates.toggle(candidate.authorizer, enabled))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.config_try_multiple_authorizers_on_install),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.headlineSmall
        )
        InfoTipCard(
            text = stringResource(R.string.config_smart_authorizer_fallback_list_tip),
            noPadding = true
        )
        DraggableList(
            items = sheetCandidates,
            itemKey = { it.authorizer.value },
            itemName = { smartAuthorizerDisplayName(it.authorizer, context::getString) },
            itemDescription = {
                smartAuthorizerAvailabilityDescription(
                    authorizer = it.authorizer,
                    rootMode = rootMode,
                    shizukuMode = shizukuMode,
                    shizukuAuthorized = shizukuAuthorized,
                    dhizukuAvailable = dhizukuAvailable,
                    dhizukuAuthorized = dhizukuAuthorized,
                    getString = context::getString
                )
            },
            itemLeadingIcon = { smartAuthorizerIcon(it.authorizer) },
            onMove = { from, to ->
                updateCandidates(sheetCandidates.move(from, to))
            },
            onItemClick = { candidate ->
                toggleCandidate(candidate, !candidate.enabled)
            },
            noContentTitle = stringResource(R.string.config_authorizer),
            trailingContent = { candidate ->
                Checkbox(
                    checked = candidate.enabled,
                    onCheckedChange = { checked ->
                        toggleCandidate(candidate, checked)
                    },
                    modifier = Modifier.clearAndSetSemantics {}
                )
            }
        )
        Spacer(Modifier.navigationBarsPadding())
    }
}

private fun smartAuthorizerDisplayName(
    authorizer: Authorizer,
    getString: (Int) -> String
): String =
    if (authorizer == Authorizer.None) {
        getString(R.string.working_status_system_installer)
    } else {
        getString(authorizer.displayNameRes)
    }

@Composable
private fun smartAuthorizerIcon(authorizer: Authorizer): ImageVector =
    when (authorizer) {
        Authorizer.None -> AppIcons.None
        Authorizer.Root -> AppIcons.Root
        Authorizer.Shizuku -> ImageVector.vectorResource(R.drawable.ic_shizuku)
        Authorizer.Dhizuku -> AppIcons.InstallAllowRestrictedPermissions
        else -> AppIcons.Authorizer
    }

private fun smartAuthorizerAvailabilityDescription(
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

/**
 * Widget for selecting the Root Implementation (Magisk/KernelSU/APatch).
 * Mimics the logic from MiuixRootImplementationDialog but uses DropDownMenuWidget.
 */
@Composable
private fun LabRootImplementationWidget(viewModel: LabSettingsViewModel) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val currentRootImpl = uiState.labRootMode

    val data = remember {
        mapOf(
            RootMode.Magisk to "Magisk",
            RootMode.KernelSU to "KernelSU",
            RootMode.APatch to "APatch"
        )
    }

    val options = data.values.toList()
    val keys = data.keys.toList()

    val selectedIndex = keys.indexOf(currentRootImpl).coerceAtLeast(0)

    DropDownMenuWidget(
        icon = AppIcons.RootMethod,
        title = stringResource(R.string.lab_module_select_root_impl),
        description = options.getOrNull(selectedIndex),
        choice = selectedIndex,
        data = options,
        onChoiceChange = { newIndex ->
            keys.getOrNull(newIndex)?.let { impl ->
                viewModel.dispatch(LabSettingsAction.LabChangeRootImplementation(impl))
            }
        }
    )
}

@Composable
private fun LabHttpProfileWidget(viewModel: LabSettingsViewModel) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val profiles = remember {
        listOf(
            HttpProfile.ALLOW_SECURE,
            HttpProfile.ALLOW_LOCAL,
            HttpProfile.ALLOW_ALL
        )
    }
    val options = profiles.map { profile ->
        when (profile) {
            HttpProfile.ALLOW_SECURE -> stringResource(R.string.lab_http_profile_secure)
            HttpProfile.ALLOW_LOCAL -> stringResource(R.string.lab_http_profile_local)
            HttpProfile.ALLOW_ALL -> stringResource(R.string.lab_http_profile_all)
        }
    }

    val currentIndex = profiles.indexOf(uiState.labHttpProfile).coerceAtLeast(0)

    DropDownMenuWidget(
        icon = Icons.Default.Security,
        title = stringResource(R.string.lab_http_profile),
        description = options.getOrNull(currentIndex),
        choice = currentIndex,
        data = options,
        onChoiceChange = { index ->
            val selectedProfile = profiles.getOrElse(index) { HttpProfile.ALLOW_SECURE }
            viewModel.dispatch(LabSettingsAction.LabChangeHttpProfile(selectedProfile))
        }
    )
}
