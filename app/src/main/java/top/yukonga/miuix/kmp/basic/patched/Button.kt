// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.basic.patched

import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * A [ProgressButton] component with Miuix style that can show progress.
 * The button's background acts as a track, and a progress layer fills it
 * from left to right based on the `progress` value.
 *
 * @param progress The current progress value between 0.0f and 1.0f.
 * @param onClick The callback when the [ProgressButton] is clicked.
 * @param modifier The modifier to be applied to the [ProgressButton].
 * @param enabled Whether the [ProgressButton] is enabled.
 * @param cornerRadius The corner radius of the [ProgressButton].
 * @param minWidth The minimum width of the [ProgressButton].
 * @param minHeight The minimum height of the [ProgressButton].
 * @param colors The [ProgressButtonColors] of the [ProgressButton].
 * @param insideMargin The margin inside the [ProgressButton].
 * @param interactionSource The [MutableInteractionSource] to be used for the [ProgressButton].
 * @param indication The [Indication] to be used for the [ProgressButton].
 * @param content The [Composable] content of the [ProgressButton].
 */
@Composable
@NonRestartableComposable
fun ProgressButton(
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cornerRadius: Dp = ButtonDefaults.CornerRadius,
    minWidth: Dp = ButtonDefaults.MinWidth,
    minHeight: Dp = ButtonDefaults.MinHeight,
    colors: ProgressButtonColors = ProgressButtonDefaults.progressButtonColors(),
    insideMargin: PaddingValues = ButtonDefaults.InsideMargin,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = LocalIndication.current,
    content: @Composable RowScope.() -> Unit
) {
    // Coerce progress value to be between 0.0 and 1.0
    val progressCoerced = progress.coerceIn(0f, 1f)

    val shape = RoundedCornerShape(cornerRadius)

    val trackColor = if (enabled) colors.trackColor else colors.disabledTrackColor
    val progressColor = if (enabled) colors.progressColor else colors.disabledProgressColor
    val contentColor = if (enabled) colors.contentColor else colors.disabledContentColor

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.semantics { role = Role.Button },
        shape = shape,
        color = trackColor,
        contentColor = contentColor, // Let Surface handle the ContentColor delegation
        interactionSource = interactionSource,
        indication = indication,
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minWidth = minWidth, minHeight = minHeight)
                .drawWithContent {
                    // Only draw the progress indicator if progress is greater than 0.
                    if (progressCoerced > 0f) {
                        drawRect(
                            color = progressColor,
                            size = Size(width = size.width * progressCoerced, height = size.height)
                        )
                    }
                    drawContent()
                }
                .padding(insideMargin),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

object ProgressButtonDefaults {
    /**
     * The default [ProgressButtonColors] for progress buttons.
     */
    @Composable
    fun progressButtonColors(
        trackColor: Color = MiuixTheme.colorScheme.secondaryVariant,
        progressColor: Color = MiuixTheme.colorScheme.primary,
        contentColor: Color = MiuixTheme.colorScheme.onPrimary,
        disabledTrackColor: Color = MiuixTheme.colorScheme.disabledSecondaryVariant,
        disabledProgressColor: Color = MiuixTheme.colorScheme.disabledPrimaryButton,
        disabledContentColor: Color = MiuixTheme.colorScheme.disabledOnPrimaryButton
    ): ProgressButtonColors = remember(
        trackColor, progressColor, contentColor,
        disabledTrackColor, disabledProgressColor, disabledContentColor
    ) { // Sync with the standard Button: Use remember for caching instances
        ProgressButtonColors(
            trackColor = trackColor,
            progressColor = progressColor,
            contentColor = contentColor,
            disabledTrackColor = disabledTrackColor,
            disabledProgressColor = disabledProgressColor,
            disabledContentColor = disabledContentColor
        )
    }
}

// Sync with the standard Button: Must be a data class to ensure correct recomposition skipping
@Immutable
data class ProgressButtonColors(
    val trackColor: Color,
    val progressColor: Color,
    val contentColor: Color,
    val disabledTrackColor: Color,
    val disabledProgressColor: Color,
    val disabledContentColor: Color
)
