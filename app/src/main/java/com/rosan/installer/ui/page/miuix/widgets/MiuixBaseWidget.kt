package com.rosan.installer.ui.page.miuix.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * A base widget styled after MIUIX components.
 *
 * This component arranges content in a row, typically with optional content at the start,
 * a main title and summary section, and optional content at the end. It retains the
 * core functionalities like error state, overlay content, and haptic feedback from the original design.
 *
 * @param title The main text to be displayed.
 * @param summary Optional description text displayed below the title.
 * @param startContent Optional composable to be placed at the beginning of the widget.
 * @param endContent Optional composable to be placed at the end of the widget.
 * @param foreContent Optional composable that overlays the title and summary section.
 * @param enabled Controls the enabled state of the component. When false, it becomes non-clickable and visually disabled.
 * @param isError If true, the summary text will be displayed in the error color.
 * @param onClick The callback to be invoked when this widget is clicked.
 */
@Composable
fun MiuixBaseWidget(
    title: String,
    summary: String? = null,
    startContent: @Composable (() -> Unit)? = null,
    endContent: @Composable (RowScope.() -> Unit)? = null,
    foreContent: @Composable (BoxScope.() -> Unit) = {},
    enabled: Boolean = true,
    isError: Boolean = false,
    onClick: () -> Unit = {},
) {
    // Set alpha for disabled state to apply to all children
    val contentAlpha = if (enabled) 1f else 0.38f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClick = { onClick() }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Provide the alpha to all child composables
        CompositionLocalProvider(LocalContentColor provides LocalContentColor.current.copy(alpha = contentAlpha)) {
            // Start Content (e.g., Icon)
            if (startContent != null) {
                Box(modifier = Modifier.padding(end = 16.dp)) {
                    startContent()
                }
            }

            // Middle Content (Title and Summary)
            Box(
                modifier = Modifier.weight(1f)
            ) {
                Column {
                    // Title
                    Text(
                        text = title,
                        color = MiuixTheme.colorScheme.onSurface,
                        style = MiuixTheme.textStyles.title2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.alpha(contentAlpha) // Apply alpha directly for clarity
                    )

                    // Summary
                    summary?.let {
                        Spacer(modifier = Modifier.height(2.dp))
                        val summaryColor = when {
                            isError -> MiuixTheme.colorScheme.disabledPrimary
                            else -> MiuixTheme.colorScheme.onSurfaceVariantActions
                        }
                        Text(
                            text = it,
                            color = summaryColor,
                            style = MiuixTheme.textStyles.body2,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.alpha(contentAlpha) // Apply alpha directly
                        )
                    }
                }
                // Foreground content that overlays title and summary
                foreContent()
            }

            // End Content (e.g., Checkbox, Switch, Arrow)
            if (endContent != null) {
                Spacer(Modifier.width(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    endContent()
                }
            }
        }
    }
}