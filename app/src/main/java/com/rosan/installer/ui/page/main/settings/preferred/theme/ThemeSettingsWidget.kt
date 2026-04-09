// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.PredictiveBackAnimation
import com.rosan.installer.domain.settings.model.PredictiveBackExitDirection
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.widget.setting.BaseWidget

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
            PredictiveBackAnimation.Scale -> stringResource(R.string.theme_settings_predictive_back_animation_scale)
            PredictiveBackAnimation.KernelSUClassic -> stringResource(R.string.theme_settings_predictive_back_animation_ksu_classic)
            PredictiveBackAnimation.KernelSUOfficial -> stringResource(R.string.theme_settings_predictive_back_animation_ksu_official)
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