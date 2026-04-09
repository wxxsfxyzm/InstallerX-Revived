// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.settings.preferred.installer.notification

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.main.settings.preferred.installer.notification.NotificationSettingsAction
import com.rosan.installer.ui.page.main.settings.preferred.installer.notification.NotificationSettingsViewModel
import com.rosan.installer.ui.page.main.settings.preferred.installer.notification.NotificationStyle
import com.rosan.installer.ui.page.miuix.settings.preferred.MiuixAutoClearNotificationTimeWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import com.rosan.installer.ui.page.miuix.widgets.MiuixIntNumberPickerWidget
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
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun MiuixNotificationSettingsPage(
    viewModel: NotificationSettingsViewModel = koinViewModel()
) {
    val navigator = LocalNavigator.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val capabilityProvider = koinInject<DeviceCapabilityProvider>()

    val scrollBehavior = MiuixScrollBehavior()
    val hazeState = remember { HazeState() }
    val hazeStyle = rememberMiuixHazeStyle()

    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    val isModernEligible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA
    val isMiIslandSupported = remember { capabilityProvider.isSupportMiIsland }

    // Dynamically build dropdown options based on device capabilities
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

    // Convert the string list to SpinnerEntry list for WindowSpinnerPreference
    val spinnerEntries = remember(styleNames) {
        styleNames.map { SpinnerEntry(title = it) }
    }

    val activeStyle = if (!isModernEligible) NotificationStyle.STANDARD else uiState.currentStyle
    val selectedIndex = styleOptions.indexOf(activeStyle).coerceAtLeast(0)

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.installerHazeEffect(hazeState, hazeStyle),
                color = hazeState.getMiuixAppBarColor(),
                title = stringResource(R.string.notification_settings),
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
                .hazeSource(hazeState)
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
            item { SmallTitle(stringResource(R.string.notification_style)) }

            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    // 1. Notification style dropdown
                    WindowSpinnerPreference(
                        title = stringResource(R.string.notification_style),
                        summary = if (isModernEligible) styleNames[selectedIndex] else stringResource(R.string.notification_style_unsupported_desc),
                        enabled = isModernEligible,
                        items = spinnerEntries,
                        selectedIndex = selectedIndex,
                        onSelectedIndexChange = { index ->
                            val selectedStyle = styleOptions[index]
                            viewModel.dispatch(NotificationSettingsAction.ChangeStyle(selectedStyle))
                        }
                    )
                    // 2. Mi Island bypass restriction (animated visibility)
                    AnimatedVisibility(
                        visible = activeStyle == NotificationStyle.MI_ISLAND,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        MiuixSwitchWidget(
                            title = stringResource(id = R.string.lab_mi_island_bypass_restriction),
                            description = stringResource(id = R.string.lab_mi_island_bypass_restriction_desc),
                            checked = uiState.miIslandBypassRestriction,
                            onCheckedChange = {
                                viewModel.dispatch(NotificationSettingsAction.ChangeMiIslandBypassRestriction(it))
                            }
                        )
                    }
                    // 3. Mi Island network blocking duration (animated visibility)
                    AnimatedVisibility(
                        visible = activeStyle == NotificationStyle.MI_ISLAND && uiState.miIslandBypassRestriction,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        MiuixIntNumberPickerWidget(
                            title = stringResource(R.string.lab_mi_island_countdown),
                            description = stringResource(R.string.lab_mi_island_countdown_desc),
                            value = uiState.miIslandBlockingInterval,
                            startInt = 50,
                            endInt = 350
                        ) {
                            viewModel.dispatch(NotificationSettingsAction.ChangeMiIslandBlockingInterval(it))
                        }
                    }
                    // 4. Mi Island outer glow (animated visibility)
                    AnimatedVisibility(
                        visible = activeStyle == NotificationStyle.MI_ISLAND,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        MiuixSwitchWidget(
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
            item { SmallTitle(stringResource(R.string.config_label_preferences)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    // 3. Show dialog when pressing notification
                    MiuixSwitchWidget(
                        title = stringResource(id = R.string.show_dialog_when_pressing_notification),
                        description = stringResource(id = R.string.change_notification_touch_behavior),
                        checked = uiState.showDialogOnPress,
                        onCheckedChange = {
                            viewModel.dispatch(NotificationSettingsAction.ChangeShowDialogOnPress(it))
                        }
                    )

                    // 4. Auto-clear success notification time
                    MiuixAutoClearNotificationTimeWidget(
                        currentValue = uiState.successAutoClearSeconds,
                        onValueChange = { seconds ->
                            viewModel.dispatch(NotificationSettingsAction.ChangeAutoClearSeconds(seconds))
                        }
                    )
                }
            }

            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}