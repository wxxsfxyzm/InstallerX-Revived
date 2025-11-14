package com.rosan.installer.ui.page.miuix.installer.sheetcontent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.ui.page.main.installer.dialog.InstallerViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun UninstallingContent(viewModel: InstallerViewModel) {
    val uninstallInfo by viewModel.uiUninstallInfo.collectAsState()
    val info = uninstallInfo ?: return

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppInfoSlot(
            icon = info.appIcon,
            label = info.appLabel ?: "Unknown App",
            packageName = info.packageName
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            enabled = false,
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InfiniteProgressIndicator()
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.installer_uninstalling)
                )
            }
        }
    }
}