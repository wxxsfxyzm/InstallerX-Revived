package com.rosan.installer.ui.page.main.widget.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.rosan.installer.ui.theme.bottomShape
import com.rosan.installer.ui.theme.middleShape
import com.rosan.installer.ui.theme.singleShape
import com.rosan.installer.ui.theme.topShape

/**
 * Settings Group Container
 *
 * @param title the title of the group
 * @param content a list of composable that will be displayed in the group
 * @param modifier Modifier
 */
@Composable
fun SplicedColumnGroup(
    modifier: Modifier = Modifier,
    title: String = "",
    content: List<@Composable () -> Unit>,
) {
    if (content.isEmpty()) return

    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        // Group title
        if (title != "")
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )

        // The container for setting items.
        Column(
            modifier = Modifier.clip(
                // Clip the whole column to ensure content stays within the rounded bounds.
                singleShape
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            content.forEachIndexed { index, itemContent ->
                // Determine the shape based on the pkg's position.
                val shape = when {
                    content.size == 1 -> singleShape
                    index == 0 -> topShape
                    index == content.size - 1 -> bottomShape
                    else -> middleShape
                }

                // Apply background with the correct shape to the pkg.
                Column(
                    modifier = Modifier
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surfaceBright)
                ) {
                    itemContent()
                }
            }
        }
    }
}