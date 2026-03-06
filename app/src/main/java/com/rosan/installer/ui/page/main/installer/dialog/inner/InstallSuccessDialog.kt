// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer.dialog.inner

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.engine.model.AppEntity
import com.rosan.installer.domain.privileged.usecase.OpenAppUseCase
import com.rosan.installer.domain.privileged.usecase.OpenAppUseCase.Companion.PRIVILEGED_START_TIMEOUT_MS
import com.rosan.installer.domain.privileged.usecase.OpenLSPosedUseCase
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.isPrivileged
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.dialog.DialogParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParamsType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@Composable
fun installSuccessDialog(
    installer: InstallerSessionRepository,
    viewModel: InstallerViewModel
): DialogParams {
    val context = LocalContext.current
    val deviceCapabilityProvider: DeviceCapabilityProvider = koinInject()
    val currentPackageName by viewModel.currentPackageName.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val settings = viewModel.viewSettings

    val openAppUseCase: OpenAppUseCase = koinInject()
    val openLSPosedUseCase: OpenLSPosedUseCase = koinInject()

    val packageName = currentPackageName ?: installer.analysisResults.firstOrNull()?.packageName ?: ""
    val currentPackage = installer.analysisResults.find { it.packageName == packageName }

    val selectedEntities = currentPackage?.appEntities
        ?.filter { it.selected }
        ?.map { it.app }
    val effectivePrimaryEntity = selectedEntities?.filterIsInstance<AppEntity.BaseEntity>()?.firstOrNull()
        ?: selectedEntities?.filterIsInstance<AppEntity.ModuleEntity>()?.firstOrNull()

    val isXposedModule = if (effectivePrimaryEntity is AppEntity.BaseEntity) effectivePrimaryEntity.isXposedModule else false

    val baseParams = installInfoDialog(
        installer = installer,
        viewModel = viewModel,
        onTitleExtraClick = {
            if (packageName.isNotEmpty()) {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.fromParts("package", packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            viewModel.dispatch(InstallerViewAction.Background)
        }
    )

    return baseParams.copy(
        buttons = dialogButtons(
            DialogParamsType.InstallerInstallSuccess.id
        ) {
            val launchIntent = remember(packageName) {
                if (packageName.isNotEmpty()) {
                    context.packageManager.getLaunchIntentForPackage(packageName)
                } else null
            }

            buildList {
                if (isXposedModule && installer.config.isPrivileged(deviceCapabilityProvider)) {
                    add(DialogButton(stringResource(R.string.open_lsposed)) {
                        coroutineScope.launch(Dispatchers.IO) {
                            val success = openLSPosedUseCase(installer.config)
                            if (success) {
                                withContext(Dispatchers.Main) {
                                    viewModel.dispatch(InstallerViewAction.Close)
                                }
                            }
                        }
                    })
                }

                if (launchIntent != null) {
                    add(DialogButton(stringResource(R.string.open)) {
                        coroutineScope.launch(Dispatchers.IO) {
                            val result = openAppUseCase(
                                config = installer.config,
                                launchIntent = launchIntent
                            )

                            when (result) {
                                is OpenAppUseCase.Result.SuccessPrivileged -> {
                                    withContext(Dispatchers.Main) {
                                        viewModel.dispatch(InstallerViewAction.Close)
                                    }
                                }

                                is OpenAppUseCase.Result.FallbackRequired -> {
                                    withContext(Dispatchers.Main) {
                                        context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

                                        if (installer.config.authorizer == Authorizer.Dhizuku) {
                                            delay(settings.autoCloseCountDown * 1000L)
                                        } else {
                                            delay(PRIVILEGED_START_TIMEOUT_MS)
                                        }
                                        viewModel.dispatch(InstallerViewAction.Close)
                                    }
                                }
                            }
                        }
                    })
                }

                add(DialogButton(stringResource(R.string.finish)) {
                    viewModel.dispatch(InstallerViewAction.Close)
                })
            }
        }
    )
}
