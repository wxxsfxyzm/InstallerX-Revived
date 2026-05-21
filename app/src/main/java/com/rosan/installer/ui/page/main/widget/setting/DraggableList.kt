// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.rosan.installer.ui.page.main.widget.setting

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.rosan.installer.R

/**
 * A draggable list widget that supports item reordering.
 *
 * When the list is empty, an empty-state [BaseWidget] is shown.
 * When the list contains a single item, it is rendered as a [BaseWidget].
 * When the list contains multiple items, they are rendered as segmented list items
 * and can be reordered by long-press dragging.
 *
 * @param T The type of items in the list.
 * @param modifier The modifier to be applied to the list.
 * @param items The list of items to display.
 * @param itemKey A function that provides a unique key for each item.
 * @param itemName A function that returns the display name of an item.
 * @param itemDescription A function that returns the display description of an item.
 * @param leadingIcon The icon displayed at the start of each item.
 * @param onMove Callback invoked when an item is moved to a new position.
 * @param noContentTitle Title shown when the list is empty.
 * @param noContentDescription Optional description shown when the list is empty.
 * @param trailingContent Optional content displayed at the end of each item.
 * @param bottomBarContent Optional content displayed below the list.
 */
@Composable
fun <T> DraggableList(
    modifier: Modifier = Modifier,
    items: List<T>,
    itemKey: (T) -> Any,
    itemName: (T) -> String,
    itemDescription: (T) -> String,
    leadingIcon: ImageVector? = null,
    onMove: (from: Int, to: Int) -> Unit,
    noContentTitle: String,
    noContentDescription: String? = stringResource(R.string.config_add_one_to_get_started),
    trailingContent: (@Composable (item: T) -> Unit)? = null,
    bottomBarContent: (@Composable () -> Unit)? = null
) {
    var isWaitingForUpdate by remember { mutableStateOf(false) }
    var localItems by remember { mutableStateOf(items) }

    // Sync local state with external items
    LaunchedEffect(items) {
        if (isWaitingForUpdate) {
            isWaitingForUpdate = false
        } else {
            localItems = items
        }
    }

    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var itemHeightPx by remember { mutableFloatStateOf(0f) }

    val targetIndex by remember(itemHeightPx, localItems.size) {
        derivedStateOf {
            if (draggedIndex == null || itemHeightPx <= 0f) null
            else {
                val shift = (dragOffsetY / itemHeightPx + if (dragOffsetY > 0) 0.5f else -0.5f).toInt()
                (draggedIndex!! + shift).coerceIn(0, localItems.lastIndex)
            }
        }
    }

    Column(modifier = modifier.padding(bottom = 8.dp)) {
        if (items.isEmpty()) {
            BaseWidget(
                modifier = Modifier.clip(MaterialTheme.shapes.large),
                title = noContentTitle,
                description = noContentDescription,
                icon = Icons.Default.Info
            )
        } else if (localItems.size == 1) {
            val item = localItems.first()
            BaseWidget(
                modifier = Modifier.clip(MaterialTheme.shapes.large),
                title = itemName(item),
                description = itemDescription(item),
                icon = leadingIcon,
                iconPlaceholder = leadingIcon != null,
                iconColor = MaterialTheme.colorScheme.primary,
                trailingContent = { trailingContent?.invoke(item) }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(if (draggedIndex != null) 1f else 0f)
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                if (itemHeightPx > 0) {
                                    val index = (offset.y / itemHeightPx).toInt()
                                    if (index in localItems.indices) draggedIndex = index
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetY += dragAmount.y
                            },
                            onDragEnd = {
                                if (draggedIndex != null && targetIndex != null && draggedIndex != targetIndex) {
                                    val from = draggedIndex!!
                                    val to = targetIndex!!
                                    val newList = localItems.toMutableList()
                                    val item = newList.removeAt(from)
                                    newList.add(to, item)
                                    localItems = newList
                                    isWaitingForUpdate = true
                                    onMove(from, to)
                                }
                                draggedIndex = null
                                dragOffsetY = 0f
                            },
                            onDragCancel = {
                                draggedIndex = null
                                dragOffsetY = 0f
                            }
                        )
                    },
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
            ) {
                localItems.forEachIndexed { index, item ->
                    key(itemKey(item)) {
                        val isDragged = index == draggedIndex
                        val interactionSource = remember { MutableInteractionSource() }

                        var dragInteraction by remember { mutableStateOf<DragInteraction.Start?>(null) }
                        LaunchedEffect(isDragged) {
                            if (isDragged) {
                                // When a drag is detected, notify InteractionSource to enter the "dragging" state.
                                val interaction = DragInteraction.Start()
                                interactionSource.emit(interaction)
                                dragInteraction = interaction
                            } else {
                                // Drag ended, notify InteractionSource to stop the state.
                                dragInteraction?.let {
                                    interactionSource.emit(DragInteraction.Stop(it))
                                    dragInteraction = null
                                }
                            }
                        }

                        val translationY = if (isDragged) dragOffsetY
                        else if (draggedIndex != null && targetIndex != null) {
                            if (draggedIndex!! < targetIndex!! && index in (draggedIndex!! + 1)..targetIndex!!) -itemHeightPx
                            else if (draggedIndex!! > targetIndex!! && index in targetIndex!!..<draggedIndex!!) itemHeightPx
                            else 0f
                        } else 0f

                        val animatedTranslationY by animateFloatAsState(targetValue = translationY, label = "drag_anim")
                        val finalTranslationY = if (isDragged) dragOffsetY else if (draggedIndex == null) 0f else animatedTranslationY

                        SegmentedListItem(
                            onClick = { },
                            shapes = ListItemDefaults.segmentedShapes(index, localItems.size),
                            colors = ListItemDefaults.segmentedColors(
                                containerColor = MaterialTheme.colorScheme.surfaceBright,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                leadingContentColor = MaterialTheme.colorScheme.primary
                            ),
                            elevation = ListItemDefaults.elevation(0.dp, 6.dp),
                            interactionSource = interactionSource,
                            modifier = Modifier
                                .onGloballyPositioned { if (itemHeightPx == 0f) itemHeightPx = it.size.height.toFloat() }
                                .zIndex(if (isDragged || finalTranslationY != 0f) 10f else 0f)
                                .graphicsLayer { this.translationY = finalTranslationY },
                            content = { Text(itemName(item)) },
                            supportingContent = { Text(itemDescription(item)) },
                            leadingContent = leadingIcon?.let { icon -> { Icon(icon, null) } },
                            trailingContent = { trailingContent?.invoke(item) }
                        )
                    }
                }
            }
        }

        // Bottom Bar (Add button, info text, etc.)
        bottomBarContent?.let { content ->
            Box(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}
