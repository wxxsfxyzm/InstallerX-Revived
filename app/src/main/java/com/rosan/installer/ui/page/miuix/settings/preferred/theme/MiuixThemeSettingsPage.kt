// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.settings.preferred.theme

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.rosan.installer.R
import com.rosan.installer.core.env.DeviceConfig
import com.rosan.installer.domain.device.model.Manufacturer
import com.rosan.installer.domain.settings.model.PredictiveBackAnimation
import com.rosan.installer.domain.settings.model.PredictiveBackExitDirection
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.main.settings.preferred.theme.ThemeSettingsAction
import com.rosan.installer.ui.page.main.settings.preferred.theme.ThemeSettingsViewModel
import com.rosan.installer.ui.page.main.widget.card.ColorSwatchPreview
import com.rosan.installer.ui.page.miuix.settings.preferred.MiuixColorSpecWidget
import com.rosan.installer.ui.page.miuix.settings.preferred.MiuixPaletteStyleWidget
import com.rosan.installer.ui.page.miuix.settings.preferred.MiuixThemeEngineWidget
import com.rosan.installer.ui.page.miuix.settings.preferred.MiuixThemeModeWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import com.rosan.installer.ui.page.miuix.widgets.MiuixSwitchWidget
import com.rosan.installer.ui.theme.getMiuixAppBarColor
import com.rosan.installer.ui.theme.installerHazeEffect
import com.rosan.installer.ui.theme.rememberMiuixHazeStyle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SpinnerEntry
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowDialog

