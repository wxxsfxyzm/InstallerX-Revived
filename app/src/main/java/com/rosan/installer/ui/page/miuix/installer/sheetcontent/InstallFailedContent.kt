package com.rosan.installer.ui.page.miuix.installer.sheetcontent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.miuix.settings.preferred.MiuixNavigationItemWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixErrorTextBlock
import com.rosan.installer.ui.page.miuix.widgets.MiuixUninstallConfirmationDialog
import com.rosan.installer.ui.theme.InstallerTheme
import com.rosan.installer.ui.theme.miuixSheetCardColorDark
import com.rosan.installer.ui.util.isGestureNavigation
import com.rosan.installer.ui.util.rememberErrorSuggestions
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.isDynamicColor

@Composable
fun InstallFailedContent(
    appInfo: AppInfoState,
    viewModel: InstallerViewModel,
    onClose: () -> Unit
) {
    val isDarkMode = InstallerTheme.isDark
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentError = uiState.error

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppInfoSlot(appInfo = appInfo)
        Spacer(modifier = Modifier.height(32.dp))
        MiuixErrorTextBlock(
            error = currentError,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.height(16.dp))
        MiuixErrorSuggestions(
            isDarkMode = isDarkMode,
            error = currentError,
            viewModel = viewModel
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 8.dp, bottom = if (isGestureNavigation()) 24.dp else 0.dp),
        ) {
            TextButton(
                onClick = onClose,
                text = stringResource(R.string.close),
                colors = ButtonDefaults.textButtonColors(
                    color = if (isDynamicColor) MiuixTheme.colorScheme.secondaryContainer else MiuixTheme.colorScheme.secondaryVariant,
                    textColor = if (isDynamicColor) MiuixTheme.colorScheme.onSecondaryContainer else MiuixTheme.colorScheme.onSecondaryVariant
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MiuixErrorSuggestions(
    isDarkMode: Boolean,
    error: Throwable,
    viewModel: InstallerViewModel,
) {
    val showUninstallConfirmDialogState = remember { mutableStateOf(false) }
    var confirmKeepData by remember { mutableStateOf(false) }
    var pendingConflictingPackage by remember { mutableStateOf<String?>(null) }

    val visibleSuggestions = rememberErrorSuggestions(
        error = error,
        viewModel = viewModel,
        onShowUninstallConfirm = { keepData, conflictingPkg ->
            confirmKeepData = keepData
            pendingConflictingPackage = conflictingPkg
            showUninstallConfirmDialogState.value = true
        }
    )

    if (visibleSuggestions.isNotEmpty()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SmallTitle(
                text = stringResource(R.string.smart_suggestions),
                insideMargin = PaddingValues(12.dp, 8.dp)
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardColors(
                    color = if (isDynamicColor) MiuixTheme.colorScheme.surfaceContainer else
                        if (isDarkMode) miuixSheetCardColorDark else Color.White,
                    contentColor = MiuixTheme.colorScheme.onSurface
                )
            ) {
                visibleSuggestions.forEach { suggestion ->
                    MiuixNavigationItemWidget(
                        title = stringResource(id = suggestion.labelRes),
                        description = suggestion.descriptionRes?.let { stringResource(id = it) } ?: "",
                        onClick = suggestion.onClick,
                        insideMargin = PaddingValues(12.dp)
                    )
                }
            }
        }
    }

    MiuixUninstallConfirmationDialog(
        showState = showUninstallConfirmDialogState,
        onDismiss = { showUninstallConfirmDialogState.value = false },
        onConfirm = {
            viewModel.dispatch(
                InstallerViewAction.UninstallAndRetryInstall(
                    keepData = confirmKeepData,
                    conflictingPackage = pendingConflictingPackage
                )
            )
        },
        keepData = confirmKeepData
    )
}
