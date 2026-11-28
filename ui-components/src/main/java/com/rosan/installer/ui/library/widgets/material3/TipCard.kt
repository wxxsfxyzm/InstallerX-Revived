package com.rosan.installer.ui.library.widgets.material3

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * A general-purpose card for displaying tips or important information.
 *
 * @param modifier The modifier to be applied to the card.
 * @param tipContent The composable slot for the main tip message.
 * This is typically a Row with an Icon and Text.
 * @param actionContent The composable slot for action buttons, typically aligned to the end.
 * It's within a ColumnScope, so modifiers like `align` are allowed.
 * @param noPadding Don't add more padding in internal
 */
@Composable
fun TipCard(
    modifier: Modifier = Modifier,
    cardShape: RoundedCornerShape = RoundedCornerShape(16.dp),
    noPadding: Boolean = false,
    tipContent: @Composable () -> Unit,
    actionContent: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = if (noPadding) {
            modifier
                .fillMaxWidth()
        } else {
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f),
        ),
        shape = cardShape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 0.dp),
        ) {
            tipContent()
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = actionContent,
            )
        }
    }
}

@Composable
fun InfoTipCard(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, noPadding: Boolean = false) {
    TipCard(
        modifier = modifier,
        noPadding = noPadding,
        tipContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiary,
                    )
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMediumEmphasized,
                    color = MaterialTheme.colorScheme.onTertiary,
                )
            }
        },
    ) {
        Spacer(modifier = Modifier.size(16.dp))
    }
}

@Composable
fun TitleTipCard(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    noPadding: Boolean = false,
) {
    TipCard(
        modifier = modifier,
        noPadding = noPadding,
        tipContent = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    icon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiary,
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiary,
                    )
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiary,
                )
            }
        },
    ) {
        Spacer(modifier = Modifier.size(16.dp))
    }
}
