package com.rosan.installer.ui.page.main.installer.dialog

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.main.widget.dialog.PositionDialog
import com.rosan.installer.ui.theme.m3color.PaletteStyle
import com.rosan.installer.ui.theme.m3color.dynamicColorScheme
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun DialogPage(
    installer: InstallerRepo, viewModel: InstallerViewModel = koinViewModel {
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
    if (viewModel.state !is InstallerViewState.Ready) {
        val params = dialogGenerateParams(installer, viewModel)

        PositionDialog(
            onDismissRequest = {
                if (viewModel.isDismissible) {
                    // Only allow dismiss if the current state is dismissible
                    // If the setting is enabled, close the dialog directly.
                    // Otherwise, send it to the background (which shows a notification).
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