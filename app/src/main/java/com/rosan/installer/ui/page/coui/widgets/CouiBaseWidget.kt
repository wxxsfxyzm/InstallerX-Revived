// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itos.coui.checkbox.Checkbox
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.icons.CouiIcons
import com.itos.coui.input.CouiHapticFeedback
import com.itos.coui.input.performCouiHapticFeedback
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.layout.Item
import com.itos.coui.menu.CouiMenuItem
import com.itos.coui.menu.Menu
import com.itos.coui.menu.couiMenuAnchor
import com.itos.coui.menu.rememberCouiMenuAnchor
import com.itos.coui.radiobutton.RadioButton
import com.itos.coui.switch.Switch
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme

/**
 * True when rendered inside a translucent frosted-glass COUI floating dialog surface,
 * allowing nested cards and secondary buttons to adapt their translucency natively.
 */
val LocalCouiGlassSurface = compositionLocalOf { false }

@Composable
fun CouiCard(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    shape: Shape = CouiPageDefaults.shape,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isGlass = LocalCouiGlassSurface.current
    val resolvedColor = when {
        color.isSpecified -> color
        isGlass -> if (CouiTheme.isDark) Color(0x14FFFFFF) else Color(0xA6FFFFFF)
        else -> CouiTheme.colorScheme.card
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(resolvedColor),
        content = content,
    )
}

@Composable
fun CouiBasicItem(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = CouiTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    val pageColors = CouiPageDefaults.themeColors()
    Item(
        modifier = modifier,
        icon = icon?.let { vec ->
            {
                Icon(
                    imageVector = vec,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = if (enabled) iconTint else pageColors.disabled,
                )
            }
        },
        summary = summary?.takeIf { it.isNotEmpty() }?.let { desc ->
            {
                Text(
                    text = desc,
                    kind = CouiTextKind.ItemSummary,
                    color = if (enabled) pageColors.summary else pageColors.disabled,
                )
            }
        },
        trailing = trailing,
        onClick = onClick,
        enabled = enabled,
    ) {
        Text(
            text = title,
            kind = CouiTextKind.ItemTitle,
            color = if (enabled) pageColors.title else pageColors.disabled,
        )
    }
}

@Composable
fun CouiNavigationItem(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    valueText: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val pageColors = CouiPageDefaults.themeColors()
    CouiBasicItem(
        title = title,
        modifier = modifier,
        summary = description,
        icon = icon,
        enabled = enabled,
        onClick = onClick,
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (!valueText.isNullOrEmpty()) {
                    Text(
                        text = valueText,
                        kind = CouiTextKind.ItemValue,
                        color = if (enabled) pageColors.summary else pageColors.disabled,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = CouiIcons.Filled.Next,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (enabled) pageColors.summary else pageColors.disabled,
                )
            }
        },
    )
}

@Composable
fun CouiSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val dark = CouiTheme.isDark
    val pageColors = CouiPageDefaults.themeColors()
    val view = LocalView.current
    val source = remember { MutableInteractionSource() }

    Item(
        modifier = modifier
            .couiButtonFeedback(dark = dark, interactionSource = source, enabled = enabled, list = true)
            .toggleable(
                value = checked,
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Switch,
            ) { target ->
                view.performCouiHapticFeedback(CouiHapticFeedback.Switch)
                onCheckedChange(target)
            },
        icon = icon?.let { vec ->
            {
                Icon(
                    imageVector = vec,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = if (enabled) pageColors.title else pageColors.disabled,
                )
            }
        },
        summary = description?.takeIf { it.isNotEmpty() }?.let { desc ->
            {
                Text(
                    text = desc,
                    kind = CouiTextKind.ItemSummary,
                    color = if (enabled) pageColors.summary else pageColors.disabled,
                )
            }
        },
        trailing = {
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = null,
                onDragCheckedChange = onCheckedChange,
                interactionSource = source,
                enabled = enabled,
            )
        },
    ) {
        Text(
            text = title,
            kind = CouiTextKind.ItemTitle,
            color = if (enabled) pageColors.title else pageColors.disabled,
        )
    }
}

@Composable
fun CouiCheckboxItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val dark = CouiTheme.isDark
    val pageColors = CouiPageDefaults.themeColors()
    val source = remember { MutableInteractionSource() }

    Item(
        modifier = modifier
            .couiButtonFeedback(dark = dark, interactionSource = source, enabled = enabled, list = true)
            .toggleable(
                value = checked,
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            ),
        icon = icon?.let { vec ->
            {
                Icon(
                    imageVector = vec,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = if (enabled) pageColors.title else pageColors.disabled,
                )
            }
        },
        summary = description?.takeIf { it.isNotEmpty() }?.let { desc ->
            {
                Text(
                    text = desc,
                    kind = CouiTextKind.ItemSummary,
                    color = if (enabled) pageColors.summary else pageColors.disabled,
                )
            }
        },
        trailing = {
            Spacer(modifier = Modifier.width(12.dp))
            Checkbox(
                checked = checked,
                enabled = enabled,
            )
        },
    ) {
        Text(
            text = title,
            kind = CouiTextKind.ItemTitle,
            color = if (enabled) pageColors.title else pageColors.disabled,
        )
    }
}

