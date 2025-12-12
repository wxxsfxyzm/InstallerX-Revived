package com.rosan.installer.ui.page.main.installer.dialog

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.InstallerViewState
import com.rosan.installer.ui.page.main.installer.dialog.inner.ModuleInstallSheetContent
import com.rosan.installer.ui.page.main.widget.dialog.PositionDialog
import com.rosan.installer.ui.theme.m3color.PaletteStyle
import com.rosan.installer.ui.theme.m3color.dynamicColorScheme
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogPage(
    installer: InstallerRepo,
    viewModel: InstallerViewModel = koinViewModel {
        parametersOf(installer)
    },
    activeColorSchemeState: MutableState<ColorScheme>,
    globalColorScheme: ColorScheme,
    isDarkMode: Boolean,
    basePaletteStyle: PaletteStyle
) {
    val temporarySeedColor by viewModel.seedColor.collectAsState()
    LaunchedEffect(temporarySeedColor, globalColorScheme, isDarkMode, basePaletteStyle) {
        if (temporarySeedColor == null) {
            activeColorSchemeState.value = globalColorScheme
        } else {
            activeColorSchemeState.value = dynamicColorScheme(
                keyColor = temporarySeedColor!!,
                isDark = isDarkMode,
                style = basePaletteStyle
            )
        }
    }

    LaunchedEffect(installer.id) {
        viewModel.dispatch(InstallerViewAction.CollectRepo(installer))
    }

    val state = viewModel.state

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
            containerColor = activeColorSchemeState.value.surfaceContainer,
            contentColor = activeColorSchemeState.value.onSurface
        ) {
            ModuleInstallSheetContent(
                outputLines = state.output,
                isFinished = state.isFinished,
                onClose = {
                    viewModel.dispatch(InstallerViewAction.Close)
                },
                colorScheme = activeColorSchemeState.value
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