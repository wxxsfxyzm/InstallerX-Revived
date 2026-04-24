// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.rosan.installer.ui.page.main.widget.setting

import android.os.Build
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.rosan.installer.ui.theme.ConnectionRadius
import com.rosan.installer.ui.theme.CornerRadius
import kotlin.math.roundToInt

private const val PADDING_HORIZONTAL = 16
private const val PADDING_VERTICAL = 8

private const val bouncyStiffness = 800f
private const val bouncyDamping = 0.5f

/**
 * A [CompositionLocal] that provides the dynamically calculated [Shape] for items
 * inside a [SegmentedColumn]. Defaults to a rounded corner shape with [CornerRadius].
 */
val LocalSegmentedItemShape = compositionLocalOf<Shape> { RoundedCornerShape(CornerRadius) }

@DslMarker
annotation class SegmentedColumnDsl

/**
 * Represents the configuration and content of an individual item within a [SegmentedColumn].
 *
 * @property key A unique identifier for the item, utilized for optimized composition and state tracking.
 * @property visible Determines the visibility state of the item. Changes to this state are animated.
 * @property customTopPadding Optional custom padding applied to the top of this specific item.
 * @property forceFlatTop If `true`, overrides the default corner rounding and forces the top corner radius to 0.dp.
 * @property forceFlatBottom If `true`, overrides the default corner rounding and forces the bottom corner radius to 0.dp.
 * @property content The composable payload of the item, which receives the dynamically calculated [Shape].
 */
@Immutable
data class SegmentedItemData(
    val key: Any?,
    val visible: Boolean,
    val customTopPadding: Dp? = null,
    val forceFlatTop: Boolean = false,
    val forceFlatBottom: Boolean = false,
    val content: @Composable (Shape) -> Unit
)

/**
 * A DSL scope used to define the items and their layout behaviors within a [SegmentedColumn].
 */
@SegmentedColumnDsl
class SegmentedColumnScope {
    val items = mutableListOf<SegmentedItemData>()

    /**
     * Registers a standard item within the group.
     *
     * @param key A unique identifier for the item. Defaults to the current index.
     * @param visible The visibility state of the item.
     * @param topPadding Optional explicit top padding for this item.
     * @param forceFlatTop Disables top corner rounding if `true`.
     * @param forceFlatBottom Disables bottom corner rounding if `true`.
     * @param content The composable content representing the UI of this item.
     */
    fun item(
        key: Any? = null,
        visible: Boolean = true,
        topPadding: Dp? = null,
        forceFlatTop: Boolean = false,
        forceFlatBottom: Boolean = false,
        content: @Composable (Shape) -> Unit
    ) {
        items.add(SegmentedItemData(key ?: items.size, visible, topPadding, forceFlatTop, forceFlatBottom, content))
    }

    /**
     * Registers an expandable item pairing a header with conditionally visible body content.
     *
     * This method orchestrates the dynamic corner radius flattening between the header and the body
     * at the engine level. When expanded, the seam between the two components seamlessly loses its
     * internal rounded corners to appear as a single unified container.
     *
     * @param visible Whether the entire expandable structural unit should be rendered.
     * @param expanded Whether the body content is currently expanded (visible) or collapsed.
     * @param topPadding Optional explicit top padding applied to the header item.
     * @param bottomPadding Spacing applied above the body content, effectively acting as bottom padding for the expanded visual block.
     * @param topContent The composable representing the persistently visible header.
     * @param bottomContent The composable representing the expansible body content.
     */
    fun expandableItem(
        visible: Boolean = true,
        expanded: Boolean,
        topPadding: Dp? = null,
        bottomPadding: Dp = 1.dp,
        topContent: @Composable (Shape) -> Unit,
        bottomContent: @Composable (Shape) -> Unit
    ) {
        // Header component: When expanded, forcefully flatten the bottom corner
        // to establish a seamless connection with the expanding body below.
        item(
            visible = visible,
            topPadding = topPadding,
            forceFlatBottom = expanded,
            content = topContent
        )

        // Body component: Regardless of its visibility trigger, forcefully flatten
        // the top corner to sit flush against the header component above.
        item(
            visible = visible && expanded,
            topPadding = bottomPadding,
            forceFlatTop = true,
            content = bottomContent
        )
    }
}

/**
 * A highly customized vertical layout group that visually splices multiple composable items together.
 *
 * This layout dynamically calculates and animates the corner radii of its children to maintain
 * a unified rounded appearance for the outermost edges of the group, while adjusting interior
 * connections. It leverages a custom [Layout] to efficiently measure, place, and animate
 * entering/exiting children.
 *
 * @param modifier The modifier to be applied to the group container.
 * @param title An optional title string displayed above the group of items.
 * @param contentPadding The padding applied to the group container.
 * @param content A lambda providing a [SegmentedColumnScope] to declare the children.
 */
