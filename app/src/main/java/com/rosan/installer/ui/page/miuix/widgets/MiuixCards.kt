package com.rosan.installer.ui.page.miuix.widgets

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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
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
import com.rosan.installer.ui.theme.InstallerTheme
import com.rosan.installer.util.help
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.isDynamicColor

@Composable
private fun TipCard(
    modifier: Modifier = Modifier,
    tipContent: @Composable () -> Unit,
    actionContent: @Composable (() -> Unit)? = null
) {
    val endPadding = if (actionContent == null) 16.dp else 12.dp

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
            contentColor = MiuixTheme.colorScheme.onSurface
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = endPadding, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
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
fun WarningCard(
    isDark: Boolean,
    message: String
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 0.dp, vertical = 8.dp)
            .fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = when {
                isDynamicColor -> MiuixTheme.colorScheme.errorContainer
                isDark -> Color(0XFF310808)
                else -> Color(0xFFF8E2E2)
            }
        )
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
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun MiuixScopeTipCard(viewModel: AllViewModel) {
    TipCard(
        // modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
                imageVector = MiuixIcons.Regular.Close,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun MiuixSettingsTipCard(text: String) {
    TipCard(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        tipContent = {
            Text(
                text = text,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    )
}

@Composable
fun MiuixInstallerTipCard(text: String) {
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
    )
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
    val scheme = if (InstallerTheme.useMiuixMonet)
        InstallerTheme.colorScheme
    else
        InstallerTheme.colorScheme.copy(
            errorContainer = MiuixTheme.colorScheme.errorContainer,
            onErrorContainer = MiuixTheme.colorScheme.onErrorContainer
        )
    val cardBackgroundColor = scheme.errorContainer
    val contentColor = scheme.onErrorContainer

    Card(
        modifier = modifier,
        colors = CardColors(
            color = cardBackgroundColor,
            contentColor = contentColor
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                color = scheme.error,
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
                    .padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outline
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
                    textStyle = LocalTextStyle.current.copy(color = contentColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        // Allow vertical scrolling for long stack traces
                        .verticalScroll(rememberScrollState())
                )
            }
        }
    }
}