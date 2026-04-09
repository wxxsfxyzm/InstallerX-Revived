// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.installer.notification

import android.os.Build
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.main.settings.preferred.AutoClearNotificationTimeWidget
import com.rosan.installer.ui.page.main.widget.setting.AppBackButton
import com.rosan.installer.ui.page.main.widget.setting.DropDownMenuWidget
import com.rosan.installer.ui.page.main.widget.setting.IntNumberPickerWidget
import com.rosan.installer.ui.page.main.widget.setting.SplicedColumnGroup
import com.rosan.installer.ui.page.main.widget.setting.SwitchWidget
import com.rosan.installer.ui.theme.getM3TopBarColor
import com.rosan.installer.ui.theme.installerHazeEffect
import com.rosan.installer.ui.theme.none
import com.rosan.installer.ui.theme.rememberMaterial3HazeStyle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewNotificationSettingsPage(
    viewModel: NotificationSettingsViewModel = koinViewModel()
) {
    val navigator = LocalNavigator.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val capabilityProvider = koinInject<DeviceCapabilityProvider>()

    val topAppBarState = rememberTopAppBarState()
    val hazeState = remember { HazeState() }
    val hazeStyle = rememberMaterial3HazeStyle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

    LaunchedEffect(Unit) {
        topAppBarState.heightOffset = topAppBarState.heightOffsetLimit
    }

    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    val isModernEligible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA
    val isMiIslandSupported = remember { capabilityProvider.isSupportMiIsland }

    val styleOptions = remember(isModernEligible, isMiIslandSupported) {
        val list = mutableListOf(NotificationStyle.STANDARD)
        if (isModernEligible) {
            list.add(NotificationStyle.LIVE_ACTIVITY)
            if (isMiIslandSupported) {
                list.add(NotificationStyle.MI_ISLAND)
            }
        }
        list
    }

    val styleNames = styleOptions.map { style ->
        when (style) {
            NotificationStyle.STANDARD -> stringResource(R.string.notification_style_standard)
            NotificationStyle.LIVE_ACTIVITY -> stringResource(R.string.notification_style_live_activity)
            NotificationStyle.MI_ISLAND -> stringResource(R.string.notification_style_mi_island)
        }
    }

    val activeStyle = if (!isModernEligible) NotificationStyle.STANDARD else uiState.currentStyle
    val selectedIndex = styleOptions.indexOf(activeStyle).coerceAtLeast(0)

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        contentWindowInsets = WindowInsets.none,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                modifier = Modifier.installerHazeEffect(hazeState, hazeStyle),
                windowInsets = TopAppBarDefaults.windowInsets.add(WindowInsets(left = 12.dp)),
                title = {
                    Text(stringResource(R.string.notification_settings))
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
                    containerColor = hazeState.getM3TopBarColor(),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    scrolledContainerColor = hazeState.getM3TopBarColor()
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState),
            contentPadding = PaddingValues(
                start = horizontalSafeInsets.calculateStartPadding(layoutDirection),
                top = paddingValues.calculateTopPadding(),
                end = horizontalSafeInsets.calculateEndPadding(layoutDirection),
                bottom = paddingValues.calculateBottomPadding()
            )
        ) {
            item {
                SplicedColumnGroup(
                    title = stringResource(R.string.notification_style)
                ) {
                    item {
                        DropDownMenuWidget(
                            icon = AppIcons.Palette,
                            title = stringResource(R.string.notification_style),
                            description = if (isModernEligible) styleNames[selectedIndex] else stringResource(R.string.notification_style_unsupported_desc),
                            enabled = isModernEligible,
                            choice = selectedIndex,
                            data = styleNames,
                            onChoiceChange = { index ->
                                val selectedStyle = styleOptions[index]
                                viewModel.dispatch(NotificationSettingsAction.ChangeStyle(selectedStyle))
                            }
                        )
                    }
                    item(visible = activeStyle == NotificationStyle.MI_ISLAND) {
                        SwitchWidget(
                            icon = AppIcons.Bypass,
                            title = stringResource(id = R.string.lab_mi_island_bypass_restriction),
                            description = stringResource(id = R.string.lab_mi_island_bypass_restriction_desc),
                            checked = uiState.miIslandBypassRestriction,
                            onCheckedChange = {
                                viewModel.dispatch(NotificationSettingsAction.ChangeMiIslandBypassRestriction(it))
                            }
                        )
                    }
                    item(visible = activeStyle == NotificationStyle.MI_ISLAND && uiState.miIslandBypassRestriction) {
                        IntNumberPickerWidget(
                            icon = AppIcons.StopWatch,
                            title = stringResource(R.string.lab_mi_island_countdown),
                            description = stringResource(R.string.lab_mi_island_countdown_desc),
                            value = uiState.miIslandBlockingInterval,
                            startInt = 50,
                            endInt = 350,
                            onValueChange = {
                                viewModel.dispatch(NotificationSettingsAction.ChangeMiIslandBlockingInterval(it))
                            }
                        )
                    }
                    item(visible = activeStyle == NotificationStyle.MI_ISLAND) {
                        SwitchWidget(
                            icon = AppIcons.Glow,
                            title = stringResource(id = R.string.lab_mi_island_outer_glow),
                            description = stringResource(id = R.string.lab_mi_island_outer_glow_desc),
                            checked = uiState.miIslandOuterGlow,
                            onCheckedChange = {
                                viewModel.dispatch(NotificationSettingsAction.ChangeMiIslandOuterGlow(it))
                            }
                        )
                    }
                }
            }
            item {
                SplicedColumnGroup(
                    title = stringResource(R.string.config_label_preferences)
                ) {
                    item {
                        SwitchWidget(
                            icon = AppIcons.Dialog,
                            title = stringResource(id = R.string.show_dialog_when_pressing_notification),
                            description = stringResource(id = R.string.change_notification_touch_behavior),
                            checked = uiState.showDialogOnPress,
                            isM3E = true,
                            onCheckedChange = {
                                viewModel.dispatch(NotificationSettingsAction.ChangeShowDialogOnPress(it))
                            }
                        )
                    }

                    item {
                        AutoClearNotificationTimeWidget(
                            currentValue = uiState.successAutoClearSeconds,
                            onValueChange = { seconds ->
                                viewModel.dispatch(NotificationSettingsAction.ChangeAutoClearSeconds(seconds))
                            }
                        )
                    }
                }
            }

            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}
