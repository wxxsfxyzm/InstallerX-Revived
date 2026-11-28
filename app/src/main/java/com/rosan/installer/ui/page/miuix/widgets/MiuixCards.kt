package com.rosan.installer.ui.page.miuix.widgets

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.core.env.AppConfig
import com.rosan.installer.ui.feature.settings.config.all.AllViewAction
import com.rosan.installer.ui.feature.settings.config.all.AllViewModel
import com.rosan.installer.ui.miuix.widgets.MiuixErrorDetails
import com.rosan.installer.ui.miuix.widgets.MiuixTipCard
import com.rosan.installer.ui.theme.InstallerTheme
import com.rosan.installer.util.help
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MiuixScopeTipCard(viewModel: AllViewModel) {
    MiuixTipCard(
        tipContent = {
            Text(
                text = stringResource(R.string.scope_tips),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        },
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
                tint = MiuixTheme.colorScheme.primary,
            )
        }
    }
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
fun MiuixErrorTextBlock(error: Throwable, modifier: Modifier = Modifier) {
    val scheme = if (InstallerTheme.useMiuixMonet) {
        InstallerTheme.colorScheme
    } else {
        InstallerTheme.colorScheme.copy(
            errorContainer = MiuixTheme.colorScheme.errorContainer,
            onErrorContainer = MiuixTheme.colorScheme.onErrorContainer,
        )
    }
    val details = if (AppConfig.isDebug) {
        error.stackTraceToString()
    } else {
        error.message ?: "An unknown error occurred."
    }.trim()
    MiuixErrorDetails(
        message = error.help(),
        details = details,
        modifier = modifier,
        containerColor = scheme.errorContainer,
        contentColor = scheme.onErrorContainer,
        titleColor = scheme.error,
    )
}
