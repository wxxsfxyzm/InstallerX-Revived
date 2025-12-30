package com.rosan.installer.ui.page.miuix.widgets

// Copyright 2025, miuix-kotlin-multiplatform contributors
// SPDX-License-Identifier: Apache-2.0

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.SuperListPopup
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowUpDown
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * A standalone dropdown menu styled to look and feel like a SmallTitle.
 * It functions as a selector that occupies the space of a title.
 *
 * @param items The list of options for the dropdown menu.
 * @param selectedIndex The index of the currently selected item.
 * @param onSelectedIndexChange A callback invoked when a new item is selected.
 * @param modifier The modifier to be applied to the entire component.
 * @param enabled Controls the enabled state of the dropdown.
 * @param placeholder The text to display when the items list is empty.
 */
@Composable
fun MiuixDropdown(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "No Selection"
) {
    // State to manage whether the dropdown is expanded or not.
    val isDropdownExpanded = remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current

    val itemsNotEmpty = items.isNotEmpty()
    val actualEnabled = enabled && itemsNotEmpty

    // Box to anchor the popup menu.
    Box(modifier = modifier) {
        // The visible, clickable part of the component.
        Row(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = actualEnabled,
                    onClick = {
                        isDropdownExpanded.value = !isDropdownExpanded.value
                        if (isDropdownExpanded.value) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                        }
                    }
                )
                // Use the same padding as SmallTitle for consistent layout.
                .padding(horizontal = 28.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Display the selected item's text, styled like SmallTitle.
            val textColor = if (actualEnabled) {
                MiuixTheme.colorScheme.onBackgroundVariant
            } else {
                MiuixTheme.colorScheme.disabledOnSecondaryVariant
            }
            Text(
                text = if (itemsNotEmpty) items[selectedIndex] else placeholder,
                fontSize = MiuixTheme.textStyles.subtitle.fontSize, // from SmallTitle
                fontWeight = FontWeight.Bold, // from SmallTitle
                color = textColor
            )

            // The dropdown arrow icon.
            val iconColor = if (actualEnabled) {
                MiuixTheme.colorScheme.onSurfaceVariantActions
            } else {
                MiuixTheme.colorScheme.disabledOnSecondaryVariant
            }
            Image(
                modifier = Modifier.size(10.dp, 16.dp),
                imageVector = MiuixIcons.Basic.ArrowUpDown,
                colorFilter = ColorFilter.tint(iconColor),
                contentDescription = "Toggle Dropdown"
            )
        }

        // The popup menu itself, shown when isDropdownExpanded is true.
        SuperListPopup(
            show = isDropdownExpanded,
            alignment = PopupPositionProvider.Align.Start,
            popupPositionProvider = DropdownWithStartMarginProvider,
            onDismissRequest = { isDropdownExpanded.value = false }
        ) {
            ListPopupColumn {
                items.forEachIndexed { index, text ->
                    DropdownImpl(
                        text = text,
                        optionSize = items.size,
                        isSelected = selectedIndex == index,
                        index = index,
                        onSelectedIndexChange = { newIndex ->
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                            onSelectedIndexChange(newIndex)
                            isDropdownExpanded.value = false // Close dropdown after selection
                        }
                    )
                }
            }
        }
    }
}

/**
 * A custom PopupPositionProvider that is identical to the default dropdown provider,
 * but adds a horizontal margin.
 */
private val DropdownWithStartMarginProvider = object : PopupPositionProvider {
    // This positioning logic is copied directly from ListPopupDefaults.DropdownPositionProvider
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowBounds: IntRect,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
        popupMargin: IntRect,
        alignment: PopupPositionProvider.Align
    ): IntOffset {
        val offsetX = if (alignment == PopupPositionProvider.Align.End) {
            anchorBounds.right - popupContentSize.width - popupMargin.right
        } else {
            anchorBounds.left + popupMargin.left
        }
        val offsetY = if (windowBounds.bottom - anchorBounds.bottom > popupContentSize.height) {
            // Show below
            anchorBounds.bottom + popupMargin.bottom
        } else if (anchorBounds.top - windowBounds.top > popupContentSize.height) {
            // Show above
            anchorBounds.top - popupContentSize.height - popupMargin.top
        } else {
            // Middle
            anchorBounds.top + anchorBounds.height / 2 - popupContentSize.height / 2
        }
        return IntOffset(
            x = offsetX.coerceIn(
                windowBounds.left,
                (windowBounds.right - popupContentSize.width - popupMargin.right).coerceAtLeast(windowBounds.left)
            ),
            y = offsetY.coerceIn(
                (windowBounds.top + popupMargin.top).coerceAtMost(windowBounds.bottom - popupContentSize.height - popupMargin.bottom),
                windowBounds.bottom - popupContentSize.height - popupMargin.bottom
            )
        )
    }

    // Provide a 12.dp horizontal margin.
    override fun getMargins(): PaddingValues {
        return PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    }
}