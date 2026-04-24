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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
 * A generic draggable list widget using [SegmentedListItem].
 * Handles drag-and-drop reordering logic and consistent styling.
 *
 * @param T The type of items in the list.
 * @param modifier The modifier to be applied to the list.
 * @param items The list of items to display.
 * @param itemKey A function to provide a unique key for each item.
 * @param itemName A function to get the display name of an item.
 * @param itemDescription A function to get the display description of an item.
 * @param leadingIcon The icon to display at the start of each item.
 * @param onMove Callback invoked when an item is moved to a new position.
 * @param onRemove Callback invoked when an item is removed.
 * @param noContentTitle Title to show when the list is empty.
 * @param noContentDescription Description to show when the list is empty.
 * @param bottomBarContent Content to display below the list (e.g., an "Add" button).
 */
@Composable
fun <T> DraggableManagedList(
    modifier: Modifier = Modifier,
    items: List<T>,
    itemKey: (T) -> Any,
    itemName: (T) -> String,
    itemDescription: (T) -> String,
    leadingIcon: ImageVector,
    onMove: (from: Int, to: Int) -> Unit,
    onRemove: (T) -> Unit,
    noContentTitle: String,
    noContentDescription: String = stringResource(R.string.config_add_one_to_get_started),
    bottomBarContent: @Composable () -> Unit = {}
) {
    var showDeleteConfirmation by remember { mutableStateOf<T?>(null) }
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
                iconColor = MaterialTheme.colorScheme.primary,
                content = {
                    IconButton(onClick = { showDeleteConfirmation = item }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            elevation = ListItemDefaults.elevation(0.dp, 6.dp),
                            interactionSource = interactionSource,
                            modifier = Modifier
                                .onGloballyPositioned { if (itemHeightPx == 0f) itemHeightPx = it.size.height.toFloat() }
                                .zIndex(if (isDragged || finalTranslationY != 0f) 10f else 0f)
                                .graphicsLayer { this.translationY = finalTranslationY },
                            content = { Text(itemName(item)) },
                            supportingContent = { Text(itemDescription(item)) },
                            leadingContent = { Icon(leadingIcon, null, tint = MaterialTheme.colorScheme.primary) },
                            trailingContent = {
                                IconButton(onClick = { showDeleteConfirmation = item }) {
                                    Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Bottom Bar (Add button, info text, etc.)
        Box(modifier = Modifier.fillMaxWidth()) {
            bottomBarContent()
        }
    }

    // Common Delete Dialog
    showDeleteConfirmation?.let { item ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = { Text(stringResource(R.string.config_confirm_deletion)) },
            text = { Text(stringResource(R.string.config_confirm_deletion_desc, itemName(item))) },
            confirmButton = {
                TextButton(onClick = {
                    onRemove(item)
                    showDeleteConfirmation = null
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
