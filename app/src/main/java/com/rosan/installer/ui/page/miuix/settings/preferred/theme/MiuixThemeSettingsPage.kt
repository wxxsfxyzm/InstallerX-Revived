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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.preferences.PredictiveBackAnimation
import com.rosan.installer.domain.settings.model.preferences.PredictiveBackExitDirection
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.main.settings.preferred.theme.ThemeSettingsAction
import com.rosan.installer.ui.page.main.settings.preferred.theme.ThemeSettingsViewModel
import com.rosan.installer.ui.page.main.widget.card.ColorSwatchPreview
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import com.rosan.installer.ui.page.miuix.widgets.MiuixSwitchWidget
import com.rosan.installer.ui.theme.getMiuixAppBarColor
import com.rosan.installer.ui.theme.installerMiuixBlurEffect
import com.rosan.installer.domain.settings.model.preferences.theme.PaletteStyle
import com.rosan.installer.domain.settings.model.preferences.theme.ThemeColorSpec
import com.rosan.installer.domain.settings.model.preferences.theme.ThemeMode
import com.rosan.installer.ui.theme.rememberMiuixBlurBackdrop
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@SuppressLint("RestrictedApi")
@Composable
fun MiuixThemeSettingsPage(
    viewModel: ThemeSettingsViewModel = koinViewModel()
) {
    val navigator = LocalNavigator.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = MiuixScrollBehavior()
    val transition = LocalNavAnimatedContentScope.current.transition

    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    val topBarBackdrop = rememberMiuixBlurBackdrop(uiState.useBlur)

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.installerMiuixBlurEffect(topBarBackdrop),
                color = topBarBackdrop.getMiuixAppBarColor(),
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
                .then(topBarBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)
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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        MiuixSwitchWidget(
                            title = stringResource(R.string.theme_settings_use_blur),
                            description = stringResource(R.string.theme_settings_use_blur_desc),
                            checked = uiState.useBlur,
                            onCheckedChange = { viewModel.dispatch(ThemeSettingsAction.SetUseBlur(it)) }
                        )
                    }
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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && uiState.showLiveActivity)
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
                                                        isSelected =
                                                            uiState.seedColor == rawColor.color
                                                            && !(uiState.useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S),
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
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
                            visible = uiState.predictiveBackAnimation == PredictiveBackAnimation.Scale ||
                                    uiState.predictiveBackAnimation == PredictiveBackAnimation.AOSP,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            MiuixPredictiveBackExitDirectionWidget(
                                currentDirection = uiState.predictiveBackExitDirection,
                                onDirectionChange = {
                                    transition.setPlaytimeAfterInitialAndTargetStateEstablished(
                                        transition.targetState,
                                        transition.targetState,
                                        transition.playTimeNanos
                                    )
                                    viewModel.dispatch(ThemeSettingsAction.SetPredictiveBackExitDirection(it))
                                }
                            )
                        }
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
            PredictiveBackAnimation.MIUIX -> stringResource(R.string.theme_settings_predictive_back_animation_miuix)
            PredictiveBackAnimation.Scale -> stringResource(R.string.theme_settings_predictive_back_animation_scale)
            PredictiveBackAnimation.Classic -> stringResource(R.string.theme_settings_predictive_back_animation_ksu_classic)
        }
        DropdownItem(title = title)
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
        DropdownItem(title = title)
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

/**
 * Theme Engine selection widget using WindowSpinnerPreference, following the provided pattern.
 * Simplified version without data class and icons.
 *
 * @param currentThemeIsMiuix True if MIUIX theme is selected, false if Google theme is selected.
 * @param onThemeChange Callback when the selection changes. Boolean parameter indicates new selection (true = MIUIX).
 */
@SuppressLint("LocalContextGetResourceValueCall")
@Composable
private fun MiuixThemeEngineWidget(
    modifier: Modifier = Modifier,
    currentThemeIsMiuix: Boolean,
    onThemeChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current

    val themeOptions = remember {
        mapOf(
            true to R.string.theme_settings_miuix_ui, // Key = true -> MIUIX UI string resource
            false to R.string.theme_settings_google_ui // Key = false -> Google UI string resource
        )
    }

    // Convert map entries to List<DropdownItem> for WindowSpinnerPreference.
    // Ensure the order matches the keys: index 0 = true, index 1 = false.
    val spinnerEntries = remember(themeOptions) {
        themeOptions.entries.sortedByDescending { it.key }.map { entry ->
            DropdownItem(
                title = context.getString(entry.value)
            )
        }
    }

    // Determine selected index based on currentThemeIsMiuix state.
    // Index 0 corresponds to true (MIUIX), Index 1 corresponds to false (Google).
    val selectedIndex = remember(currentThemeIsMiuix) {
        if (currentThemeIsMiuix) 0 else 1
    }

    WindowSpinnerPreference(
        modifier = modifier,
        title = stringResource(id = R.string.theme_settings_ui_engine),
        // summary = spinnerEntries[selectedIndex].title,
        items = spinnerEntries,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { newIndex ->
            // Convert index back to boolean key (0 -> true, 1 -> false)
            val newModeIsMiuix = themeOptions.keys.sortedDescending().elementAt(newIndex)
            if (currentThemeIsMiuix != newModeIsMiuix) {
                onThemeChange(newModeIsMiuix)
            }
        }
    )
}

