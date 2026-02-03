package com.rosan.installer.ui.page.miuix.installer.sheetcontent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.recycle.util.openAppPrivileged
import com.rosan.installer.data.recycle.util.openLSPosedPrivileged
import com.rosan.installer.data.settings.model.room.entity.ext.isPrivileged
import com.rosan.installer.ui.util.isGestureNavigation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun InstallSuccessContent(
    installer: InstallerRepo,
    appInfo: AppInfoState,
    dhizukuAutoClose: Int,
    onClose: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val isXposedModule = if (appInfo.primaryEntity is AppEntity.BaseEntity) appInfo.primaryEntity.isXposedModule else false

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppInfoSlot(appInfo = appInfo)

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.installer_install_success),
            style = MiuixTheme.textStyles.headline2,
            fontWeight = FontWeight.Bold,
            color = MiuixTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (isXposedModule && installer.config.isPrivileged) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    text = stringResource(R.string.open_lsposed),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            openLSPosedPrivileged(
                                config = installer.config,
                                onSuccess = onClose
                            )
                        }
                    }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 24.dp, bottom = if (isGestureNavigation()) 24.dp else 0.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val intent =
                if (appInfo.packageName.isNotEmpty()) context.packageManager.getLaunchIntentForPackage(
                    appInfo.packageName
                ) else null
            TextButton(
                text = stringResource(R.string.finish),
                modifier = Modifier.weight(1f),
                onClick = onClose,
            )
            if (intent != null)
                TextButton(
                    text = stringResource(R.string.open),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            openAppPrivileged(
                                context = context,
                                config = installer.config,
                                packageName = appInfo.packageName,
                                dhizukuAutoCloseSeconds = dhizukuAutoClose,
                                onSuccess = onClose
                            )
                        }
                    }
                )
        }
    }
}