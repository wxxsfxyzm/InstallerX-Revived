package top.yukonga.miuix.kmp.basic.patched

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.G2RoundedCornerShape

/**
 * A [ProgressButton] component with Miuix style that can show progress.
 * The button's background acts as a track, and a progress layer fills it
 * from left to right based on the `progress` value.
 *
 * This implementation is structurally identical to the standard [top.yukonga.miuix.kmp.basic.Button] to avoid layout issues,
 * and uses a custom draw modifier to render the progress visually.
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
 * @param content The [Composable] content of the [ProgressButton].
 */
@Composable
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
    content: @Composable RowScope.() -> Unit
) {
    // Coerce progress value to be between 0.0 and 1.0
    val progressCoerced = progress.coerceIn(0f, 1f)

    val shape = remember(cornerRadius) { G2RoundedCornerShape(cornerRadius) }

    // Select colors based on the enabled state
    val trackColor = if (enabled) colors.trackColor else colors.disabledTrackColor
    val progressColor = if (enabled) colors.progressColor else colors.disabledProgressColor
    val contentColor = if (enabled) colors.contentColor else colors.disabledContentColor

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.semantics { role = Role.Button },
        shape = shape,
        color = trackColor,
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minWidth = minWidth, minHeight = minHeight)
                .drawWithContent {
                    val cornerRadiusPx = cornerRadius.toPx()
                    val minProgressWidth = cornerRadiusPx * 2
                    val progressWidth = minProgressWidth + (size.width - minProgressWidth) * progressCoerced

                    drawRoundRect(
                        color = progressColor,
                        size = Size(width = progressWidth, height = size.height),
                        cornerRadius = CornerRadius(cornerRadiusPx)
                    )

                    drawContent()
                }
                .padding(insideMargin),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Provide the correct content color to children composables.
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                content()
            }
        }
    }
}

object ProgressButtonDefaults {
    /**
     * The [ProgressButtonColors] for progress buttons.
     */
    @Composable
    fun progressButtonColors(
        trackColor: Color = MiuixTheme.colorScheme.secondaryVariant,
        progressColor: Color = MiuixTheme.colorScheme.primary,
        contentColor: Color = MiuixTheme.colorScheme.onPrimary,
        disabledTrackColor: Color = MiuixTheme.colorScheme.disabledSecondaryVariant,
        disabledProgressColor: Color = MiuixTheme.colorScheme.disabledPrimaryButton,
        disabledContentColor: Color = MiuixTheme.colorScheme.disabledOnPrimaryButton
    ): ProgressButtonColors = ProgressButtonColors(
        trackColor = trackColor,
        progressColor = progressColor,
        contentColor = contentColor,
        disabledTrackColor = disabledTrackColor,
        disabledProgressColor = disabledProgressColor,
        disabledContentColor = disabledContentColor
    )
}

@Immutable
class ProgressButtonColors(
    val trackColor: Color,
    val progressColor: Color,
    val contentColor: Color,
    val disabledTrackColor: Color,
    val disabledProgressColor: Color,
    val disabledContentColor: Color
)