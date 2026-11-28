// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.dialog.Dialog
import com.itos.coui.menu.rememberCouiWindowBackdrop
import com.itos.coui.theme.CouiTheme
import com.rosan.installer.R
import com.rosan.installer.ui.page.main.widget.chip.NoticeModel

@Composable
fun CouiBadge(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = CouiTheme.colorScheme.accentOnSurface,
    containerColor: Color = CouiTheme.colorScheme.accent.copy(alpha = 0.16f),
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 24.dp)
            .clip(CircleShape)
            .background(containerColor)
            .padding(horizontal = 9.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            style = LocalTextStyle.current.copy(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        )
    }
}

@Composable
fun CouiInfoChipGroup(modifier: Modifier = Modifier, notices: List<NoticeModel>) {
    var selectedNotice by remember { mutableStateOf<NoticeModel?>(null) }
    var visible by remember { mutableStateOf(false) }

    if (notices.isEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        return
    }

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        notices.forEach { item ->
            CouiBadge(
                text = item.shortLabel,
                textColor = item.color,
                containerColor = item.color.copy(alpha = 0.16f),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable {
                        selectedNotice = item
                        visible = true
                    },
            )
        }
    }

    val backdrop = rememberCouiWindowBackdrop()
    val material = if (CouiDialogDefaults.isMaterialSupported() && backdrop != null) {
        CouiDialogDefaults.material(CouiTheme.isDark)
    } else {
        null
    }

    Dialog(
        visible = visible,
        onDismissRequest = { visible = false },
        title = selectedNotice?.shortLabel,
        confirmAction = CouiDialogAction(
            text = stringResource(R.string.confirm),
            onClick = { visible = false },
            style = CouiDialogActionStyle.Primary,
        ),
        material = material,
        backdrop = backdrop,
    ) {
        selectedNotice?.let { notice ->
            SelectionContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CouiDialogDefaults.contentPadding)
                    .padding(bottom = CouiDialogDefaults.messageBottom),
            ) {
                Text(
                    text = notice.fullDescription,
                    color = CouiTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
            }
        }
    }
}
