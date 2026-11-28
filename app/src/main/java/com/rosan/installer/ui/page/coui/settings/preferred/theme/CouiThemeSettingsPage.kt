// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.settings.preferred.theme

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
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.itos.coui.layout.Divider
import com.itos.coui.layout.Group
import com.itos.coui.layout.Scaffold
import com.itos.coui.slider.Slider
import com.itos.coui.theme.CouiTheme
import com.itos.coui.topappbar.CouiTopAppBarDefaults
import com.itos.coui.topappbar.LargeTopAppBar
import kotlin.math.roundToInt
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.preferences.PredictiveBackAnimation
import com.rosan.installer.domain.settings.model.preferences.PredictiveBackExitDirection
import com.rosan.installer.domain.settings.model.preferences.theme.PaletteStyle
import com.rosan.installer.domain.settings.model.preferences.theme.ThemeColorSpec
import com.rosan.installer.domain.settings.model.preferences.theme.ThemeMode
import com.rosan.installer.domain.settings.model.preferences.theme.UiStyle
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.coui.widgets.CouiBackButton
import com.rosan.installer.ui.page.coui.widgets.CouiSpinnerItem
import com.rosan.installer.ui.page.coui.widgets.CouiSwitchItem
import com.rosan.installer.ui.page.main.settings.preferred.theme.ThemeSettingsAction
import com.rosan.installer.ui.page.main.settings.preferred.theme.ThemeSettingsViewModel
import com.rosan.installer.ui.page.main.widget.card.ColorSwatchPreview
import com.rosan.installer.ui.theme.material.PresetColors
import org.koin.androidx.compose.koinViewModel

