// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.DesignServices
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.PredictiveBackAnimation
import com.rosan.installer.domain.settings.model.PredictiveBackExitDirection
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.widget.setting.BaseWidget
import com.rosan.installer.ui.page.main.widget.setting.DropDownMenuWidget
import com.rosan.installer.ui.theme.material.PaletteStyle
import com.rosan.installer.ui.theme.material.ThemeColorSpec

@Composable
fun ColorSpecSelector(viewModel: ThemeSettingsViewModel) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    // Check if the current PaletteStyle supports SPEC_2025
    val isSpec2025Supported = uiState.paletteStyle in listOf(
        PaletteStyle.TonalSpot,
        PaletteStyle.Neutral,
        PaletteStyle.Vibrant,
        PaletteStyle.Expressive
    )

    // Filter available specs based on support
    val availableSpecs = if (isSpec2025Supported) {
        ThemeColorSpec.entries
    } else {
        listOf(ThemeColorSpec.SPEC_2021)
    }

    // Determine the actual spec being applied to match the fallback logic
    val activeSpec = if (!isSpec2025Supported) ThemeColorSpec.SPEC_2021 else uiState.colorSpec

    // Use a static localized string for the unsupported state
    val descriptionText = if (!isSpec2025Supported) {
        stringResource(id = R.string.theme_settings_color_spec_only_2021)
    } else {
        activeSpec.displayName
    }

    DropDownMenuWidget(
        icon = Icons.TwoTone.DesignServices,
        title = stringResource(id = R.string.theme_settings_color_spec),
        description = descriptionText,
        enabled = isSpec2025Supported, // Disable interaction if not supported
        choice = availableSpecs.indexOf(activeSpec).coerceAtLeast(0),
        data = availableSpecs.map { it.displayName },
        onChoiceChange = { index ->
            val selectedSpec = availableSpecs[index]
            viewModel.dispatch(ThemeSettingsAction.SetColorSpec(selectedSpec))
        }
    )
}

@Composable
fun PredictiveBackAnimationWidget(
    uiState: ThemeSettingsState,
    onClick: () -> Unit
) {
    BaseWidget(
        icon = AppIcons.PredictiveBack,
        title = stringResource(R.string.theme_settings_predictive_back_animation),
        description = when (uiState.predictiveBackAnimation) {
            PredictiveBackAnimation.None -> stringResource(R.string.theme_settings_predictive_back_animation_none)
            PredictiveBackAnimation.AOSP -> stringResource(R.string.theme_settings_predictive_back_animation_aosp)
            PredictiveBackAnimation.MIUIX -> stringResource(R.string.theme_settings_predictive_back_animation_miuix)
            PredictiveBackAnimation.Scale -> stringResource(R.string.theme_settings_predictive_back_animation_scale)
            PredictiveBackAnimation.Classic -> stringResource(R.string.theme_settings_predictive_back_animation_ksu_classic)
        },
        onClick = onClick
    ) {}
}

@Composable
fun PredictiveBackAnimationDirectionWidget(
    uiState: ThemeSettingsState,
    onClick: () -> Unit
) {
    BaseWidget(
        icon = AppIcons.PredictiveBackDirection,
        title = stringResource(R.string.theme_settings_predictive_back_exit_direction),
        description = when (uiState.predictiveBackExitDirection) {
            PredictiveBackExitDirection.FOLLOW_GESTURE -> stringResource(R.string.theme_settings_predictive_back_exit_direction_follow_gesture)
            PredictiveBackExitDirection.ALWAYS_RIGHT -> stringResource(R.string.theme_settings_predictive_back_exit_direction_always_right)
            PredictiveBackExitDirection.ALWAYS_LEFT -> stringResource(R.string.theme_settings_predictive_back_exit_direction_always_left)
        },
        onClick = onClick
    ) {}
}