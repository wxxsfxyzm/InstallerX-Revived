// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.theme

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.twotone.Colorize
import androidx.compose.material.icons.twotone.InvertColors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.PredictiveBackAnimation
import com.rosan.installer.domain.settings.model.PredictiveBackExitDirection
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.main.widget.card.ColorSwatchPreview
import com.rosan.installer.ui.page.main.widget.dialog.BlurWarningDialog
import com.rosan.installer.ui.page.main.widget.dialog.HideLauncherIconWarningDialog
import com.rosan.installer.ui.page.main.widget.setting.AppBackButton
import com.rosan.installer.ui.page.main.widget.setting.BaseItemContainer
import com.rosan.installer.ui.page.main.widget.setting.BaseWidget
import com.rosan.installer.ui.page.main.widget.setting.SegmentedColumn
import com.rosan.installer.ui.page.main.widget.setting.SwitchWidget
import com.rosan.installer.ui.theme.getMaterial3AppBarColor
import com.rosan.installer.ui.theme.installerMaterial3BlurEffect
import com.rosan.installer.ui.theme.material.PaletteStyle
import com.rosan.installer.ui.theme.material.ThemeMode
import com.rosan.installer.ui.theme.rememberMaterial3BlurBackdrop
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.blur.layerBackdrop

@SuppressLint("RestrictedApi")
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThemeSettingsPage(
    viewModel: ThemeSettingsViewModel = koinViewModel()
) {
    val navigator = LocalNavigator.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val topAppBarState = rememberTopAppBarState(-154f, -154f) // from debugger
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

    var showHideLauncherIconDialog by remember { mutableStateOf(false) }
    var showPaletteDialog by remember { mutableStateOf(false) }
    var showThemeModeDialog by remember { mutableStateOf(false) }
    var showBlurWarningDialog by remember { mutableStateOf(false) }
    var showPredictiveBackAnimationDialog by remember { mutableStateOf(false) }
    var showPredictiveBackExitDirectionDialog by remember { mutableStateOf(false) }
    val transition = LocalNavAnimatedContentScope.current.transition

    if (showPredictiveBackAnimationDialog) {
        PredictiveBackAnimationDialog(
            currentAnimation = uiState.predictiveBackAnimation,
            onDismiss = { showPredictiveBackAnimationDialog = false },
            onSelect = { animation ->
                // Hey Google
                // Why you keep playing the animation even we are already play completed?

                // This is very dirty, We are using RestrictedApi, but we don't have other choice
                transition.setPlaytimeAfterInitialAndTargetStateEstablished(
                    transition.targetState,
                    transition.targetState,
                    transition.playTimeNanos
                )

                viewModel.dispatch(ThemeSettingsAction.SetPredictiveBackAnimation(animation))
                showPredictiveBackAnimationDialog = false
            }
        )
    }

    if (showPredictiveBackExitDirectionDialog) {
        PredictiveBackExitDirectionDialog(
            currentDirection = uiState.predictiveBackExitDirection,
            onDismiss = { showPredictiveBackExitDirectionDialog = false },
            onSelect = { direction ->
                viewModel.dispatch(ThemeSettingsAction.SetPredictiveBackExitDirection(direction))
                showPredictiveBackExitDirectionDialog = false
            }
        )
    }

    if (showPaletteDialog) {
        PaletteStyleDialog(
            currentStyle = uiState.paletteStyle,
            onDismiss = { showPaletteDialog = false },
            onSelect = { style ->
                viewModel.dispatch(ThemeSettingsAction.SetPaletteStyle(style))
                showPaletteDialog = false
            }
        )
    }

    if (showThemeModeDialog) {
        ThemeModeDialog(
            currentMode = uiState.themeMode,
            onDismiss = { showThemeModeDialog = false },
            onSelect = { mode ->
                viewModel.dispatch(ThemeSettingsAction.SetThemeMode(mode))
                showThemeModeDialog = false
            }
        )
    }

    HideLauncherIconWarningDialog(
        show = showHideLauncherIconDialog,
        onDismiss = { showHideLauncherIconDialog = false },
        onConfirm = {
            showHideLauncherIconDialog = false
            viewModel.dispatch(ThemeSettingsAction.ChangeShowLauncherIcon(false))
        }
    )

    BlurWarningDialog(
        show = showBlurWarningDialog,
        onDismiss = { showBlurWarningDialog = false },
        onConfirm = {
            showBlurWarningDialog = false
            viewModel.dispatch(ThemeSettingsAction.SetUseBlur(true))
        }
    )

    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    val backdrop = rememberMaterial3BlurBackdrop(uiState.useBlur)

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
                    Text(stringResource(R.string.theme_settings))
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
                    containerColor = backdrop.getMaterial3AppBarColor(),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    scrolledContainerColor = backdrop.getMaterial3AppBarColor()
                )
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
            contentPadding = PaddingValues(
                start = horizontalSafeInsets.calculateStartPadding(layoutDirection),
                top = paddingValues.calculateTopPadding(),
                end = horizontalSafeInsets.calculateEndPadding(layoutDirection),
                bottom = paddingValues.calculateBottomPadding()
            )
        ) {
            // --- Group 1: UI Style Selection ---
            item {
                SegmentedColumn(
                    title = stringResource(R.string.theme_settings_ui_style)
                ) {
                    // Option 1: Google UI
                    item {
                        val selected = !uiState.showMiuixUI
                        val onClick = {
                            if (uiState.showMiuixUI) {
                                viewModel.dispatch(ThemeSettingsAction.ChangeUseMiuix(false))
                            }
                        }
                        BaseWidget(
                            icon = null,
                            iconPlaceholder = false, // Force text alignment to the start edge
                            title = stringResource(R.string.theme_settings_google_ui),
                            description = stringResource(R.string.theme_settings_google_ui_desc),
                            selected = selected,
                            onClick = onClick
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = onClick,
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                    // Option 2: MIUIX UI
                    item {
                        val selected = uiState.showMiuixUI
                        val onClick = {
                            if (!uiState.showMiuixUI) {
                                viewModel.dispatch(ThemeSettingsAction.ChangeUseMiuix(true))
                            }
                        }
                        BaseWidget(
                            icon = null,
                            iconPlaceholder = false, // Force text alignment to the start edge
                            title = stringResource(R.string.theme_settings_miuix_ui),
                            description = stringResource(R.string.theme_settings_miuix_ui_desc),
                            selected = selected,
                            onClick = onClick
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = onClick,
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }

            // --- Group 2: Google UI Options ---
            item {
                SegmentedColumn(
                    title = stringResource(R.string.theme_settings_google_ui)
                ) {
                    item {
                        SwitchWidget(
                            icon = AppIcons.Blur,
                            title = stringResource(R.string.theme_settings_use_blur),
                            description = stringResource(R.string.theme_settings_use_blur_desc),
                            checked = uiState.useBlur,
                            onCheckedChange = { isChecked ->
                                if (isChecked && Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
                                    showBlurWarningDialog = true
                                } else {
                                    viewModel.dispatch(ThemeSettingsAction.SetUseBlur(isChecked))
                                }
                            }
                        )
                    }
                    item {
                        BaseWidget(
                            icon = Icons.Default.DarkMode,
                            title = stringResource(R.string.theme_settings_theme_mode),
                            description = when (uiState.themeMode) {
                                ThemeMode.LIGHT -> stringResource(R.string.theme_settings_theme_mode_light)
                                ThemeMode.DARK -> stringResource(R.string.theme_settings_theme_mode_dark)
                                ThemeMode.SYSTEM -> stringResource(R.string.theme_settings_theme_mode_system)
                            },
                            onClick = { showThemeModeDialog = true }
                        ) {}
                    }
                    item {
                        BaseWidget(
                            icon = AppIcons.Palette,
                            title = stringResource(R.string.theme_settings_palette_style),
                            description = uiState.paletteStyle.displayName,
                            onClick = { showPaletteDialog = true }
                        ) {}
                    }
                    item { ColorSpecSelector(viewModel) }
                    item {
                        SwitchWidget(
                            icon = Icons.TwoTone.InvertColors,
                            title = stringResource(R.string.theme_settings_dynamic_color),
                            description = stringResource(R.string.theme_settings_dynamic_color_desc),
                            checked = uiState.useDynamicColor,
                            onCheckedChange = { viewModel.dispatch(ThemeSettingsAction.SetUseDynamicColor(it)) }
                        )
                    }
                    item {
                        SwitchWidget(
                            icon = Icons.TwoTone.Colorize,
                            title = stringResource(R.string.theme_settings_dynamic_color_follow_icon),
                            description = stringResource(R.string.theme_settings_dynamic_color_follow_icon_desc),
                            checked = uiState.useDynColorFollowPkgIcon,
                            onCheckedChange = { viewModel.dispatch(ThemeSettingsAction.SetDynColorFollowPkgIcon(it)) }
                        )
                    }
                    // Conditional item for Live Activity
                    item(visible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && uiState.showLiveActivity) {
                        SwitchWidget(
                            icon = Icons.TwoTone.Colorize,
                            title = stringResource(R.string.theme_settings_live_activity_dynamic_color_follow_icon),
                            description = stringResource(R.string.theme_settings_live_activity_dynamic_color_follow_icon_desc),
                            checked = uiState.useDynColorFollowPkgIconForLiveActivity,
                            onCheckedChange = {
                                viewModel.dispatch(
                                    ThemeSettingsAction.SetDynColorFollowPkgIconForLiveActivity(
                                        it
                                    )
                                )
                            }
                        )
                    }
                }
            }

            // --- Group 3: Theme Color (Manual Selection) ---
            item {
                AnimatedVisibility(
                    visible = !uiState.useDynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S,
                    enter = fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)) +
                            expandVertically(animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)) +
                            shrinkVertically(animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing))
                ) {
                    SegmentedColumn(
                        title = stringResource(R.string.theme_settings_theme_color)
                    ) {
                        item {
                            BaseItemContainer {
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
                                                            textStyle = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                                                            textColor = MaterialTheme.colorScheme.onSurface,
                                                            isSelected = uiState.seedColor == rawColor.color &&
                                                                    !(uiState.useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S),
                                                        ) {
                                                            viewModel.dispatch(ThemeSettingsAction.SetSeedColor(rawColor.color))
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
            }

            // --- Group 4: Predictive Back ---
            item {
                SegmentedColumn(
                    title = stringResource(R.string.theme_settings_predictive_back)
                ) {
                    item { PredictiveBackAnimationWidget(uiState) { showPredictiveBackAnimationDialog = true } }
                    item(
                        visible = uiState.predictiveBackAnimation == PredictiveBackAnimation.Scale ||
                                uiState.predictiveBackAnimation == PredictiveBackAnimation.AOSP
                    ) {
                        PredictiveBackAnimationDirectionWidget(uiState) { showPredictiveBackExitDirectionDialog = true }
                    }
                }
            }

            // --- Group 5: Package Icons ---
            item {
                SegmentedColumn(
                    title = stringResource(R.string.theme_settings_package_icons)
                ) {
                    item {
                        SwitchWidget(
                            icon = AppIcons.IconPack,
                            title = stringResource(R.string.theme_settings_prefer_system_icon),
                            description = stringResource(R.string.theme_settings_prefer_system_icon_desc),
                            checked = uiState.preferSystemIcon,
                            onCheckedChange = { viewModel.dispatch(ThemeSettingsAction.ChangePreferSystemIcon(it)) }
                        )
                    }
                }
            }

            // --- Group 6: Launcher Icons ---
            item {
                SegmentedColumn(
                    title = stringResource(R.string.theme_settings_launcher_icons)
                ) {
                    item {
                        SwitchWidget(
                            icon = AppIcons.Launcher,
                            title = stringResource(R.string.theme_settings_hide_launcher_icon),
                            description = stringResource(R.string.theme_settings_hide_launcher_icon_desc),
                            checked = !uiState.showLauncherIcon,
                            onCheckedChange = { newCheckedState ->
                                if (newCheckedState) {
                                    showHideLauncherIconDialog = true
                                } else {
                                    viewModel.dispatch(ThemeSettingsAction.ChangeShowLauncherIcon(true))
                                }
                            }
                        )
                    }
                }
            }

            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}

@Composable
fun PaletteStyleDialog(
    currentStyle: PaletteStyle,
    onDismiss: () -> Unit,
    onSelect: (PaletteStyle) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme_settings_palette_style_desc)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                PaletteStyle.entries.forEach { style ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(style) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (style == currentStyle),
                            onClick = { onSelect(style) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(style.displayName)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
fun PredictiveBackAnimationDialog(
    currentAnimation: PredictiveBackAnimation,
    onDismiss: () -> Unit,
    onSelect: (PredictiveBackAnimation) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme_settings_predictive_back_animation_desc)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                PredictiveBackAnimation.entries.forEach { animation ->
                    val animationText = when (animation) {
                        PredictiveBackAnimation.None -> stringResource(R.string.theme_settings_predictive_back_animation_none)
                        PredictiveBackAnimation.AOSP -> stringResource(R.string.theme_settings_predictive_back_animation_aosp)
                        PredictiveBackAnimation.MIUIX -> stringResource(R.string.theme_settings_predictive_back_animation_miuix)
                        PredictiveBackAnimation.Scale -> stringResource(R.string.theme_settings_predictive_back_animation_scale)
                        PredictiveBackAnimation.Classic -> stringResource(R.string.theme_settings_predictive_back_animation_ksu_classic)
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(animation) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (animation == currentAnimation),
                            onClick = { onSelect(animation) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(animationText)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
fun PredictiveBackExitDirectionDialog(
    currentDirection: PredictiveBackExitDirection,
    onDismiss: () -> Unit,
    onSelect: (PredictiveBackExitDirection) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme_settings_predictive_back_exit_direction_desc)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                PredictiveBackExitDirection.entries.forEach { direction ->
                    val directionText = when (direction) {
                        PredictiveBackExitDirection.FOLLOW_GESTURE -> stringResource(R.string.theme_settings_predictive_back_exit_direction_follow_gesture)
                        PredictiveBackExitDirection.ALWAYS_RIGHT -> stringResource(R.string.theme_settings_predictive_back_exit_direction_always_right)
                        PredictiveBackExitDirection.ALWAYS_LEFT -> stringResource(R.string.theme_settings_predictive_back_exit_direction_always_left)
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(direction) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (direction == currentDirection),
                            onClick = { onSelect(direction) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(directionText)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
fun ThemeModeDialog(
    currentMode: ThemeMode,
    onDismiss: () -> Unit,
    onSelect: (ThemeMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme_settings_theme_mode_desc)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                ThemeMode.entries.forEach { mode ->
                    val modeText = when (mode) {
                        ThemeMode.LIGHT -> stringResource(R.string.theme_settings_theme_mode_light)
                        ThemeMode.DARK -> stringResource(R.string.theme_settings_theme_mode_dark)
                        ThemeMode.SYSTEM -> stringResource(R.string.theme_settings_theme_mode_system)
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(mode) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (mode == currentMode),
                            onClick = { onSelect(mode) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(modeText)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}