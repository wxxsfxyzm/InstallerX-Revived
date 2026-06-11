// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.widgets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.theme.miuixSheetCardColors
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun <T> MiuixDraggableList(
    items: List<T>,
    itemKey: (T) -> Any,
    itemName: (T) -> String,
    itemDescription: (T) -> String?,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    onItemClick: ((T) -> Unit)? = null,
    cardColors: CardColors? = null,
    startAction: @Composable (() -> Unit)? = {
        Icon(
            imageVector = ImageVector.vectorResource(AppIcons.DragHandle),
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onSurfaceVariantActions
        )
    },
    trailingContent: @Composable RowScope.(item: T) -> Unit
) {
    val resolvedCardColors = cardColors ?: miuixSheetCardColors()
    var isWaitingForUpdate by remember { mutableStateOf(false) }
    var localItems by remember { mutableStateOf(items) }

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
    val itemGapPx = with(LocalDensity.current) { 10.dp.toPx() }

    val itemStepPx by remember(itemHeightPx, itemGapPx) {
        derivedStateOf {
            if (itemHeightPx > 0f) itemHeightPx + itemGapPx else 0f
        }
    }

    val targetIndex by remember(itemStepPx, localItems.size) {
        derivedStateOf {
            if (draggedIndex == null || itemStepPx <= 0f) null
            else {
                val shift = (dragOffsetY / itemStepPx + if (dragOffsetY > 0) 0.5f else -0.5f).toInt()
                (draggedIndex!! + shift).coerceIn(0, localItems.lastIndex)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (draggedIndex != null) 1f else 0f)
            .pointerInput(localItems.size, itemStepPx) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        if (itemStepPx > 0) {
                            val index = (offset.y / itemStepPx).toInt()
                            if (index in localItems.indices) draggedIndex = index
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetY += dragAmount.y
                    },
                    onDragEnd = {
                        val from = draggedIndex
                        val to = targetIndex
                        if (from != null && to != null && from != to) {
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
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        localItems.forEachIndexed { index, item ->
            key(itemKey(item)) {
                val isDragged = index == draggedIndex
                val translationY = if (isDragged) dragOffsetY
                else if (draggedIndex != null && targetIndex != null) {
                    if (draggedIndex!! < targetIndex!! && index in (draggedIndex!! + 1)..targetIndex!!) {
                        -itemStepPx
                    } else if (draggedIndex!! > targetIndex!! && index in targetIndex!!..<draggedIndex!!) {
                        itemStepPx
                    } else {
                        0f
                    }
                } else {
                    0f
                }

                val animatedTranslationY by animateFloatAsState(
                    targetValue = translationY,
                    label = "miuix_drag_anim"
                )
                val finalTranslationY = if (isDragged) dragOffsetY
                else if (draggedIndex == null) 0f
                else animatedTranslationY

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned {
                            if (itemHeightPx == 0f) itemHeightPx = it.size.height.toFloat()
                        }
                        .zIndex(if (isDragged || finalTranslationY != 0f) 10f else 0f)
                        .graphicsLayer { this.translationY = finalTranslationY },
                    colors = resolvedCardColors
                ) {
                    BasicComponent(
                        title = itemName(item),
                        summary = itemDescription(item),
                        startAction = startAction,
                        endActions = { trailingContent(item) },
                        holdDownState = isDragged,
                        onClick = { onItemClick?.invoke(item) }
                    )
                }
            }
        }
    }
}
