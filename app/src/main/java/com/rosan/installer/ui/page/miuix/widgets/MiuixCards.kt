package com.rosan.installer.ui.page.miuix.widgets

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.build.RsConfig
import com.rosan.installer.ui.page.main.settings.config.all.AllViewAction
import com.rosan.installer.ui.page.main.settings.config.all.AllViewModel
import com.rosan.installer.util.help
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.Cancel
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
private fun TipCard(
    modifier: Modifier = Modifier,
    tipContent: @Composable () -> Unit,
    actionContent: @Composable () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
            contentColor = MiuixTheme.colorScheme.onSurface
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                tipContent()
            }
            actionContent()
        }
    }
}

@Composable
fun MiuixScopeTipCard(viewModel: AllViewModel) {
    TipCard(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        tipContent = {
            Text(
                text = stringResource(R.string.scope_tips),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    ) {
        IconButton(
            modifier = Modifier
                .size(22.dp)
                .padding(start = 2.dp),
            onClick = { viewModel.dispatch(AllViewAction.UserReadScopeTips) },
        ) {
            Icon(
                imageVector = MiuixIcons.Useful.Cancel,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun MiuixNoneInstallerTipCard() {
    TipCard(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        tipContent = {
            Text(
                text = stringResource(R.string.config_authorizer_none_tips),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    ) {}
}

@Composable
fun MiuixInstallChoiceTipCard(text: String) {
    TipCard(
        modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp),
        tipContent = {
            Text(
                text = text,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    ) {}
}

/**
 * A Composable that displays an error message in a MIUIX-style Card.
 * The card is divided into two sections:
 * - The top section shows a user-friendly error message from `error.help()`.
 * - The bottom section displays detailed error information, which is the full
 * stack trace in debug builds or the error message in release builds.
 *
 *
 * @param error The throwable error to display.
 * @param modifier Modifier for the root Card.
 */
@Composable
fun MiuixErrorTextBlock(
    error: Throwable,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val cardBackgroundColor = if (isDark) Color(0xCC8C2323) else Color(0xCCFBEAEA)
    val errorColor = if (isDark) Color.White else Color(0xFF601A15)

    Card(
        modifier = modifier,
        colors = CardColors(
            color = cardBackgroundColor,
            contentColor = errorColor
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = error.help(),
                fontWeight = FontWeight.Bold,
                style = MiuixTheme.textStyles.body1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 16.dp, 16.dp, 8.dp)
            )

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            val textToShow = if (RsConfig.isDebug) {
                error.stackTraceToString()
            } else {
                error.message ?: "An unknown error occurred."
            }.trim()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 8.dp, 16.dp, 16.dp)
            ) {
                BasicTextField(
                    value = textToShow,
                    onValueChange = {},
                    readOnly = true,
                    textStyle = LocalTextStyle.current.copy(color = errorColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        // Allow vertical scrolling for long stack traces
                        .verticalScroll(rememberScrollState())
                )
            }
        }
    }
}