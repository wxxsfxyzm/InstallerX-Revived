// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itos.coui.icons.CouiIcons
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.layout.Divider
import com.itos.coui.theme.CouiTheme
import com.rosan.installer.R
import com.rosan.installer.core.env.AppConfig
import com.rosan.installer.ui.page.main.settings.config.all.AllViewAction
import com.rosan.installer.ui.page.main.settings.config.all.AllViewModel
import com.rosan.installer.ui.theme.InstallerTheme
import com.rosan.installer.util.help

@Composable
private fun CouiTipCard(
    modifier: Modifier = Modifier,
    tipContent: @Composable () -> Unit,
    actionContent: @Composable (() -> Unit)? = null,
) {
    val scheme = CouiTheme.colorScheme
    val endPadding = if (actionContent == null) 16.dp else 12.dp

    CouiCard(
        modifier = modifier,
        color = scheme.accent.copy(alpha = if (CouiTheme.isDark) 0.16f else 0.10f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = endPadding, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                tipContent()
            }
            if (actionContent != null) {
                Spacer(modifier = Modifier.width(8.dp))
                actionContent()
            }
        }
    }
}

@Composable
fun CouiWarningCard(isDark: Boolean, message: String, modifier: Modifier = Modifier) {
    val containerColor = if (isDark) Color(0xFF331414) else Color(0xFFFDECEC)
    val contentColor = if (isDark) Color(0xFFFF7A70) else Color(0xFFD93025)

    CouiCard(
        modifier = modifier.padding(vertical = 8.dp),
        color = containerColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = message,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun CouiScopeTipCard(viewModel: AllViewModel) {
    val accent = CouiTheme.colorScheme.accentOnSurface
    CouiTipCard(
        modifier = Modifier.padding(horizontal = CouiPageDefaults.horizontalMargin, vertical = 8.dp),
        tipContent = {
            Text(
                text = stringResource(R.string.scope_tips),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = accent,
                fontWeight = FontWeight.Medium,
            )
        },
        actionContent = {
            IconButton(
                modifier = Modifier.size(28.dp),
                onClick = { viewModel.dispatch(AllViewAction.UserReadScopeTips) },
            ) {
                Icon(
                    imageVector = CouiIcons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = accent,
                )
            }
        },
    )
}

@Composable
fun CouiSettingsTipCard(text: String) {
    val accent = CouiTheme.colorScheme.accentOnSurface
    CouiTipCard(
        modifier = Modifier.padding(horizontal = CouiPageDefaults.horizontalMargin, vertical = 8.dp),
        tipContent = {
            Text(
                text = text,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = accent,
                fontWeight = FontWeight.Medium,
            )
        },
    )
}

@Composable
fun CouiInstallerTipCard(text: String) {
    val accent = CouiTheme.colorScheme.accentOnSurface
    CouiTipCard(
        modifier = Modifier.padding(vertical = 8.dp),
        tipContent = {
            Text(
                text = text,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = accent,
                fontWeight = FontWeight.Medium,
            )
        },
    )
}

@Composable
fun CouiErrorTextBlock(error: Throwable, modifier: Modifier = Modifier) {
    val dark = CouiTheme.isDark
    val scheme = InstallerTheme.colorScheme
    val cardBackgroundColor = if (InstallerTheme.useCouiMonet) {
        scheme.errorContainer
    } else if (dark) {
        Color(0xFF331414)
    } else {
        Color(0xFFFDECEC)
    }
    val titleColor = if (InstallerTheme.useCouiMonet) {
        scheme.error
    } else if (dark) {
        Color(0xFFFF7A70)
    } else {
        Color(0xFFD93025)
    }
    val contentColor = if (InstallerTheme.useCouiMonet) {
        scheme.onErrorContainer
    } else if (dark) {
        Color(0xFFFFDAD6)
    } else {
        Color(0xFF410E0B)
    }

    CouiCard(
        modifier = modifier,
        color = cardBackgroundColor,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                color = titleColor,
                text = error.help(),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 16.dp, 16.dp, 8.dp),
            )

            Divider(color = titleColor.copy(alpha = 0.2f))

            val textToShow = if (AppConfig.isDebug) {
                error.stackTraceToString()
            } else {
                error.message ?: "An unknown error occurred."
            }.trim()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 8.dp, 16.dp, 16.dp),
            ) {
                BasicTextField(
                    value = textToShow,
                    onValueChange = {},
                    readOnly = true,
                    textStyle = LocalTextStyle.current.copy(color = contentColor, fontSize = 13.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                )
            }
        }
    }
}
