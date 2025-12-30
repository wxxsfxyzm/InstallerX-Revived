package com.rosan.installer.ui.page.main.installer.dialog.inner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rosan.installer.build.RsConfig
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.util.help

val pausingIcon: @Composable () -> Unit = {
    Icon(
        imageVector = AppIcons.Pausing, contentDescription = null
    )
}

val workingIcon: @Composable () -> Unit = {
    Icon(
        imageVector = AppIcons.Working, contentDescription = null
    )
}

val menuIcon: @Composable () -> Unit = {
    Icon(
        imageVector = AppIcons.MenuOpen, contentDescription = null, modifier = Modifier.size(32.dp)
    )
}

val permissionIcon: @Composable () -> Unit = {
    Icon(
        imageVector = AppIcons.Permission,
        contentDescription = null,
        modifier = Modifier.size(32.dp)
    )
}

/**
 * A composable that displays an error message in a collapsible block.
 * It also provides a slot for suggestions to be shown when the block is not expanded.
 *
 * @param error The throwable error to display.
 * @param modifier Modifier for the root Column.
 * @param suggestions A composable lambda for displaying suggestion chips below the error block.
 */
@Composable
fun ErrorTextBlock(
    error: Throwable,
    modifier: Modifier = Modifier,
    suggestions: @Composable () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(targetValue = if (expanded) -180f else 0f, label = "errorRotation")

    Column(modifier = modifier.fillMaxWidth()) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onErrorContainer) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        expanded = !expanded
                    }
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = AppIcons.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .rotate(rotation)
                    )
                    Text(
                        text = error.help(),
                        fontWeight = FontWeight.Bold,
                        maxLines = Int.MAX_VALUE
                    )
                }
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
                ) {
                    val textToShow = if (RsConfig.isDebug) {
                        error.stackTraceToString()
                    } else {
                        error.message ?: "An unknown error occurred."
                    }.trim()
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        BasicTextField(
                            value = textToShow,
                            onValueChange = {},
                            readOnly = true,
                            textStyle = LocalTextStyle.current.copy(color = LocalContentColor.current)
                        )
                    }
                }
            }
        }
        // Suggestions are only visible when the error text is NOT expanded.
        AnimatedVisibility(
            visible = !expanded,
            enter = fadeIn(animationSpec = tween(delayMillis = 150)) + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            suggestions()
        }
    }
}

/**
 * A composable that displays a list of warnings in a styled block.
 * The style is similar to ErrorTextBlock but without title or icons.
 *
 * @param warnings A list of pairs, where each pair contains the text string and the specific Color for that text.
 * @param modifier Modifier for the root Column.
 */
@Composable
fun WarningTextBlock(
    warnings: List<Pair<String, Color>>,
    modifier: Modifier = Modifier,
) {
    // Only display the block if there are warnings to show.
    if (warnings.isNotEmpty()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(12.dp)
        ) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onErrorContainer) {
                warnings.forEachIndexed { index, (text, color) ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(
                        text = text,
                        color = if (color == Color.Unspecified) LocalContentColor.current else color,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}