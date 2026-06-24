// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.uninstaller

import android.content.Intent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.core.bitmask.hasFlag
import com.rosan.installer.domain.engine.model.install.UninstallFlags
import com.rosan.installer.ui.activity.UninstallerActivity
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.main.widget.card.InfoTipCard
import com.rosan.installer.ui.page.main.widget.dialog.UninstallPackageDialog
import com.rosan.installer.ui.page.main.widget.setting.ExpressiveBackButton
import com.rosan.installer.ui.page.main.widget.setting.NavigationItemWidget
import com.rosan.installer.ui.page.main.widget.setting.SegmentedColumn
import com.rosan.installer.ui.page.main.widget.setting.SwitchWidget
import com.rosan.installer.ui.theme.getMaterial3AppBarColor
import com.rosan.installer.ui.theme.installerMaterial3BlurEffect
import com.rosan.installer.ui.theme.rememberMaterial3BlurBackdrop
import com.rosan.installer.util.toast
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.blur.layerBackdrop

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UninstallerGlobalSettingsPage(
    useBlur: Boolean,
    viewModel: UninstallerSettingsViewModel = koinViewModel()
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

    LaunchedEffect(Unit) {
        topAppBarState.heightOffset = topAppBarState.heightOffsetLimit
    }

    var showUninstallInputDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UninstallerSettingsEvent.ShowMessage -> context.toast(event.resId)
            }
        }
    }

    if (showUninstallInputDialog)
        UninstallPackageDialog(
            onDismiss = { showUninstallInputDialog = false },
            onConfirm = { packageName ->
                showUninstallInputDialog = false
                val intent = Intent(context, UninstallerActivity::class.java).apply {
                    putExtra("package_name", packageName)
                }
                context.startActivity(intent)
            }
        )

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
                    Text(stringResource(R.string.uninstaller_settings))
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
            item { InfoTipCard(text = stringResource(R.string.uninstall_authorizer_tip)) }
            // --- Group 1: Global Settings ---
            item {
                SegmentedColumn(
                    title = stringResource(R.string.global)
                ) {
                    item {
                        SwitchWidget(
                            icon = AppIcons.Save,
                            title = stringResource(id = R.string.uninstall_keep_data),
                            description = stringResource(id = R.string.uninstall_keep_data_desc),
                            checked = uiState.uninstallFlags.hasFlag(UninstallFlags.DELETE_KEEP_DATA),
                            onCheckedChange = {
                                viewModel.dispatch(
                                    UninstallerSettingsAction.ToggleGlobalUninstallFlag(
                                        UninstallFlags.DELETE_KEEP_DATA,
                                        it
                                    )
                                )
                            }
                        )
                    }
                    item {
                        SwitchWidget(
                            icon = AppIcons.InstallForAllUsers,
                            title = stringResource(id = R.string.uninstall_all_users),
                            description = stringResource(id = R.string.uninstall_all_users_desc),
                            checked = uiState.uninstallFlags.hasFlag(UninstallFlags.DELETE_ALL_USERS),
                            onCheckedChange = {
                                viewModel.dispatch(
                                    UninstallerSettingsAction.ToggleGlobalUninstallFlag(
                                        UninstallFlags.DELETE_ALL_USERS,
                                        it
                                    )
                                )
                            }
                        )
                    }
                    item {
                        SwitchWidget(
                            icon = AppIcons.BugReport,
                            title = stringResource(id = R.string.uninstall_delete_system_app),
                            description = stringResource(id = R.string.uninstall_delete_system_app_desc),
                            checked = uiState.uninstallFlags.hasFlag(UninstallFlags.DELETE_SYSTEM_APP),
                            onCheckedChange = {
                                viewModel.dispatch(
                                    UninstallerSettingsAction.ToggleGlobalUninstallFlag(
                                        UninstallFlags.DELETE_SYSTEM_APP,
                                        it
                                    )
                                )
                            }
                        )
                    }
                    if (BiometricManager
                            .from(context)
                            .canAuthenticate(BIOMETRIC_WEAK or BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
                    ) item {
                        SwitchWidget(
                            icon = AppIcons.BiometricAuth,
                            title = stringResource(R.string.uninstaller_settings_require_biometric_auth),
                            description = stringResource(R.string.uninstaller_settings_require_biometric_auth_desc),
                            checked = uiState.uninstallerRequireBiometricAuth,
                            onCheckedChange = {
                                viewModel.dispatch(UninstallerSettingsAction.ChangeBiometricAuth(it))
                            }
                        )
                    }
                }
            }

            // --- Group 2: Manual Uninstaller Call ---
            item {
                SegmentedColumn(
                    title = stringResource(R.string.uninstall_call_uninstaller)
                ) {
                    item {
                        NavigationItemWidget(
                            icon = AppIcons.Delete,
                            title = stringResource(R.string.uninstall_call_uninstaller_manually),
                            description = stringResource(R.string.uninstall_call_uninstaller_manually_desc)
                        ) {
                            showUninstallInputDialog = true
                        }
                    }
                }
            }

        }
    }
}
