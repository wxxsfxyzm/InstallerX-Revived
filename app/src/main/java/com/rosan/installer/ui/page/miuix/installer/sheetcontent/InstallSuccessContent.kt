// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.installer.sheetcontent

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.engine.model.AppEntity
import com.rosan.installer.domain.privileged.usecase.OpenAppUseCase
import com.rosan.installer.domain.privileged.usecase.OpenAppUseCase.Companion.PRIVILEGED_START_TIMEOUT_MS
import com.rosan.installer.domain.privileged.usecase.OpenLSPosedUseCase
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.isPrivileged
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.util.isGestureNavigation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.isDynamicColor

@Composable
fun InstallSuccessContent(
    appInfo: AppInfoState,
    viewModel: InstallerViewModel,
    dhizukuAutoClose: Int,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val config = uiState.config
    val coroutineScope = rememberCoroutineScope()
    val capabilityProvider: DeviceCapabilityProvider = koinInject()
    val openAppUseCase: OpenAppUseCase = koinInject()
    val openLSPosedUseCase: OpenLSPosedUseCase = koinInject()

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

        if (isXposedModule && config.isPrivileged(capabilityProvider)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    text = stringResource(R.string.open_lsposed),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(
                        color = if (isDynamicColor) MiuixTheme.colorScheme.secondaryContainer else MiuixTheme.colorScheme.secondaryVariant,
                        textColor = if (isDynamicColor) MiuixTheme.colorScheme.onSecondaryContainer else MiuixTheme.colorScheme.onSecondaryVariant
                    ),
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            val success = openLSPosedUseCase(config)
                            if (success) {
                                launch(Dispatchers.Main) { onClose() }
                            }
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
                colors = ButtonDefaults.textButtonColors(
                    color = if (isDynamicColor) MiuixTheme.colorScheme.secondaryContainer else MiuixTheme.colorScheme.secondaryVariant,
                    textColor = if (isDynamicColor) MiuixTheme.colorScheme.onSecondaryContainer else MiuixTheme.colorScheme.onSecondaryVariant
                ),
                onClick = onClose,
            )
            if (intent != null)
                TextButton(
                    text = stringResource(R.string.open),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            val result = openAppUseCase(
                                config = config,
                                launchIntent = intent
                            )

                            when (result) {
                                is OpenAppUseCase.Result.SuccessPrivileged -> {
                                    launch(Dispatchers.Main) { onClose() }
                                }

                                is OpenAppUseCase.Result.FallbackRequired -> {
                                    launch(Dispatchers.Main) {
                                        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

                                        if (config.authorizer == Authorizer.Dhizuku) {
                                            delay(dhizukuAutoClose * 1000L)
                                        } else {
                                            delay(PRIVILEGED_START_TIMEOUT_MS)
                                        }
                                        onClose()
                                    }
                                }
                            }
                        }
                    }
                )
        }
    }
}