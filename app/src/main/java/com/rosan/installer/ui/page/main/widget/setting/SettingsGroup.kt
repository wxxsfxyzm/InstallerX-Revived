package com.rosan.installer.ui.page.main.widget.setting

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import com.rosan.installer.ui.theme.bottomShape
import com.rosan.installer.ui.theme.middleShape
import com.rosan.installer.ui.theme.singleShape
import com.rosan.installer.ui.theme.topShape

data class SplicedItemData(
    val visible: Boolean,
    val content: @Composable () -> Unit
)

/**
 * Scope for the spliced group DSL, similar to LazyListScope.
 */
class SplicedGroupScope {
    val items = mutableListOf<SplicedItemData>()

    fun item(visible: Boolean = true, content: @Composable () -> Unit) {
        items.add(SplicedItemData(visible, content))
    }
}

/**
 * A container that groups items with a spliced, continuous look (M3 Expressive).
 * Automatically handles corner shapes based on item visibility.
 */
@Composable
fun SplicedColumnGroup(
    modifier: Modifier = Modifier,
    title: String = "",
    content: SplicedGroupScope.() -> Unit
) {
    val scope = remember { SplicedGroupScope() }
    scope.items.clear()
    scope.content()

    val allItems = scope.items

    if (allItems.isEmpty()) return

    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Find effective start/end indices ignoring hidden items to apply correct corner shapes
            val firstVisibleIndex = allItems.indexOfFirst { it.visible }
            val lastVisibleIndex = allItems.indexOfLast { it.visible }

            allItems.forEachIndexed { index, itemData ->
                AnimatedVisibility(
                    visible = itemData.visible,
                    enter = expandVertically(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        expandFrom = Alignment.Top
                    ) + fadeIn(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    ) + scaleIn(
                        initialScale = 0.9f,
                        transformOrigin = TransformOrigin(0.5f, 0f),
                        animationSpec = spring(stiffness = Spring.StiffnessMedium)
                    ),
                    exit = shrinkVertically(
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        shrinkTowards = Alignment.Top
                    ) + scaleOut(
                        targetScale = 0.9f,
                        transformOrigin = TransformOrigin(0.5f, 0f),
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    )
                ) {
                    // Dynamically determine shape based on visual position
                    val shape = when {
                        index == firstVisibleIndex && index == lastVisibleIndex -> singleShape
                        index == firstVisibleIndex -> topShape
                        index == lastVisibleIndex -> bottomShape
                        else -> middleShape
                    }

                    Column(
                        modifier = Modifier
                            .clip(shape)
                            .background(MaterialTheme.colorScheme.surfaceBright)
                    ) {
                        itemData.content()
                    }
                }
            }
        }
    }
}