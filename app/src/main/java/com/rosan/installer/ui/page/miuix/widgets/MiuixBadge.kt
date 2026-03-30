// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalTextStyle
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
import com.rosan.installer.R
import com.rosan.installer.ui.page.main.widget.chip.NoticeModel
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun MiuixBadge(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MiuixTheme.colorScheme.primary,
    containerColor: Color = MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
) {
    Surface(
        // Set a minimum height to eliminate visual differences between languages
        modifier = modifier.defaultMinSize(minHeight = 24.dp),
        shape = CircleShape,
        color = containerColor,
        contentColor = textColor
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                // Remove default font padding for accurate vertical centering
                style = LocalTextStyle.current.copy(
                    platformStyle = PlatformTextStyle(
                        includeFontPadding = false
                    )
                )
            )
        }
    }
}

@Composable
fun MiuixInfoChipGroup(
    modifier: Modifier = Modifier,
    notices: List<NoticeModel>
) {
    var selectedNotice by remember { mutableStateOf<NoticeModel?>(null) }
    val showDialog = remember { mutableStateOf(false) }

    if (notices.isEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        return
    }

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        notices.forEach { item ->
            MiuixBadge(
                text = item.shortLabel,
                textColor = item.color,
                containerColor = item.color.copy(alpha = 0.2f),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable {
                        selectedNotice = item
                        showDialog.value = true
                    }
            )
        }
    }

    if (selectedNotice != null) {
        WindowDialog(
            show = showDialog.value,
            title = selectedNotice!!.shortLabel,
            titleColor = selectedNotice!!.color,
            summary = selectedNotice!!.fullDescription,
            onDismissRequest = {
                showDialog.value = false
            },
            onDismissFinished = {
                selectedNotice = null
            },
            content = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.confirm),
                        onClick = { showDialog.value = false }
                    )
                }
            }
        )
    }
}
