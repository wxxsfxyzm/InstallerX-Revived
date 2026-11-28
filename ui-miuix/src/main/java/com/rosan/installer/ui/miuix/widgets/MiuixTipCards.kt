package com.rosan.installer.ui.miuix.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.isDynamicColor

@Composable
fun MiuixTipCard(
    modifier: Modifier = Modifier,
    tipContent: @Composable () -> Unit,
    actionContent: @Composable (() -> Unit)? = null,
) {
    val endPadding = if (actionContent == null) 16.dp else 12.dp

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
            contentColor = MiuixTheme.colorScheme.onSurface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = endPadding, top = 16.dp, bottom = 16.dp),
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
fun WarningCard(isDark: Boolean, message: String) {
    Card(
        modifier = Modifier
            .padding(horizontal = 0.dp, vertical = 8.dp)
            .fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = when {
                isDynamicColor -> MiuixTheme.colorScheme.errorContainer
                isDark -> Color(0XFF310808)
                else -> Color(0xFFF8E2E2)
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = message,
                style = MiuixTheme.textStyles.body2,
                color = if (isDynamicColor) MiuixTheme.colorScheme.onErrorContainer else Color(0xFFF72727),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun MiuixSettingsTipCard(text: String) {
    MiuixTipCard(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        tipContent = {
            Text(
                text = text,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        },
    )
}

@Composable
fun MiuixInstallerTipCard(text: String) {
    MiuixTipCard(
        modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp),
        tipContent = {
            Text(
                text = text,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        },
    )
}

@Composable
fun MiuixErrorDetails(
    message: String,
    details: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MiuixTheme.colorScheme.errorContainer,
    contentColor: Color = MiuixTheme.colorScheme.onErrorContainer,
    titleColor: Color = MiuixTheme.colorScheme.error,
    dividerColor: Color = MiuixTheme.colorScheme.dividerLine,
) {
    Card(
        modifier = modifier,
        colors = CardColors(
            color = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                color = titleColor,
                text = message,
                fontWeight = FontWeight.Bold,
                style = MiuixTheme.textStyles.body1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 16.dp, 16.dp, 8.dp),
            )

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = dividerColor,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 8.dp, 16.dp, 16.dp),
            ) {
                BasicTextField(
                    value = details,
                    onValueChange = {},
                    readOnly = true,
                    textStyle = MiuixTheme.textStyles.body2.copy(color = contentColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        // Allow vertical scrolling for long stack traces
                        .verticalScroll(rememberScrollState()),
                )
            }
        }
    }
}