@Composable
fun CouiRadioItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
) {
    val dark = CouiTheme.isDark
    val pageColors = CouiPageDefaults.themeColors()
    val source = remember { MutableInteractionSource() }

    Item(
        modifier = modifier
            .couiButtonFeedback(dark = dark, interactionSource = source, enabled = enabled, list = true)
            .selectable(
                selected = selected,
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        summary = description?.takeIf { it.isNotEmpty() }?.let { desc ->
            {
                Text(
                    text = desc,
                    kind = CouiTextKind.ItemSummary,
                    color = if (enabled) pageColors.summary else pageColors.disabled,
                )
            }
        },
        trailing = {
            Spacer(modifier = Modifier.width(12.dp))
            RadioButton(
                selected = selected,
                enabled = enabled,
            )
        },
    ) {
        Text(
            text = title,
            kind = CouiTextKind.ItemTitle,
            color = if (enabled) pageColors.title else pageColors.disabled,
        )
    }
}

@Composable
fun <T> CouiMenuSelectItem(
    title: String,
    items: List<CouiMenuItem<T>>,
    selectedText: String,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val dark = CouiTheme.isDark
    val pageColors = CouiPageDefaults.themeColors()
    val anchor = rememberCouiMenuAnchor()
    val source = remember { MutableInteractionSource() }
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Item(
            modifier = Modifier
                .couiMenuAnchor(anchor)
                .couiButtonFeedback(
                    dark = dark,
                    interactionSource = source,
                    enabled = enabled,
                    list = true,
                    hoverLocked = expanded,
                )
                .clickable(
                    interactionSource = source,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                ) {
                    anchor.open { expanded = true }
                },
            icon = icon?.let { vec ->
                {
                    Icon(
                        imageVector = vec,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = if (enabled) pageColors.title else pageColors.disabled,
                    )
                }
            },
            summary = summary?.takeIf { it.isNotEmpty() }?.let { desc ->
                {
                    Text(
                        text = desc,
                        kind = CouiTextKind.ItemSummary,
                        color = if (enabled) pageColors.summary else pageColors.disabled,
                    )
                }
            },
            trailing = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = selectedText,
                        kind = CouiTextKind.ItemValue,
                        color = if (enabled) pageColors.summary else pageColors.disabled,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        imageVector = CouiIcons.Filled.Popup,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (enabled) pageColors.summary else pageColors.disabled,
                    )
                }
            },
        ) {
            Text(
                text = title,
                kind = CouiTextKind.ItemTitle,
                color = if (enabled) pageColors.title else pageColors.disabled,
            )
        }

        Menu(
            anchor = anchor,
            expanded = expanded,
            onDismissRequest = { expanded = false },
            items = items,
            onItemClick = { clicked ->
                expanded = false
                onItemSelected(clicked.value)
            },
        )
    }
}

@Composable
fun CouiSpinnerItem(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val safeIndex = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    val menuItems = remember(items, safeIndex) {
        items.mapIndexed { index, label ->
            CouiMenuItem(
                value = index,
                text = label,
                selected = index == safeIndex,
            )
        }
    }
    CouiMenuSelectItem(
        title = title,
        items = menuItems,
        selectedText = items.getOrElse(safeIndex) { "" },
        onItemSelected = onSelectedIndexChange,
        modifier = modifier,
        summary = summary,
        icon = icon,
        enabled = enabled,
    )
}

@Composable
fun CouiMultiApkCheckboxItem(
    title: String,
    summary: String? = null,
    enabled: Boolean = true,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val dark = CouiTheme.isDark
    val pageColors = CouiPageDefaults.themeColors()
    val source = remember { MutableInteractionSource() }

    Item(
        modifier = Modifier
            .couiButtonFeedback(dark = dark, interactionSource = source, enabled = enabled, list = true)
            .toggleable(
                value = checked,
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        summary = summary?.let { desc ->
            {
                Text(
                    text = desc,
                    kind = CouiTextKind.ItemSummary,
                    color = if (enabled) pageColors.summary else pageColors.disabled,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        trailing = {
            Spacer(modifier = Modifier.width(12.dp))
            Checkbox(checked = checked, enabled = enabled)
        },
    ) {
        Text(
            text = title,
            kind = CouiTextKind.ItemTitle,
            color = if (enabled) pageColors.title else pageColors.disabled,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