@Composable
fun CouiThemeSettingsPage(viewModel: ThemeSettingsViewModel = koinViewModel()) {
    val navigator = LocalNavigator.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scrollBehavior = CouiTopAppBarDefaults.exitUntilCollapsedScrollBehavior {
        listState.canScrollForward || listState.canScrollBackward
    }
    val density = LocalDensity.current

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CouiTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(R.string.theme_settings)) },
                navigationIcon = {
                    CouiBackButton(onClick = { navigator.pop() })
                },
                scrollBehavior = scrollBehavior,
                useCouiBlur = uiState.useBlur,
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
                Group(title = stringResource(R.string.theme_settings_ui_style)) {
                    CouiThemeEngineWidget(
                        currentUiStyle = uiState.uiStyle,
                        onThemeChange = { style ->
                            viewModel.dispatch(ThemeSettingsAction.SetUiStyle(style))
                        },
                    )
                }
            }

            item {
                Group(title = stringResource(R.string.theme_settings_coui_ui)) {
                    CouiThemeModeWidget(
                        currentThemeMode = uiState.themeMode,
                        onThemeModeChange = { newMode ->
                            viewModel.dispatch(ThemeSettingsAction.SetThemeMode(newMode))
                        },
                    )
                    Divider()
                    CouiSwitchItem(
                        title = stringResource(R.string.theme_settings_use_amoled),
                        description = stringResource(R.string.theme_settings_use_amoled_desc),
                        checked = uiState.useAmoled,
                        onCheckedChange = { viewModel.dispatch(ThemeSettingsAction.SetUseAmoled(it)) },
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Divider()
                        CouiSwitchItem(
                            title = stringResource(R.string.theme_settings_use_blur),
                            description = stringResource(R.string.theme_settings_use_blur_desc),
                            checked = uiState.useBlur,
                            onCheckedChange = { viewModel.dispatch(ThemeSettingsAction.SetUseBlur(it)) },
                        )
                        AnimatedVisibility(
                            visible = uiState.useBlur,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            val blurOffLabel = stringResource(R.string.theme_settings_coui_blur_off)
                            Column {
                                Divider()
                                CouiSliderPreferenceItem(
                                    title = stringResource(R.string.theme_settings_coui_blur_radius),
                                    description = stringResource(R.string.theme_settings_coui_blur_radius_desc),
                                    value = uiState.couiBlurRadius,
                                    valueRange = 0..200,
                                    valueLabel = { v -> if (v <= 0) blurOffLabel else "$v" },
                                    onValueCommit = { radius ->
                                        viewModel.dispatch(ThemeSettingsAction.SetCouiBlurRadius(radius))
                                    },
                                )
                                Divider()
                                CouiSliderPreferenceItem(
                                    title = stringResource(R.string.theme_settings_coui_glass_opacity),
                                    description = stringResource(R.string.theme_settings_coui_glass_opacity_desc),
                                    value = uiState.couiGlassOpacity,
                                    valueRange = 40..100,
                                    valueLabel = { v -> "$v%" },
                                    onValueCommit = { opacity ->
                                        viewModel.dispatch(ThemeSettingsAction.SetCouiGlassOpacity(opacity))
                                    },
                                )
                            }
                        }
                        Divider()
                        CouiSwitchItem(
                            title = stringResource(R.string.theme_settings_coui_contour_glow),
                            description = stringResource(R.string.theme_settings_coui_contour_glow_desc),
                            checked = uiState.useCouiContourGlow,
                            onCheckedChange = { viewModel.dispatch(ThemeSettingsAction.SetUseCouiContourGlow(it)) },
                        )
                    }
                    Divider()
                    CouiSwitchItem(
                        title = stringResource(R.string.theme_settings_coui_custom_colors),
                        description = stringResource(R.string.theme_settings_coui_custom_colors_desc),
                        checked = uiState.useCouiMonet,
                        onCheckedChange = { viewModel.dispatch(ThemeSettingsAction.SetUseCouiMonet(it)) },
                    )
                    AnimatedVisibility(
                        visible = uiState.useCouiMonet,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Column {
                            Divider()
                            CouiSwitchItem(
                                title = stringResource(R.string.theme_settings_dynamic_color),
                                description = stringResource(R.string.theme_settings_dynamic_color_desc),
                                checked = uiState.useDynamicColor,
                                onCheckedChange = { viewModel.dispatch(ThemeSettingsAction.SetUseDynamicColor(it)) },
                            )
                            Divider()
                            CouiPaletteStyleWidget(
                                currentPaletteStyle = uiState.paletteStyle,
                                onPaletteStyleChange = { newStyle ->
                                    viewModel.dispatch(ThemeSettingsAction.SetPaletteStyle(newStyle))
                                },
                            )
                            Divider()
                            CouiColorSpecWidget(
                                currentColorSpec = uiState.colorSpec,
                                currentPaletteStyle = uiState.paletteStyle,
                                onColorSpecChange = { newSpec ->
                                    viewModel.dispatch(ThemeSettingsAction.SetColorSpec(newSpec))
                                },
                            )
                            Divider()
                            CouiSwitchItem(
                                title = stringResource(R.string.theme_settings_dynamic_color_follow_icon),
                                description = stringResource(R.string.theme_settings_dynamic_color_follow_icon_desc),
                                checked = uiState.useDynColorFollowPkgIcon,
                                onCheckedChange = {
                                    viewModel.dispatch(ThemeSettingsAction.SetDynColorFollowPkgIcon(it))
                                },
                            )
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && uiState.showLiveActivity) {
                        Divider()
                        CouiSwitchItem(
                            title = stringResource(R.string.theme_settings_live_activity_dynamic_color_follow_icon),
                            description = stringResource(R.string.theme_settings_live_activity_dynamic_color_follow_icon_desc),
                            checked = uiState.useDynColorFollowPkgIconForLiveActivity,
                            onCheckedChange = {
                                viewModel.dispatch(ThemeSettingsAction.SetDynColorFollowPkgIconForLiveActivity(it))
                            },
                        )
                    }
                }
            }

            item {
                AnimatedVisibility(
                    visible = uiState.useCouiMonet && !uiState.useDynamicColor,
                    enter = fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)) +
                        expandVertically(animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)) +
                        shrinkVertically(animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)),
                ) {
                    Group(title = stringResource(R.string.theme_settings_theme_color)) {
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 16.dp),
                        ) {
                            val itemMinWidth = 88.dp
                            val columns = (this.maxWidth / itemMinWidth).toInt().coerceAtLeast(1)
                            val chunkedColors = uiState.availableColors.chunked(columns)

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                chunkedColors.forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                    ) {
                                        rowItems.forEach { rawColor ->
                                            Box(
                                                modifier = Modifier.weight(1f),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                ColorSwatchPreview(
                                                    rawColor = rawColor,
                                                    currentStyle = uiState.paletteStyle,
                                                    colorSpec = uiState.colorSpec,
                                                    textStyle = MaterialTheme.typography.labelSmall,
                                                    textColor = CouiTheme.colorScheme.onSurface,
                                                    isSelected = uiState.seedColor == rawColor.color,
                                                ) {
                                                    viewModel.dispatch(
                                                        ThemeSettingsAction.SetSeedColor(
                                                            rawColor.color,
                                                            uiState.useDynamicColor &&
                                                                Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
                                                                uiState.availableColors != PresetColors,
                                                        ),
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                item {
                    Group(title = stringResource(R.string.theme_settings_predictive_back)) {
                        CouiPredictiveBackAnimationWidget(
                            currentAnimation = uiState.predictiveBackAnimation,
                            onAnimationChange = { newAnim ->
                                viewModel.dispatch(ThemeSettingsAction.SetPredictiveBackAnimation(newAnim))
                            },
                        )
                        AnimatedVisibility(
                            visible = uiState.predictiveBackAnimation == PredictiveBackAnimation.Scale,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            Column {
                                Divider()
                                CouiPredictiveBackExitDirectionWidget(
                                    currentDirection = uiState.predictiveBackExitDirection,
                                    onDirectionChange = {
                                        viewModel.dispatch(ThemeSettingsAction.SetPredictiveBackExitDirection(it))
                                    },
                                )
                            }
                        }
                    }
                }
            }

            item {
                Group(title = stringResource(R.string.theme_settings_package_icons)) {
                    CouiSwitchItem(
                        title = stringResource(R.string.theme_settings_prefer_system_icon),
                        description = stringResource(R.string.theme_settings_prefer_system_icon_desc),
                        checked = uiState.preferSystemIcon,
                        onCheckedChange = {
                            viewModel.dispatch(ThemeSettingsAction.ChangePreferSystemIcon(it))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CouiSliderPreferenceItem(
    title: String,
    description: String,
    value: Int,
    valueRange: IntRange,
    valueLabel: (Int) -> String,
    onValueCommit: (Int) -> Unit,
) {
    var sliderValue by remember { mutableFloatStateOf(value.toFloat()) }
    LaunchedEffect(value) {
        sliderValue = value.toFloat()
    }
    val displayInt = sliderValue.roundToInt().coerceIn(valueRange.first, valueRange.last)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                color = CouiTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = valueLabel(displayInt),
                color = CouiTheme.colorScheme.controlAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Text(
            text = description,
            color = CouiTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp),
        )
        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = sliderValue,
            onValueChange = { sliderValue = it },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            onValueChangeFinished = {
                onValueCommit(sliderValue.roundToInt().coerceIn(valueRange.first, valueRange.last))
            },
        )
    }
}

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
private fun CouiThemeEngineWidget(
    currentUiStyle: UiStyle,
    onThemeChange: (UiStyle) -> Unit,
) {
    val context = LocalContext.current
    val themeOptions = remember {
        listOf(
            UiStyle.COUI to R.string.theme_settings_coui_ui,
            UiStyle.MIUIX to R.string.theme_settings_miuix_ui,
            UiStyle.MATERIAL3 to R.string.theme_settings_google_ui,
        )
    }
    val labels = remember(themeOptions) {
        themeOptions.map { (_, resId) -> context.getString(resId) }
    }
    val selectedIndex = remember(currentUiStyle, themeOptions) {
        themeOptions.indexOfFirst { it.first == currentUiStyle }.coerceAtLeast(0)
    }

    CouiSpinnerItem(
        title = stringResource(id = R.string.theme_settings_ui_engine),
        items = labels,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { newIndex ->
            val newStyle = themeOptions[newIndex].first
            if (currentUiStyle != newStyle) {
                onThemeChange(newStyle)
            }
        },
    )
}

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
private fun CouiThemeModeWidget(
    currentThemeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    val context = LocalContext.current
    val options = remember {
        listOf(
            ThemeMode.LIGHT to R.string.theme_settings_theme_mode_light,
            ThemeMode.DARK to R.string.theme_settings_theme_mode_dark,
            ThemeMode.SYSTEM to R.string.theme_settings_theme_mode_system,
        )
    }
    val labels = remember(options) {
        options.map { (_, resId) -> context.getString(resId) }
    }
    val selectedIndex = remember(currentThemeMode, options) {
        options.indexOfFirst { it.first == currentThemeMode }.coerceAtLeast(0)
    }

    CouiSpinnerItem(
        title = stringResource(id = R.string.theme_settings_theme_mode),
        items = labels,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { newIndex ->
            val newMode = options[newIndex].first
            if (currentThemeMode != newMode) {
                onThemeModeChange(newMode)
            }
        },
    )
}

@Composable
private fun CouiPaletteStyleWidget(
    currentPaletteStyle: PaletteStyle,
    onPaletteStyleChange: (PaletteStyle) -> Unit,
) {
    val options = remember { PaletteStyle.entries }
    val labels = remember(options) { options.map { it.displayName } }
    val selectedIndex = remember(currentPaletteStyle, options) {
        options.indexOf(currentPaletteStyle).coerceAtLeast(0)
    }

    CouiSpinnerItem(
        title = stringResource(id = R.string.theme_settings_palette_style),
        items = labels,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { newIndex ->
            val newStyle = options[newIndex]
            if (currentPaletteStyle != newStyle) {
                onPaletteStyleChange(newStyle)
            }
        },
    )
}

@Composable
private fun CouiColorSpecWidget(
    currentColorSpec: ThemeColorSpec,
    currentPaletteStyle: PaletteStyle,
    onColorSpecChange: (ThemeColorSpec) -> Unit,
) {
    val availableSpecs = currentPaletteStyle.supportedSpecs
    val activeSpec = currentPaletteStyle.effectiveSpec(currentColorSpec)
    val descriptionText = if (availableSpecs.size == 1) {
        stringResource(
            id = when (activeSpec) {
                ThemeColorSpec.SPEC_2026 -> R.string.theme_settings_color_spec_only_2026
                else -> R.string.theme_settings_color_spec_only_2021
            },
        )
    } else {
        null
    }
    val labels = remember(availableSpecs) { availableSpecs.map { it.displayName } }
    val selectedIndex = remember(activeSpec, availableSpecs) {
        availableSpecs.indexOf(activeSpec).coerceAtLeast(0)
    }

    CouiSpinnerItem(
        title = stringResource(id = R.string.theme_settings_color_spec),
        summary = descriptionText,
        enabled = availableSpecs.size > 1,
        items = labels,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { newIndex ->
            val selectedSpec = availableSpecs[newIndex]
            if (currentColorSpec != selectedSpec) {
                onColorSpecChange(selectedSpec)
            }
        },
    )
}

@Composable
private fun CouiPredictiveBackAnimationWidget(
    currentAnimation: PredictiveBackAnimation,
    onAnimationChange: (PredictiveBackAnimation) -> Unit,
) {
    val options = remember { PredictiveBackAnimation.entries }
    val labels = options.map { anim ->
        when (anim) {
            PredictiveBackAnimation.None -> stringResource(R.string.theme_settings_predictive_back_animation_none)
            PredictiveBackAnimation.AOSP -> stringResource(R.string.theme_settings_predictive_back_animation_aosp)
            PredictiveBackAnimation.MIUIX -> stringResource(R.string.theme_settings_predictive_back_animation_miuix)
            PredictiveBackAnimation.Scale -> stringResource(R.string.theme_settings_predictive_back_animation_scale)
            PredictiveBackAnimation.Classic -> stringResource(R.string.theme_settings_predictive_back_animation_ksu_classic)
        }
    }
    val selectedIndex = remember(currentAnimation, options) {
        options.indexOf(currentAnimation).coerceAtLeast(0)
    }

    CouiSpinnerItem(
        title = stringResource(id = R.string.theme_settings_predictive_back_animation),
        items = labels,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { newIndex ->
            val newAnim = options[newIndex]
            if (currentAnimation != newAnim) {
                onAnimationChange(newAnim)
            }
        },
    )
}

@Composable
private fun CouiPredictiveBackExitDirectionWidget(
    currentDirection: PredictiveBackExitDirection,
    onDirectionChange: (PredictiveBackExitDirection) -> Unit,
) {
    val options = remember { PredictiveBackExitDirection.entries }
    val labels = options.map { dir ->
        when (dir) {
            PredictiveBackExitDirection.FOLLOW_GESTURE -> stringResource(R.string.theme_settings_predictive_back_exit_direction_follow_gesture)
            PredictiveBackExitDirection.ALWAYS_RIGHT -> stringResource(R.string.theme_settings_predictive_back_exit_direction_always_right)
            PredictiveBackExitDirection.ALWAYS_LEFT -> stringResource(R.string.theme_settings_predictive_back_exit_direction_always_left)
        }
    }
    val selectedIndex = remember(currentDirection, options) {
        options.indexOf(currentDirection).coerceAtLeast(0)
    }

    CouiSpinnerItem(
        title = stringResource(id = R.string.theme_settings_predictive_back_exit_direction),
        items = labels,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { newIndex ->
            val newDir = options[newIndex]
            if (currentDirection != newDir) {
                onDirectionChange(newDir)
            }
        },
    )
}
