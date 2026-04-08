// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.settings.preferred.uninstaller

import android.content.Intent
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.ui.activity.UninstallerActivity
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.main.settings.preferred.uninstaller.UninstallerSettingsEvent
import com.rosan.installer.ui.page.main.settings.preferred.uninstaller.UninstallerSettingsViewModel
import com.rosan.installer.ui.page.miuix.settings.preferred.MiuixNavigationItemWidget
import com.rosan.installer.ui.page.miuix.settings.preferred.MiuixUninstallForAllUsersWidget
import com.rosan.installer.ui.page.miuix.settings.preferred.MiuixUninstallKeepDataWidget
import com.rosan.installer.ui.page.miuix.settings.preferred.MiuixUninstallRequireBiometricAuthWidget
import com.rosan.installer.ui.page.miuix.settings.preferred.MiuixUninstallSystemAppWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import com.rosan.installer.ui.page.miuix.widgets.MiuixSettingsTipCard
import com.rosan.installer.ui.page.miuix.widgets.MiuixUninstallPackageDialog
import com.rosan.installer.ui.theme.getMiuixAppBarColor
import com.rosan.installer.ui.theme.installerHazeEffect
import com.rosan.installer.ui.theme.rememberMiuixHazeStyle
import com.rosan.installer.util.toast
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun MiuixUninstallerGlobalSettingsPage(
    viewModel: UninstallerSettingsViewModel = koinViewModel()
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = MiuixScrollBehavior()
    var showUninstallInputDialog = remember { mutableStateOf(false) }
    val hazeState = if (uiState.useBlur) remember { HazeState() } else null
    val hazeStyle = rememberMiuixHazeStyle()

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UninstallerSettingsEvent.ShowMessage -> context.toast(event.resId)
            }
        }
    }

    MiuixUninstallPackageDialog(
        showState = showUninstallInputDialog,
        onDismiss = { showUninstallInputDialog.value = false },
        onConfirm = { packageName ->
            showUninstallInputDialog.value = false
            val intent = Intent(context, UninstallerActivity::class.java).apply {
                putExtra("package_name", packageName)
            }
            context.startActivity(intent)
        }
    )

    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.installerHazeEffect(hazeState, hazeStyle),
                color = hazeState.getMiuixAppBarColor(),
                title = stringResource(R.string.uninstaller_settings),
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
                .then(hazeState?.let { Modifier.hazeSource(it) } ?: Modifier)
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
            item { MiuixSettingsTipCard(stringResource(R.string.uninstall_authorizer_tip)) }
            item { SmallTitle(stringResource(R.string.global)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 16.dp)
                ) {
                    MiuixUninstallKeepDataWidget(viewModel)
                    MiuixUninstallForAllUsersWidget(viewModel)
                    MiuixUninstallSystemAppWidget(viewModel)
                    MiuixUninstallRequireBiometricAuthWidget(viewModel)
                }
            }
            item { SmallTitle(stringResource(R.string.uninstall_call_uninstaller)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 16.dp)
                ) {
                    MiuixNavigationItemWidget(
                        title = stringResource(R.string.uninstall_call_uninstaller_manually),
                        description = stringResource(R.string.uninstall_call_uninstaller_manually_desc)
                    ) {
                        showUninstallInputDialog.value = true
                    }
                }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}