// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer.dialog.inner

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    session: InstallerSessionRepository,
    viewModel: InstallerViewModel
): DialogParams {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val deviceCapabilityProvider: DeviceCapabilityProvider = koinInject()
    val currentPackageName = uiState.currentPackageName
    val coroutineScope = rememberCoroutineScope()
    val settings = uiState.viewSettings

    val openAppUseCase: OpenAppUseCase = koinInject()
    val openLSPosedUseCase: OpenLSPosedUseCase = koinInject()

    val packageName = currentPackageName ?: session.analysisResults.firstOrNull()?.packageName ?: ""
    val currentPackage = session.analysisResults.find { it.packageName == packageName }

    val selectedEntities = currentPackage?.appEntities
        ?.filter { it.selected }
        ?.map { it.app }
    val effectivePrimaryEntity = selectedEntities?.filterIsInstance<AppEntity.BaseEntity>()?.firstOrNull()
        ?: selectedEntities?.filterIsInstance<AppEntity.ModuleEntity>()?.firstOrNull()

    val isXposedModule = if (effectivePrimaryEntity is AppEntity.BaseEntity) effectivePrimaryEntity.isXposedModule else false

    val baseParams = installInfoDialog(
        session = session,
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
                if (isXposedModule && session.config.isPrivileged(deviceCapabilityProvider)) {
                    add(DialogButton(stringResource(R.string.open_lsposed)) {
                        coroutineScope.launch(Dispatchers.IO) {
                            val success = openLSPosedUseCase(session.config)
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
                                config = session.config,
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

                                        if (session.config.authorizer == Authorizer.Dhizuku) {
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
