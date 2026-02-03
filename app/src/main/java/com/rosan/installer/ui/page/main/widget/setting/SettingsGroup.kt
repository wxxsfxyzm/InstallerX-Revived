package com.rosan.installer.ui.page.main.widget.setting

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.rosan.installer.ui.theme.ConnectionRadius
import com.rosan.installer.ui.theme.CornerRadius

data class SplicedItemData(
    val key: Any?,
    val visible: Boolean,
    val content: @Composable () -> Unit
)

class SplicedGroupScope {
    val items = mutableListOf<SplicedItemData>()

    /**
     * Adds an item to the spliced group.
     * @param key A unique identifier for the item. Crucial for correct animation state during list changes.
     */
    fun item(key: Any? = null, visible: Boolean = true, content: @Composable () -> Unit) {
        items.add(SplicedItemData(key ?: items.size, visible, content))
    }
}

/**
 * A container that groups items with a spliced, continuous look (similar to M3 Expressive).
 *
 * Features:
 * - **Dynamic Shapes**: Top and bottom corners morph smoothly between rounded (outer) and sharp (inner) based on visibility.
 * - **Blinds Animation**: Items expand/collapse vertically without scaling, simulating a shutter/blinds effect.
 * - **Stacking Order**: Items slide over each other cleanly during exit animations.
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

        Column(verticalArrangement = Arrangement.Top) {
            val firstVisibleIndex = allItems.indexOfFirst { it.visible }
            val lastVisibleIndex = allItems.indexOfLast { it.visible }

            // Use a shared stiffness constant for all animations (layout, fade, and shape morphing).
            // This ensures the physics feel connected and synchronized, preventing "ghosting" artifacts
            // where content fades out before the layout collapses.
            val sharedStiffness = Spring.StiffnessMediumLow

            allItems.forEachIndexed { index, itemData ->
                // Using a stable key is mandatory for correct AnimatedVisibility behavior in lists.
                key(itemData.key) {
                    // Z-Index Trick:
                    // We invert the visual stacking order so that items lower in the list render ON TOP of items above them.
                    // This ensures that when an item shrinks upwards (shutter effect), the item below it slides 'over'
                    // the gap rather than underneath, creating a solid, card-stacking feel.
                    val zIndex = allItems.size - index.toFloat()

                    AnimatedVisibility(
                        visible = itemData.visible,
                        modifier = Modifier.zIndex(zIndex),
                        enter = expandVertically(
                            animationSpec = spring(stiffness = sharedStiffness),
                            expandFrom = Alignment.Top // Unroll downwards like a blind
                        ) + fadeIn(
                            animationSpec = spring(stiffness = sharedStiffness)
                        ),
                        exit = shrinkVertically(
                            animationSpec = spring(stiffness = sharedStiffness),
                            shrinkTowards = Alignment.Top // Roll up upwards
                        ) + fadeOut(
                            animationSpec = spring(stiffness = sharedStiffness)
                        )
                    ) {
                        val isFirst = index == firstVisibleIndex
                        val isLast = index == lastVisibleIndex

                        // Determine target corner radii based on current visibility position.
                        // Outer boundaries get full CornerRadius; inner connections get smaller ConnectionRadius.
                        val targetTopRadius = if (isFirst) CornerRadius else ConnectionRadius
                        val targetBottomRadius = if (isLast) CornerRadius else ConnectionRadius

                        // Animate shape changes to match the enter/exit physics.
                        val animatedTopRadius by animateDpAsState(
                            targetValue = targetTopRadius,
                            animationSpec = spring(stiffness = sharedStiffness),
                            label = "TopCornerRadius"
                        )
                        val animatedBottomRadius by animateDpAsState(
                            targetValue = targetBottomRadius,
                            animationSpec = spring(stiffness = sharedStiffness),
                            label = "BottomCornerRadius"
                        )

                        val shape = RoundedCornerShape(
                            topStart = animatedTopRadius,
                            topEnd = animatedTopRadius,
                            bottomStart = animatedBottomRadius,
                            bottomEnd = animatedBottomRadius
                        )

                        // Layout Stability Fix:
                        // Instead of placing spacing/padding at the bottom, we apply it to the TOP for all items except the first.
                        // Since our animation shrinks towards the TOP, bottom-padding would be clipped first, causing
                        // the next item to "jump" instantly. By anchoring spacing to the top, it persists until the
                        // very end of the shrink animation.
                        val topPadding = if (index == 0) 0.dp else 2.dp

                        Column(
                            modifier = Modifier
                                .padding(top = topPadding)
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
}