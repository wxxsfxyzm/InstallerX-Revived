// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
@file:OptIn(ExperimentalMaterial3Api::class)

package com.rosan.installer.ui.page.main.settings.preferred.network

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.config.NetworkSourceMode
import com.rosan.installer.domain.settings.model.preferences.GithubUpdateChannel
import com.rosan.installer.domain.settings.model.preferences.HttpProfile
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.main.widget.dialog.CustomGithubProxyUrlDialog
import com.rosan.installer.ui.page.main.widget.dialog.GithubUpdateChannelSelectionDialog
import com.rosan.installer.ui.page.main.widget.setting.BaseWidget
import com.rosan.installer.ui.page.main.widget.setting.DropDownMenuWidget
import com.rosan.installer.ui.page.main.widget.setting.ExpressiveBackButton
import com.rosan.installer.ui.page.main.widget.setting.SegmentedColumn
import com.rosan.installer.ui.theme.getMaterial3AppBarColor
import com.rosan.installer.ui.theme.installerMaterial3BlurEffect
import com.rosan.installer.ui.theme.rememberMaterial3BlurBackdrop
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.blur.layerBackdrop

private val AospMainSwitchBarMargin = 16.dp
private val AospMainSwitchBarMinHeight = 72.dp
private val AospMainSwitchBarHorizontalPadding = 20.dp
private val AospMainSwitchTitleMargin = 16.dp
private val AospMainSwitchBarRadius = 35.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NetworkPage(useBlur: Boolean, viewModel: NetworkSettingsViewModel = koinViewModel()) {
    val navigator = LocalNavigator.current
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val backdrop = rememberMaterial3BlurBackdrop(useBlur)
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    var showChannelDialog by remember { mutableStateOf(false) }
    var showCustomProxyDialog by remember { mutableStateOf(false) }
    var pendingNetworkSourceMode by rememberSaveable { mutableStateOf<NetworkSourceMode?>(null) }
    var exitAfterInternetDisable by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(exitAfterInternetDisable, uiState.allowInternetAccess) {
        if (exitAfterInternetDisable && !uiState.allowInternetAccess) {
            navigator.pop()
        }
    }

    pendingNetworkSourceMode?.let { mode ->
        AlertDialog(
            onDismissRequest = { pendingNetworkSourceMode = null },
            title = { Text(stringResource(R.string.network_source_mode_warning_title)) },
            text = { Text(stringResource(R.string.network_source_mode_warning_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingNetworkSourceMode = null
                        viewModel.dispatch(NetworkSettingsAction.ConfirmNetworkSourceMode(mode))
                    },
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingNetworkSourceMode = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showChannelDialog) {
        GithubUpdateChannelSelectionDialog(
            currentSelection = uiState.githubUpdateChannel,
            onDismiss = { showChannelDialog = false },
            onConfirm = { channel ->
                showChannelDialog = false
                viewModel.dispatch(NetworkSettingsAction.ChangeGithubUpdateChannel(channel))
                if (channel == GithubUpdateChannel.CUSTOM) {
                    showCustomProxyDialog = true
                }
            },
        )
    }

    if (showCustomProxyDialog) {
        CustomGithubProxyUrlDialog(
            initialUrl = uiState.customGithubProxyUrl,
            onDismiss = {
                showCustomProxyDialog = false
                if (uiState.customGithubProxyUrl.isEmpty()) {
                    viewModel.dispatch(
                        NetworkSettingsAction.ChangeGithubUpdateChannel(
                            GithubUpdateChannel.OFFICIAL,
                        ),
                    )
                }
            },
            onConfirm = { url ->
                showCustomProxyDialog = false
                viewModel.dispatch(NetworkSettingsAction.ChangeCustomGithubProxyUrl(url))
                if (url.isEmpty()) {
                    viewModel.dispatch(
                        NetworkSettingsAction.ChangeGithubUpdateChannel(
                            GithubUpdateChannel.OFFICIAL,
                        ),
                    )
                }
            },
        )
    }

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
                    Text(stringResource(R.string.network_settings))
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
                    scrolledContainerColor = backdrop.getMaterial3AppBarColor(),
                ),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
            contentPadding = paddingValues,
        ) {
            item {
                NetworkMasterSwitch(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = AospMainSwitchBarMargin,
                            vertical = AospMainSwitchBarMargin,
                        ),
                    checked = uiState.allowInternetAccess,
                    onCheckedChange = { enabled ->
                        if (!enabled) {
                            exitAfterInternetDisable = true
                        }
                        viewModel.dispatch(
                            NetworkSettingsAction.ChangeInternetAccess(enabled),
                        )
                    },
                )
            }
            if (uiState.allowInternetAccess) {
                item {
                    SegmentedColumn(
                        title = stringResource(R.string.internet_access_enabled),
                    ) {
                        item {
                            NetworkSourceModeWidget(
                                currentMode = uiState.networkSourceMode,
                                onModeChange = { mode ->
                                    if (mode != NetworkSourceMode.Cache &&
                                        !uiState.networkSourceModeWarningAcknowledged
                                    ) {
                                        pendingNetworkSourceMode = mode
                                    } else {
                                        viewModel.dispatch(
                                            NetworkSettingsAction.ChangeNetworkSourceMode(mode),
                                        )
                                    }
                                },
                            )
                        }
                        item {
                            NetworkHttpProfileWidget(
                                currentProfile = uiState.httpProfile,
                                onProfileChange = { profile ->
                                    viewModel.dispatch(
                                        NetworkSettingsAction.ChangeHttpProfile(profile),
                                    )
                                },
                            )
                        }

                        val currentChannel = uiState.githubUpdateChannel
                        item {
                            val channelSummary = when (currentChannel) {
                                GithubUpdateChannel.OFFICIAL -> stringResource(
                                    R.string.lab_update_github_proxy_official,
                                )

                                GithubUpdateChannel.PROXY_7ED -> stringResource(
                                    R.string.lab_update_github_proxy_7ed,
                                )

                                GithubUpdateChannel.CUSTOM -> uiState.customGithubProxyUrl.ifBlank {
                                    stringResource(R.string.lab_update_github_proxy_custom)
                                }
                            }
                            BaseWidget(
                                icon = AppIcons.UpdateChannel,
                                title = stringResource(R.string.lab_update_github_proxy),
                                description = channelSummary,
                                onClick = { showChannelDialog = true },
                            ) {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkSourceModeWidget(currentMode: NetworkSourceMode, onModeChange: (NetworkSourceMode) -> Unit) {
    val modes = linkedMapOf(
        NetworkSourceMode.Cache to stringResource(R.string.config_network_source_cache),
        NetworkSourceMode.Smart to stringResource(R.string.config_network_source_smart),
        NetworkSourceMode.LowStorage to stringResource(R.string.config_network_source_low_storage),
    )
    val descriptions = mapOf(
        NetworkSourceMode.Cache to stringResource(R.string.config_network_source_cache_desc),
        NetworkSourceMode.Smart to stringResource(R.string.config_network_source_smart_desc),
        NetworkSourceMode.LowStorage to stringResource(R.string.config_network_source_low_storage_desc),
    )

    DropDownMenuWidget(
        icon = AppIcons.NetworkSource,
        title = stringResource(R.string.config_network_source_mode),
        description = descriptions.getValue(currentMode),
        choice = modes.keys.indexOf(currentMode).coerceAtLeast(0),
        data = modes.values.toList(),
        onChoiceChange = { index ->
            modes.keys.elementAtOrNull(index)?.let(onModeChange)
        },
    )
}

@Composable
private fun NetworkHttpProfileWidget(currentProfile: HttpProfile, onProfileChange: (HttpProfile) -> Unit) {
    val profiles = remember {
        listOf(
            HttpProfile.ALLOW_SECURE,
            HttpProfile.ALLOW_LOCAL,
            HttpProfile.ALLOW_ALL,
        )
    }
    val options = profiles.map { profile ->
        when (profile) {
            HttpProfile.ALLOW_SECURE -> stringResource(R.string.lab_http_profile_secure)
            HttpProfile.ALLOW_LOCAL -> stringResource(R.string.lab_http_profile_local)
            HttpProfile.ALLOW_ALL -> stringResource(R.string.lab_http_profile_all)
        }
    }
    val currentIndex = profiles.indexOf(currentProfile).coerceAtLeast(0)

    DropDownMenuWidget(
        icon = Icons.Default.Security,
        title = stringResource(R.string.lab_http_profile),
        description = options.getOrNull(currentIndex),
        choice = currentIndex,
        data = options,
        onChoiceChange = { index ->
            profiles.getOrElse(index) { HttpProfile.ALLOW_SECURE }
                .let(onProfileChange)
        },
    )
}

@Composable
private fun NetworkMasterSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val containerColor = if (checked) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = if (checked) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .heightIn(min = AospMainSwitchBarMinHeight)
            .clip(shape = RoundedCornerShape(AospMainSwitchBarRadius))
            .background(containerColor)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = AospMainSwitchBarHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.network_access),
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = contentColor,
            modifier = Modifier
                .weight(1f)
                .padding(end = AospMainSwitchTitleMargin),
        )
        Switch(
            modifier = Modifier.clearAndSetSemantics {},
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedIconColor = MaterialTheme.colorScheme.primary,
                uncheckedIconColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
            thumbContent = {
                Icon(
                    imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            },
        )
    }
}
