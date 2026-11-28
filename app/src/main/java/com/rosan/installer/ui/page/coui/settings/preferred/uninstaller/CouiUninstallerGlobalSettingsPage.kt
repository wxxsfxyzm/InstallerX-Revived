// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.settings.preferred.uninstaller

import android.content.Intent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
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
import com.rosan.installer.core.bitmask.hasFlag
import com.rosan.installer.domain.engine.model.install.UninstallFlags
import com.rosan.installer.ui.activity.UninstallerActivity
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.coui.widgets.CouiBackButton
import com.rosan.installer.ui.page.coui.widgets.CouiNavigationItem
import com.rosan.installer.ui.page.coui.widgets.CouiSettingsTipCard
import com.rosan.installer.ui.page.coui.widgets.CouiSwitchItem
import com.rosan.installer.ui.page.coui.widgets.CouiUninstallPackageDialog
import com.rosan.installer.ui.page.main.settings.preferred.uninstaller.UninstallerSettingsAction
import com.rosan.installer.ui.page.main.settings.preferred.uninstaller.UninstallerSettingsEvent
import com.rosan.installer.ui.page.main.settings.preferred.uninstaller.UninstallerSettingsViewModel
import com.rosan.installer.util.toast
import org.koin.androidx.compose.koinViewModel

@Composable
fun CouiUninstallerGlobalSettingsPage(
    useBlur: Boolean,
    viewModel: UninstallerSettingsViewModel = koinViewModel(),
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scrollBehavior = CouiTopAppBarDefaults.exitUntilCollapsedScrollBehavior {
        listState.canScrollForward || listState.canScrollBackward
    }
    val density = LocalDensity.current
    val showUninstallInputDialog = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UninstallerSettingsEvent.ShowMessage -> context.toast(event.resId)
            }
        }
    }

    CouiUninstallPackageDialog(
        showState = showUninstallInputDialog,
        onDismiss = { showUninstallInputDialog.value = false },
        onConfirm = { packageName ->
            showUninstallInputDialog.value = false
            val intent = Intent(context, UninstallerActivity::class.java).apply {
                putExtra("package_name", packageName)
            }
            context.startActivity(intent)
        },
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CouiTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(R.string.uninstaller_settings)) },
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
            item { CouiSettingsTipCard(stringResource(R.string.uninstall_authorizer_tip)) }

            item {
                Group(title = stringResource(R.string.global)) {
                    CouiSwitchItem(
                        title = stringResource(id = R.string.uninstall_keep_data),
                        description = stringResource(id = R.string.uninstall_keep_data_desc),
                        checked = uiState.uninstallFlags.hasFlag(UninstallFlags.DELETE_KEEP_DATA),
                        onCheckedChange = {
                            viewModel.dispatch(
                                UninstallerSettingsAction.ToggleGlobalUninstallFlag(UninstallFlags.DELETE_KEEP_DATA, it),
                            )
                        },
                    )
                    Divider()
                    CouiSwitchItem(
                        title = stringResource(id = R.string.uninstall_all_users),
                        description = stringResource(id = R.string.uninstall_all_users_desc),
                        checked = uiState.uninstallFlags.hasFlag(UninstallFlags.DELETE_ALL_USERS),
                        onCheckedChange = {
                            viewModel.dispatch(
                                UninstallerSettingsAction.ToggleGlobalUninstallFlag(UninstallFlags.DELETE_ALL_USERS, it),
                            )
                        },
                    )
                    Divider()
                    CouiSwitchItem(
                        title = stringResource(id = R.string.uninstall_delete_system_app),
                        description = stringResource(id = R.string.uninstall_delete_system_app_desc),
                        checked = uiState.uninstallFlags.hasFlag(UninstallFlags.DELETE_SYSTEM_APP),
                        onCheckedChange = {
                            viewModel.dispatch(
                                UninstallerSettingsAction.ToggleGlobalUninstallFlag(UninstallFlags.DELETE_SYSTEM_APP, it),
                            )
                        },
                    )
                    if (BiometricManager
                            .from(context)
                            .canAuthenticate(BIOMETRIC_WEAK or BIOMETRIC_STRONG or DEVICE_CREDENTIAL) ==
                        BiometricManager.BIOMETRIC_SUCCESS
                    ) {
                        Divider()
                        CouiSwitchItem(
                            icon = AppIcons.BiometricAuth,
                            title = stringResource(R.string.uninstaller_settings_require_biometric_auth),
                            description = stringResource(R.string.uninstaller_settings_require_biometric_auth_desc),
                            checked = uiState.uninstallerRequireBiometricAuth,
                            onCheckedChange = {
                                viewModel.dispatch(UninstallerSettingsAction.ChangeBiometricAuth(it))
                            },
                        )
                    }
                }
            }

            item {
                Group(title = stringResource(R.string.uninstall_call_uninstaller)) {
                    CouiNavigationItem(
                        title = stringResource(R.string.uninstall_call_uninstaller_manually),
                        description = stringResource(R.string.uninstall_call_uninstaller_manually_desc),
                        onClick = { showUninstallInputDialog.value = true },
                    )
                }
            }
        }
    }
}
