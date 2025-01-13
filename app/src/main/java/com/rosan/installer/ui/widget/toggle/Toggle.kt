package com.rosan.installer.ui.widget.toggle

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun Toggle(
    selected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selectedContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    unselectedContentColor: Color = LocalContentColor.current,
    content: @Composable ColumnScope.() -> Unit
) {
    @Suppress("AnimateAsStateLabel") val contentColor by animateColorAsState(
        if (selected) selectedContentColor
        else unselectedContentColor
    )
    CompositionLocalProvider(
        LocalContentColor provides contentColor,
        LocalTextStyle provides MaterialTheme.typography.labelLarge
    ) {
        Column(
            modifier = modifier
                .selectable(
                    selected = selected,
                    onClick = onSelected,
                    enabled = enabled,
                    interactionSource = remember {
                        MutableInteractionSource()
                    },
                    indication = ripple(color = contentColor)
                )
                .padding(vertical = 8.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = content
        )
    }
}
