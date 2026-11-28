// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.settings.preferred.installer.notification

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.coui.widgets.CouiBackButton
import com.rosan.installer.ui.page.coui.widgets.CouiIntNumberPickerWidget
import com.rosan.installer.ui.page.coui.widgets.CouiSpinnerItem
import com.rosan.installer.ui.page.coui.widgets.CouiSwitchItem
import com.rosan.installer.ui.page.main.settings.preferred.installer.notification.NotificationSettingsAction
import com.rosan.installer.ui.page.main.settings.preferred.installer.notification.NotificationSettingsViewModel
import com.rosan.installer.ui.page.main.settings.preferred.installer.notification.NotificationStyle
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun CouiNotificationSettingsPage(useBlur: Boolean, viewModel: NotificationSettingsViewModel = koinViewModel()) {
    val navigator = LocalNavigator.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val capabilityProvider = koinInject<DeviceCapabilityProvider>()
    val listState = rememberLazyListState()
    val scrollBehavior = CouiTopAppBarDefaults.exitUntilCollapsedScrollBehavior {
        listState.canScrollForward || listState.canScrollBackward
    }
    val density = LocalDensity.current

    val isModernEligible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA
    val isMiIslandSupported = capabilityProvider.isSupportMiIsland

    val styleOptions = remember(isModernEligible, isMiIslandSupported) {
        buildList {
            add(NotificationStyle.STANDARD)
            if (isModernEligible) add(NotificationStyle.LIVE_ACTIVITY)
            if (isMiIslandSupported) add(NotificationStyle.MI_ISLAND)
        }
    }

    val styleNames = styleOptions.map { style ->
        when (style) {
            NotificationStyle.STANDARD -> stringResource(R.string.notification_style_standard)
            NotificationStyle.LIVE_ACTIVITY -> stringResource(R.string.notification_style_live_activity)
            NotificationStyle.MI_ISLAND -> stringResource(R.string.notification_style_mi_island)
        }
    }

    val activeStyle = if (styleOptions.contains(uiState.currentStyle)) {
        uiState.currentStyle
    } else {
        NotificationStyle.STANDARD
    }
    val selectedIndex = styleOptions.indexOf(activeStyle).coerceAtLeast(0)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CouiTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(R.string.notification_settings)) },
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
            item {
                Group(title = stringResource(R.string.notification_style)) {
                    val isStyleSelectionEnabled = isModernEligible || isMiIslandSupported
                    CouiSpinnerItem(
                        title = stringResource(R.string.notification_style),
                        summary = if (isStyleSelectionEnabled) {
                            styleNames.getOrNull(selectedIndex) ?: ""
                        } else {
                            stringResource(R.string.notification_style_unsupported_desc)
                        },
                        enabled = isStyleSelectionEnabled,
                        items = styleNames,
                        selectedIndex = selectedIndex,
                        onSelectedIndexChange = { index ->
                            viewModel.dispatch(NotificationSettingsAction.ChangeStyle(styleOptions[index]))
                        },
                    )

                    AnimatedVisibility(
                        visible = activeStyle == NotificationStyle.MI_ISLAND,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Column {
                            Divider()
                            CouiSwitchItem(
                                title = stringResource(id = R.string.lab_mi_island_bypass_restriction),
                                description = stringResource(id = R.string.lab_mi_island_bypass_restriction_desc),
                                checked = uiState.miIslandBypassRestriction,
                                onCheckedChange = {
                                    viewModel.dispatch(NotificationSettingsAction.ChangeMiIslandBypassRestriction(it))
                                },
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = activeStyle == NotificationStyle.MI_ISLAND && uiState.miIslandBypassRestriction,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Column {
                            Divider()
                            CouiIntNumberPickerWidget(
                                title = stringResource(R.string.lab_mi_island_countdown),
                                description = stringResource(R.string.lab_mi_island_countdown_desc),
                                value = uiState.miIslandBlockingInterval,
                                startInt = 50,
                                endInt = 350,
                            ) {
                                viewModel.dispatch(NotificationSettingsAction.ChangeMiIslandBlockingInterval(it))
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = activeStyle == NotificationStyle.MI_ISLAND,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Column {
                            Divider()
                            CouiSwitchItem(
                                title = stringResource(id = R.string.lab_mi_island_outer_glow),
                                description = stringResource(id = R.string.lab_mi_island_outer_glow_desc),
                                checked = uiState.miIslandOuterGlow,
                                onCheckedChange = {
                                    viewModel.dispatch(NotificationSettingsAction.ChangeMiIslandOuterGlow(it))
                                },
                            )
                        }
                    }
                }
            }

            item {
                Group(title = stringResource(R.string.config_label_preferences)) {
                    CouiSwitchItem(
                        title = stringResource(id = R.string.show_dialog_when_pressing_notification),
                        description = stringResource(id = R.string.change_notification_touch_behavior),
                        checked = uiState.showDialogOnPress,
                        onCheckedChange = {
                            viewModel.dispatch(NotificationSettingsAction.ChangeShowDialogOnPress(it))
                        },
                    )
                    Divider()
                    CouiAutoClearNotificationTimeWidget(
                        currentValue = uiState.successAutoClearSeconds,
                        onValueChange = { seconds ->
                            viewModel.dispatch(NotificationSettingsAction.ChangeAutoClearSeconds(seconds))
                        },
                    )
                }
            }
        }
    }
}

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
private fun CouiAutoClearNotificationTimeWidget(
    currentValue: Int,
    onValueChange: (Int) -> Unit,
) {
    val context = LocalContext.current
    val options = remember { listOf(0, 3, 5, 10, 15, 20, 30) }
    val labels = remember(options) {
        options.map { time ->
            if (time == 0) {
                context.getString(R.string.installer_settings_auto_clear_time_never)
            } else {
                context.getString(R.string.installer_settings_auto_clear_time_seconds_format, time)
            }
        }
    }
    val selectedIndex = remember(currentValue, options) {
        options.indexOf(currentValue).coerceAtLeast(0)
    }

    CouiSpinnerItem(
        title = stringResource(id = R.string.installer_settings_auto_clear_success_notification),
        summary = stringResource(id = R.string.installer_settings_auto_clear_success_notification_desc),
        items = labels,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { newIndex ->
            val newValue = options[newIndex]
            if (currentValue != newValue) {
                onValueChange(newValue)
            }
        },
    )
}
