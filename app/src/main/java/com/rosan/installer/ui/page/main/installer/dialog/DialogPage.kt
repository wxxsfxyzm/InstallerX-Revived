// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer.dialog

import androidx.compose.animation.AnimatedContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.ui.page.main.installer.InstallerStage
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.dialog.inner.ModuleInstallSheetContent
import com.rosan.installer.ui.page.main.widget.dialog.PositionDialog
import com.rosan.installer.ui.page.main.widget.util.ToastEventCollector
import com.rosan.installer.ui.theme.InstallerMaterialExpressiveTheme
import com.rosan.installer.ui.theme.InstallerTheme
import com.rosan.installer.ui.theme.LocalInstallerColorScheme
import com.rosan.installer.ui.theme.material.dynamicColorScheme
import com.rosan.installer.ui.util.WindowBlurEffect
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogPage(
    session: InstallerSessionRepository,
    viewModel: InstallerViewModel = koinViewModel { parametersOf(session) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stage = uiState.stage
    val viewSettings = uiState.viewSettings
    val temporarySeedColor = uiState.seedColor
    val useBlur = viewSettings.useBlur && viewSettings.uiExpressive

    val globalColorScheme = InstallerTheme.colorScheme
    val isDark = InstallerTheme.isDark
    val paletteStyle = InstallerTheme.paletteStyle
    val colorSpec = InstallerTheme.colorSpec

    val activeColorScheme = remember(temporarySeedColor, globalColorScheme, isDark, paletteStyle) {
        temporarySeedColor?.let {
            dynamicColorScheme(
                keyColor = it,
                isDark = isDark,
                style = paletteStyle,
                colorSpec = colorSpec
            )
        } ?: globalColorScheme
    }

    LaunchedEffect(session.id) {
        viewModel.dispatch(InstallerViewAction.CollectSession(session))
    }

    ToastEventCollector(viewModel)

    CompositionLocalProvider(
        LocalInstallerColorScheme provides activeColorScheme
    ) {
        InstallerMaterialExpressiveTheme(
            colorScheme = activeColorScheme,
            darkTheme = isDark,
            compatStatusBarColor = false
        ) {
            val colorScheme = InstallerTheme.colorScheme
            // Handle InstallingModule state: Show ModalBottomSheet
            if (stage is InstallerStage.InstallingModule) {
                // Do NOT create a local variable for isDismissible here.
                // Capturing a changing local variable causes the lambda below to change,
                // which forces rememberModalBottomSheetState to recreate the state, resetting the sheet.

                val sheetState = rememberModalBottomSheetState(
                    skipPartiallyExpanded = true,
                    confirmValueChange = { sheetValue ->
                        if (sheetValue == SheetValue.Hidden) {
                            // Access the property directly from the stable viewModel.
                            // This ensures the lambda instance remains stable across state changes.
                            viewModel.uiState.value.isDismissible
                        } else {
                            true
                        }
                    }
                )

                ModalBottomSheet(
                    onDismissRequest = {
                        if (uiState.isDismissible) {
                            viewModel.dispatch(InstallerViewAction.Close)
                        }
                    },
                    sheetState = sheetState,
                    containerColor = colorScheme.surfaceContainer,
                    contentColor = colorScheme.onSurface
                ) {
                    val blurRadius = if (sheetState.targetValue == SheetValue.Expanded) 30 else 0
                    AnimatedContent(targetState = blurRadius) { targetState ->
                        WindowBlurEffect(useBlur = useBlur, blurRadius = targetState)
                    }

                    ModuleInstallSheetContent(
                        outputLines = stage.output,
                        isFinished = stage.isFinished,
                        onReboot = { viewModel.dispatch(InstallerViewAction.Reboot("")) },
                        onClose = { viewModel.dispatch(InstallerViewAction.Close) },
                        colorScheme = colorScheme
                    )
                }
            }
            // Handle other non-Ready states: Show standard PositionDialog
            else if (stage !is InstallerStage.Ready) {
                val params = dialogGenerateParams(viewModel)

                PositionDialog(
                    useBlur = useBlur,
                    onDismissRequest = {
                        if (uiState.isDismissible) {
                            if (viewSettings.disableNotificationOnDismiss) {
                                viewModel.dispatch(InstallerViewAction.Close)
                            } else {
                                viewModel.dispatch(InstallerViewAction.Background)
                            }
                        }
                    },
                    centerIcon = dialogInnerWidget(params.icon),
                    centerTitle = dialogInnerWidget(params.title),
                    centerSubtitle = dialogInnerWidget(params.subtitle),
                    centerText = dialogInnerWidget(params.text),
                    centerContent = dialogInnerWidget(params.content),
                    centerButton = dialogInnerWidget(params.buttons)
                )
            }
        }
    }
}