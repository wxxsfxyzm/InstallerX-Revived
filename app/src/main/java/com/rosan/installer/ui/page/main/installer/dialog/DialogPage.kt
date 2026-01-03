package com.rosan.installer.ui.page.main.installer.dialog

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.InstallerViewState
import com.rosan.installer.ui.page.main.installer.dialog.inner.ModuleInstallSheetContent
import com.rosan.installer.ui.page.main.widget.dialog.PositionDialog
import com.rosan.installer.ui.theme.InstallerMaterialExpressiveTheme
import com.rosan.installer.ui.theme.InstallerTheme
import com.rosan.installer.ui.theme.LocalInstallerColorScheme
import com.rosan.installer.ui.theme.LocalIsDark
import com.rosan.installer.ui.theme.LocalPaletteStyle
import com.rosan.installer.ui.theme.m3color.dynamicColorScheme
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogPage(
    installer: InstallerRepo,
    viewModel: InstallerViewModel = koinViewModel {
        parametersOf(installer)
    }
) {
    val temporarySeedColor by viewModel.seedColor.collectAsState()
    val globalColorScheme = LocalInstallerColorScheme.current
    val isDark = LocalIsDark.current
    val paletteStyle = LocalPaletteStyle.current

    val activeColorScheme = remember(temporarySeedColor, globalColorScheme, isDark, paletteStyle) {
        temporarySeedColor?.let {
            dynamicColorScheme(keyColor = it, isDark = isDark, style = paletteStyle)
        } ?: globalColorScheme
    }

    LaunchedEffect(installer.id) {
        viewModel.dispatch(InstallerViewAction.CollectRepo(installer))
    }

    val state = viewModel.state

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
            if (state is InstallerViewState.InstallingModule) {
                // Do NOT create a local variable for isDismissible here.
                // Capturing a changing local variable causes the lambda below to change,
                // which forces rememberModalBottomSheetState to recreate the state, resetting the sheet.

                val sheetState = rememberModalBottomSheetState(
                    skipPartiallyExpanded = true,
                    confirmValueChange = { sheetValue ->
                        if (sheetValue == SheetValue.Hidden) {
                            // Access the property directly from the stable viewModel.
                            // This ensures the lambda instance remains stable across state changes.
                            viewModel.isDismissible
                        } else {
                            true
                        }
                    }
                )

                ModalBottomSheet(
                    onDismissRequest = {
                        if (viewModel.isDismissible) {
                            viewModel.dispatch(InstallerViewAction.Close)
                        }
                    },
                    sheetState = sheetState,
                    containerColor = colorScheme.surfaceContainer,
                    contentColor = colorScheme.onSurface
                ) {
                    ModuleInstallSheetContent(
                        outputLines = state.output,
                        isFinished = state.isFinished,
                        onReboot = { viewModel.dispatch(InstallerViewAction.Reboot("")) },
                        onClose = { viewModel.dispatch(InstallerViewAction.Close) },
                        colorScheme = colorScheme
                    )
                }
            }
            // Handle other non-Ready states: Show standard PositionDialog
            else if (state !is InstallerViewState.Ready) {
                val params = dialogGenerateParams(installer, viewModel)

                PositionDialog(
                    onDismissRequest = {
                        if (viewModel.isDismissible) {
                            if (viewModel.viewSettings.disableNotificationOnDismiss) {
                                viewModel.dispatch(InstallerViewAction.Close)
                            } else {
                                viewModel.dispatch(InstallerViewAction.Background)
                            }
                        }
                    },
                    centerIcon = dialogInnerWidget(installer, params.icon),
                    centerTitle = dialogInnerWidget(installer, params.title),
                    centerSubtitle = dialogInnerWidget(installer, params.subtitle),
                    centerText = dialogInnerWidget(installer, params.text),
                    centerContent = dialogInnerWidget(installer, params.content),
                    centerButton = dialogInnerWidget(installer, params.buttons)
                )
            }
        }
    }
}