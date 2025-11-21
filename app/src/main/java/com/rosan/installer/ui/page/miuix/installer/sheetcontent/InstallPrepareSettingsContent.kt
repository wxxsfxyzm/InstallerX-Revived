package com.rosan.installer.ui.page.miuix.installer.sheetcontent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.build.Manufacturer
import com.rosan.installer.build.RsConfig
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.main.installer.dialog.InstallerViewModel
import com.rosan.installer.ui.page.miuix.widgets.MiuixSwitchWidget
import com.rosan.installer.ui.theme.miuixSheetCardColorDark
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.isDynamicColor

@Composable
fun PrepareSettingsContent(
    colorScheme: ColorScheme,
    isDarkMode: Boolean,
    installer: InstallerRepo,
    viewModel: InstallerViewModel
) {
    val settings = viewModel.viewSettings
    var autoDelete by remember { mutableStateOf(installer.config.autoDelete) }
    var displaySdk by remember { mutableStateOf(installer.config.displaySdk) }
    var showOPPOSpecial by remember { mutableStateOf(settings.showOPPOSpecial) }

    LaunchedEffect(autoDelete, displaySdk) {
        val currentConfig = installer.config
        if (currentConfig.autoDelete != autoDelete) installer.config.autoDelete = autoDelete
        if (currentConfig.displaySdk != displaySdk) installer.config.displaySdk = displaySdk
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.padding(bottom = 6.dp),
            colors = CardColors(
                color = if (isDynamicColor) colorScheme.surfaceContainer else
                    if (isDarkMode) miuixSheetCardColorDark else Color.White,
                contentColor = MiuixTheme.colorScheme.onSurface
            )
        ) {
            MiuixSwitchWidget(
                title = stringResource(R.string.config_display_sdk_version),
                description = stringResource(R.string.config_display_sdk_version_desc),
                checked = displaySdk,
                onCheckedChange = {
                    val newValue = !displaySdk
                    displaySdk = newValue
                    installer.config.displaySdk = newValue
                }
            )
            MiuixSwitchWidget(
                title = stringResource(R.string.config_auto_delete),
                description = stringResource(R.string.config_auto_delete_desc),
                checked = installer.config.autoDelete,
                onCheckedChange = {
                    val newValue = !autoDelete
                    autoDelete = newValue
                    installer.config.autoDelete = newValue
                }
            )
            if (RsConfig.currentManufacturer == Manufacturer.OPPO || RsConfig.currentManufacturer == Manufacturer.ONEPLUS)
                MiuixSwitchWidget(
                    title = stringResource(R.string.installer_show_oem_special),
                    description = stringResource(R.string.installer_show_oem_special_desc),
                    checked = showOPPOSpecial,
                    onCheckedChange = {
                        val newValue = !showOPPOSpecial
                        showOPPOSpecial = newValue
                        settings.copy(showOPPOSpecial = newValue)
                    }
                )
        }
        Spacer(Modifier.height(24.dp))
    }
}