/**
 * A WindowSpinnerPreference widget for selecting the application's theme mode (Light, Dark, or System).
 *
 * @param modifier The modifier to be applied to the WindowSpinnerPreference.
 * @param currentThemeMode The currently selected ThemeMode.
 * @param onThemeModeChange A callback that is invoked when the theme mode selection changes.
 */
@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun MiuixThemeModeWidget(
    modifier: Modifier = Modifier,
    currentThemeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    val context = LocalContext.current

    // Map of ThemeMode enum to its corresponding string resource ID.
    val themeModeOptions = remember {
        // The order in the map definition determines the order in the spinner.
        mapOf(
            ThemeMode.LIGHT to R.string.theme_settings_theme_mode_light,
            ThemeMode.DARK to R.string.theme_settings_theme_mode_dark,
            ThemeMode.SYSTEM to R.string.theme_settings_theme_mode_system
        )
    }

    // Convert the map of options to a list of DropdownItem for the WindowSpinnerPreference component.
    // The order of items in the list is important for index mapping.
    val spinnerEntries = remember(themeModeOptions) {
        themeModeOptions.entries.map { entry ->
            DropdownItem(title = context.getString(entry.value))
        }
    }

    // Calculate the selected index based on the current theme mode.
    // It finds the index of the currentThemeMode in the ordered list of keys.
    val selectedIndex = remember(currentThemeMode, themeModeOptions) {
        themeModeOptions.keys.indexOf(currentThemeMode).coerceAtLeast(0)
    }

    WindowSpinnerPreference(
        modifier = modifier,
        title = stringResource(id = R.string.theme_settings_theme_mode),
        items = spinnerEntries,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { newIndex ->
            // Retrieve the new ThemeMode based on the selected index.
            val newMode = themeModeOptions.keys.elementAt(newIndex)
            // Invoke the callback only if the mode has actually changed.
            if (currentThemeMode != newMode) {
                onThemeModeChange(newMode)
            }
        }
    )
}

/**
 * WindowSpinnerPreference widget for selecting the Palette Style.
 */
@Composable
fun MiuixPaletteStyleWidget(
    modifier: Modifier = Modifier,
    currentPaletteStyle: PaletteStyle,
    onPaletteStyleChange: (PaletteStyle) -> Unit
) {
    val options = remember { PaletteStyle.entries }
    val spinnerEntries = remember(options) {
        options.map { DropdownItem(title = it.displayName) }
    }
    val selectedIndex = remember(currentPaletteStyle, options) {
        options.indexOf(currentPaletteStyle).coerceAtLeast(0)
    }

    WindowSpinnerPreference(
        modifier = modifier,
        title = stringResource(id = R.string.theme_settings_palette_style),
        items = spinnerEntries,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { newIndex ->
            val newStyle = options[newIndex]
            if (currentPaletteStyle != newStyle) {
                onPaletteStyleChange(newStyle)
            }
        }
    )
}

/**
 * WindowSpinnerPreference widget for selecting the Theme Color Spec.
 * Includes fallback logic to gracefully handle styles that do not support SPEC_2025.
 */
@Composable
fun MiuixColorSpecWidget(
    modifier: Modifier = Modifier,
    currentColorSpec: ThemeColorSpec,
    currentPaletteStyle: PaletteStyle,
    onColorSpecChange: (ThemeColorSpec) -> Unit
) {
    // 1. Check if the current PaletteStyle supports SPEC_2025
    val isSpec2025Supported = currentPaletteStyle in listOf(
        PaletteStyle.TonalSpot,
        PaletteStyle.Neutral,
        PaletteStyle.Vibrant,
        PaletteStyle.Expressive
    )

    // 2. Filter available specs based on support
    val availableSpecs = if (isSpec2025Supported) {
        ThemeColorSpec.entries
    } else {
        listOf(ThemeColorSpec.SPEC_2021)
    }

    // 3. Determine the actual spec being applied to match the fallback logic
    val activeSpec = if (!isSpec2025Supported) ThemeColorSpec.SPEC_2021 else currentColorSpec

    // 4. Use a static localized string for the unsupported state
    val descriptionText = if (!isSpec2025Supported) {
        stringResource(id = R.string.theme_settings_color_spec_only_2021)
    } else null

    val spinnerEntries = remember(availableSpecs) {
        availableSpecs.map { DropdownItem(title = it.displayName) }
    }

    val selectedIndex = remember(activeSpec, availableSpecs) {
        availableSpecs.indexOf(activeSpec).coerceAtLeast(0)
    }

    WindowSpinnerPreference(
        modifier = modifier,
        title = stringResource(id = R.string.theme_settings_color_spec),
        summary = descriptionText,
        items = spinnerEntries,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { newIndex ->
            val selectedSpec = availableSpecs[newIndex]
            if (currentColorSpec != selectedSpec) {
                onColorSpecChange(selectedSpec)
            }
        }
    )
}