@SuppressLint("RestrictedApi")
@Composable
fun MiuixThemeSettingsPage(
    viewModel: ThemeSettingsViewModel = koinViewModel()
) {
    val navigator = LocalNavigator.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = MiuixScrollBehavior()
    val hazeState = if (uiState.useBlur) remember { HazeState() } else null
    val hazeStyle = rememberMiuixHazeStyle()
    val transition = LocalNavAnimatedContentScope.current.transition

    val showHideLauncherIconDialog = remember { mutableStateOf(false) }
    val showBlurWarningDialog = remember { mutableStateOf(false) }

    MiuixHideLauncherIconWarningDialog(
        showState = showHideLauncherIconDialog,
        onDismiss = { showHideLauncherIconDialog.value = false },
        onConfirm = {
            showHideLauncherIconDialog.value = false
            viewModel.dispatch(ThemeSettingsAction.ChangeShowLauncherIcon(false))
        }
    )

    MiuixBlurWarningDialog(
        showState = showBlurWarningDialog,
        onDismiss = { showBlurWarningDialog.value = false },
        onConfirm = {
            showBlurWarningDialog.value = false
            viewModel.dispatch(ThemeSettingsAction.SetUseBlur(true))
        }
    )

    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.installerHazeEffect(hazeState, hazeStyle),
                color = hazeState.getMiuixAppBarColor(),
                title = stringResource(R.string.theme_settings),
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
            item { SmallTitle(stringResource(R.string.theme_settings_ui_style)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    MiuixThemeEngineWidget(
                        currentThemeIsMiuix = uiState.showMiuixUI,
                        onThemeChange = { useMiuix ->
                            viewModel.dispatch(ThemeSettingsAction.ChangeUseMiuix(useMiuix))
                        }
                    )
                    MiuixSwitchWidget(
                        title = stringResource(R.string.theme_settings_use_apple_floating_bar),
                        description = stringResource(R.string.theme_settings_use_apple_floating_bar_desc),
                        checked = uiState.useAppleFloatingBar,
                        onCheckedChange = {
                            viewModel.dispatch(ThemeSettingsAction.SetUseAppleFloatingBar(it))
                        }
                    )
                }
            }
            item { SmallTitle(stringResource(R.string.theme_settings_miuix_ui)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    MiuixThemeModeWidget(
                        currentThemeMode = uiState.themeMode,
                        onThemeModeChange = { newMode ->
                            viewModel.dispatch(ThemeSettingsAction.SetThemeMode(newMode))
                        }
                    )
                    MiuixSwitchWidget(
                        title = stringResource(R.string.theme_settings_use_blur),
                        description = stringResource(R.string.theme_settings_use_blur_desc),
                        checked = uiState.useBlur,
                        onCheckedChange = { isChecked ->
                            if (isChecked && Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
                                showBlurWarningDialog.value = true
                            } else {
                                viewModel.dispatch(ThemeSettingsAction.SetUseBlur(isChecked))
                            }
                        }
                    )
                    MiuixSwitchWidget(
                        title = stringResource(R.string.theme_settings_miuix_custom_colors),
                        description = stringResource(R.string.theme_settings_miuix_custom_colors_desc),
                        checked = uiState.useMiuixMonet,
                        onCheckedChange = {
                            viewModel.dispatch(ThemeSettingsAction.SetUseMiuixMonet(it))
                        }
                    )
                    AnimatedVisibility(
                        visible = uiState.useMiuixMonet,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        MiuixSwitchWidget(
                            title = stringResource(R.string.theme_settings_dynamic_color),
                            description = stringResource(R.string.theme_settings_dynamic_color_desc),
                            checked = uiState.useDynamicColor,
                            onCheckedChange = {
                                viewModel.dispatch(ThemeSettingsAction.SetUseDynamicColor(it))
                            }
                        )
                    }
                    AnimatedVisibility(
                        visible = uiState.useMiuixMonet,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        MiuixPaletteStyleWidget(
                            currentPaletteStyle = uiState.paletteStyle,
                            onPaletteStyleChange = { newStyle ->
                                viewModel.dispatch(ThemeSettingsAction.SetPaletteStyle(newStyle))
                            }
                        )
                    }
                    AnimatedVisibility(
                        visible = uiState.useMiuixMonet,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        MiuixColorSpecWidget(
                            currentColorSpec = uiState.colorSpec,
                            currentPaletteStyle = uiState.paletteStyle,
                            onColorSpecChange = { newSpec ->
                                viewModel.dispatch(ThemeSettingsAction.SetColorSpec(newSpec))
                            }
                        )
                    }
                    AnimatedVisibility(
                        visible = uiState.useMiuixMonet,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        MiuixSwitchWidget(
                            title = stringResource(R.string.theme_settings_dynamic_color_follow_icon),
                            description = stringResource(R.string.theme_settings_dynamic_color_follow_icon_desc),
                            checked = uiState.useDynColorFollowPkgIcon,
                            onCheckedChange = {
                                viewModel.dispatch(ThemeSettingsAction.SetDynColorFollowPkgIcon(it))
                            }
                        )
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA && uiState.showLiveActivity)
                        MiuixSwitchWidget(
                            title = stringResource(R.string.theme_settings_live_activity_dynamic_color_follow_icon),
                            description = stringResource(R.string.theme_settings_live_activity_dynamic_color_follow_icon_desc),
                            checked = uiState.useDynColorFollowPkgIconForLiveActivity,
                            onCheckedChange = {
                                viewModel.dispatch(ThemeSettingsAction.SetDynColorFollowPkgIconForLiveActivity(it))
                            }
                        )
                }
            }

            item {
                AnimatedVisibility(
                    visible = uiState.useMiuixMonet && (!uiState.useDynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S),
                    enter = fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)) +
                            expandVertically(animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)) +
                            shrinkVertically(animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing))
                ) {
                    Column {
                        SmallTitle(stringResource(R.string.theme_settings_theme_color))
                        Card(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp)
                        ) {
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 16.dp)
                            ) {
                                val itemMinWidth = 88.dp
                                val columns = (this.maxWidth / itemMinWidth).toInt().coerceAtLeast(1)
                                val chunkedColors = uiState.availableColors.chunked(columns)

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    chunkedColors.forEach { rowItems ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            rowItems.forEach { rawColor ->
                                                Box(
                                                    modifier = Modifier.weight(1f),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    ColorSwatchPreview(
                                                        rawColor = rawColor,
                                                        currentStyle = uiState.paletteStyle,
                                                        colorSpec = uiState.colorSpec,
                                                        textStyle = MiuixTheme.textStyles.footnote1,
                                                        textColor = MiuixTheme.colorScheme.onSurface,
                                                        isSelected = uiState.seedColor == rawColor.color &&
                                                                !(uiState.useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S),
                                                    ) {
                                                        viewModel.dispatch(
                                                            ThemeSettingsAction.SetSeedColor(
                                                                rawColor.color
                                                            )
                                                        )
                                                    }
                                                }
                                            }

                                            val remaining = columns - rowItems.size
                                            if (remaining > 0) {
                                                repeat(remaining) {
                                                    Spacer(Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Predictive Back Section
            item { SmallTitle(stringResource(R.string.theme_settings_predictive_back)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    MiuixPredictiveBackAnimationWidget(
                        currentAnimation = uiState.predictiveBackAnimation,
                        onAnimationChange = { newAnim ->
                            // Hey Google
                            // Why you keep playing the animation even we are already play completed?
                            // This is very dirty, We are using RestrictedApi, but we don't have other choice
                            transition.setPlaytimeAfterInitialAndTargetStateEstablished(
                                transition.targetState,
                                transition.targetState,
                                transition.playTimeNanos
                            )

                            viewModel.dispatch(ThemeSettingsAction.SetPredictiveBackAnimation(newAnim))
                        }
                    )

                    AnimatedVisibility(
                        visible = uiState.predictiveBackAnimation == PredictiveBackAnimation.Scale,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        MiuixPredictiveBackExitDirectionWidget(
                            currentDirection = uiState.predictiveBackExitDirection,
                            onDirectionChange = {
                                viewModel.dispatch(ThemeSettingsAction.SetPredictiveBackExitDirection(it))
                            }
                        )
                    }
                }
            }

            item { SmallTitle(stringResource(R.string.theme_settings_package_icons)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    MiuixSwitchWidget(
                        title = stringResource(R.string.theme_settings_prefer_system_icon),
                        description = stringResource(R.string.theme_settings_prefer_system_icon_desc),
                        checked = uiState.preferSystemIcon,
                        onCheckedChange = {
                            viewModel.dispatch(
                                ThemeSettingsAction.ChangePreferSystemIcon(it)
                            )
                        }
                    )
                }
            }
            item { SmallTitle(stringResource(R.string.theme_settings_launcher_icons)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    MiuixSwitchWidget(
                        title = stringResource(R.string.theme_settings_hide_launcher_icon),
                        description = stringResource(R.string.theme_settings_hide_launcher_icon_desc),
                        checked = !uiState.showLauncherIcon,
                        onCheckedChange = { newCheckedState ->
                            if (newCheckedState) {
                                showHideLauncherIconDialog.value = true
                            } else {
                                viewModel.dispatch(ThemeSettingsAction.ChangeShowLauncherIcon(true))
                            }
                        }
                    )
                }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}

/**
 * WindowSpinnerPreference widget for selecting Predictive Back Animation
 */
@Composable
private fun MiuixPredictiveBackAnimationWidget(
    modifier: Modifier = Modifier,
    currentAnimation: PredictiveBackAnimation,
    onAnimationChange: (PredictiveBackAnimation) -> Unit
) {
    val options = remember { PredictiveBackAnimation.entries }

    // Map entries to their string resources within the Composable context
    val spinnerEntries = options.map { anim ->
        val title = when (anim) {
            PredictiveBackAnimation.None -> stringResource(R.string.theme_settings_predictive_back_animation_none)
            PredictiveBackAnimation.AOSP -> stringResource(R.string.theme_settings_predictive_back_animation_aosp)
            PredictiveBackAnimation.Scale -> stringResource(R.string.theme_settings_predictive_back_animation_scale)
            PredictiveBackAnimation.KernelSUClassic -> stringResource(R.string.theme_settings_predictive_back_animation_ksu_classic)
            PredictiveBackAnimation.KernelSUOfficial -> stringResource(R.string.theme_settings_predictive_back_animation_ksu_official)
        }
        SpinnerEntry(title = title)
    }

    val selectedIndex = remember(currentAnimation, options) {
        options.indexOf(currentAnimation).coerceAtLeast(0)
    }

    WindowSpinnerPreference(
        modifier = modifier,
        title = stringResource(id = R.string.theme_settings_predictive_back_animation),
        items = spinnerEntries,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { newIndex ->
            val newAnim = options[newIndex]
            if (currentAnimation != newAnim) {
                onAnimationChange(newAnim)
            }
        }
    )
}

/**
 * WindowSpinnerPreference widget for selecting Predictive Back Exit Direction
 */
@Composable
private fun MiuixPredictiveBackExitDirectionWidget(
    modifier: Modifier = Modifier,
    currentDirection: PredictiveBackExitDirection,
    onDirectionChange: (PredictiveBackExitDirection) -> Unit
) {
    val options = remember { PredictiveBackExitDirection.entries }

    // Map entries to their string resources within the Composable context
    val spinnerEntries = options.map { dir ->
        val title = when (dir) {
            PredictiveBackExitDirection.FOLLOW_GESTURE -> stringResource(R.string.theme_settings_predictive_back_exit_direction_follow_gesture)
            PredictiveBackExitDirection.ALWAYS_RIGHT -> stringResource(R.string.theme_settings_predictive_back_exit_direction_always_right)
            PredictiveBackExitDirection.ALWAYS_LEFT -> stringResource(R.string.theme_settings_predictive_back_exit_direction_always_left)
        }
        SpinnerEntry(title = title)
    }

    val selectedIndex = remember(currentDirection, options) {
        options.indexOf(currentDirection).coerceAtLeast(0)
    }

    WindowSpinnerPreference(
        modifier = modifier,
        title = stringResource(id = R.string.theme_settings_predictive_back_exit_direction),
        items = spinnerEntries,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { newIndex ->
            val newDir = options[newIndex]
            if (currentDirection != newDir) {
                onDirectionChange(newDir)
            }
        }
    )
}

@Composable
private fun MiuixHideLauncherIconWarningDialog(
    showState: MutableState<Boolean>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    WindowDialog(
        show = showState.value,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.warning),
        content = {
            // Custom content layout with body text and action buttons
            Column {
                // Warning message
                Text(stringResource(R.string.theme_settings_hide_launcher_icon_warning))
                if (DeviceConfig.currentManufacturer == Manufacturer.XIAOMI)
                    Text(stringResource(R.string.theme_settings_hide_launcher_icon_warning_xiaomi))
                Spacer(modifier = Modifier.height(24.dp)) // Spacing before buttons

                // Action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Dismiss button
                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss,
                        text = stringResource(R.string.cancel)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Confirm button with primary color styling
                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = onConfirm,
                        text = stringResource(R.string.confirm),
                        colors = ButtonDefaults.textButtonColorsPrimary() // Apply primary color style
                    )
                }
            }
        }
    )
}

/**
 * A miuix-style dialog to warn the user about unstable blur effects on Android 11 and below.
 */
@Composable
fun MiuixBlurWarningDialog(
    showState: MutableState<Boolean>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    WindowDialog(
        show = showState.value,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.warning),
        content = {
            Column {
                Text(stringResource(R.string.theme_settings_use_blur_warning))

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss,
                        text = stringResource(R.string.cancel)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = onConfirm,
                        text = stringResource(R.string.confirm),
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                }
            }
        }
    )
}