@Composable
fun SegmentedColumn(
    modifier: Modifier = Modifier,
    title: String = "",
    contentPadding: PaddingValues = PaddingValues(horizontal = PADDING_HORIZONTAL.dp, vertical = PADDING_VERTICAL.dp),
    content: SegmentedColumnScope.() -> Unit
) {
    val scope = SegmentedColumnScope().apply(content)
    val allItems = scope.items

    if (allItems.isEmpty()) return

    Column(modifier = modifier.padding(contentPadding)) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = PADDING_HORIZONTAL.dp, top = PADDING_VERTICAL.dp, bottom = 16.dp)
            )
        }

        val floatSpring = spring<Float>(dampingRatio = bouncyDamping, stiffness = bouncyStiffness)
        val dpSpring = spring<Dp>(dampingRatio = bouncyDamping, stiffness = bouncyStiffness)

        val progresses = allItems.map { item ->
            animateFloatAsState(
                targetValue = if (item.visible) 1f else 0f,
                animationSpec = floatSpring,
                label = "progress"
            )
        }

        val firstVisibleIndex = allItems.indexOfFirst { it.visible }
        val lastVisibleIndex = allItems.indexOfLast { it.visible }

        Layout(
            content = {
                allItems.forEachIndexed { index, itemData ->
                    key(itemData.key ?: index) {
                        val isFirst = index == firstVisibleIndex || (index == 0 && !itemData.visible)
                        val isLast = index == lastVisibleIndex || (index == allItems.lastIndex && !itemData.visible)

                        // 1. Establish the foundational corner radius based on the item's positional boundary.
                        val baseTopRadius = if (isFirst) CornerRadius else ConnectionRadius
                        val baseBottomRadius = if (isLast) CornerRadius else ConnectionRadius

                        // 2. Incorporate structural overrides. Flatten boundaries where 'forceFlat' flags dictate.
                        val targetTopRadius = if (itemData.forceFlatTop) 0.dp else baseTopRadius
                        val targetBottomRadius = if (itemData.forceFlatBottom) 0.dp else baseBottomRadius

                        // Dynamic corner animation is only supported on Android 13 (Tiramisu) and above.
                        val isDynamicDpSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

                        // 3. Drive the corner transition animations. Provides a fluid shift between
                        // rounded and flat states during expand/collapse interactions.
                        val currentTopRadius = if (isDynamicDpSupported) {
                            animateDpAsState(targetTopRadius, dpSpring, label = "TopRadius").value
                        } else targetTopRadius

                        val currentBottomRadius = if (isDynamicDpSupported) {
                            animateDpAsState(targetBottomRadius, dpSpring, label = "BottomRadius").value
                        } else targetBottomRadius

                        val shape = RoundedCornerShape(
                            topStart = currentTopRadius,
                            topEnd = currentTopRadius,
                            bottomStart = currentBottomRadius,
                            bottomEnd = currentBottomRadius
                        )

                        val targetTopPadding = itemData.customTopPadding ?: (if (isFirst) 0.dp else ListItemDefaults.SegmentedGap)
                        val currentTopPadding = if (isDynamicDpSupported) {
                            animateDpAsState(targetTopPadding, dpSpring, label = "TopPadding").value
                        } else targetTopPadding

                        Box(
                            modifier = Modifier
                                .zIndex(if (itemData.visible) (allItems.size - index).toFloat() else -index.toFloat())
                                .graphicsLayer {
                                    // Crucial performance fix: Defer state reading into the drawing phase.
                                    // This prevents the animation progression from causing frame-by-frame recompositions.
                                    val currentProgress = progresses[index].value
                                    val safeProgress = currentProgress.coerceAtLeast(0f)

                                    clip = true
                                    this.shape = object : Shape {
                                        override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
                                            return Outline.Rectangle(Rect(0f, 0f, size.width, size.height * safeProgress))
                                        }
                                    }
                                    alpha = (currentProgress * 1.5f).coerceIn(0f, 1f)
                                }
                        ) {
                            CompositionLocalProvider(LocalSegmentedItemShape provides shape) {
                                Column(modifier = Modifier.padding(top = currentTopPadding)) {
                                    itemData.content(shape)
                                }
                            }
                        }
                    }
                }
            }
        ) { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            var currentY = 0f
            val positions = mutableListOf<Int>()

            // Calculate exact placement coordinates dynamically corresponding to the 
            // current phase of the visibility animations.
            placeables.forEachIndexed { index, placeable ->
                positions.add(currentY.roundToInt())
                // Reading state during the measurement phase is perfectly fine and safe.
                val progress = progresses[index].value
                currentY += placeable.height * progress
            }

            layout(constraints.maxWidth, currentY.roundToInt().coerceAtLeast(0)) {
                placeables.forEachIndexed { index, placeable ->
                    placeable.placeRelative(x = 0, y = positions[index])
                }
            }
        }
    }
}
