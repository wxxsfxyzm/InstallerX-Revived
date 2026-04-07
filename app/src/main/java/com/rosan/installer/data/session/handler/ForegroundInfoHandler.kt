// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.data.session.handler

import com.rosan.installer.domain.notification.SessionNotifier
import com.rosan.installer.domain.session.model.ProgressEntity
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.domain.settings.model.InstallMode
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.settings.repository.IntSetting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class ForegroundInfoHandler(
    scope: CoroutineScope,
    session: InstallerSessionRepository
) : Handler(scope, session), KoinComponent {

    private var job: Job? = null

    // Track the auto-close job to allow cancellation if state changes
    private var autoCloseJob: Job? = null
    private val appSettingsRepo by inject<AppSettingsRepository>()
    private val notifier: SessionNotifier by inject { parametersOf(session) }

    override suspend fun onStart() {
        Timber.d("[id=${session.id}] onStart: Starting notification state stream.")

        job = scope.launch {
            combine(session.progress, session.background) { progress, background ->
                Pair(progress, background)
            }.collect { (progress, background) ->

                if (progress is ProgressEntity.InstallAnalysedUnsupported) {
                    notifier.showToast(progress.reason)
                    return@collect
                }

                if (progress is ProgressEntity.InstallAnalysedSuccess && session.config.installMode == InstallMode.AutoNotification) {
                    return@collect
                }

                // Pass the raw state to the framework layer.
                // The framework layer will handle throttling and fake progress internally.
                notifier.updateState(progress, background)

                // Business logic: Auto-close session upon complete success
                val isSuccess = progress is ProgressEntity.InstallSuccess ||
                        (progress is ProgressEntity.InstallCompleted && progress.results.all { it.success })

                if (background && isSuccess) {
                    // Start the countdown in a separate coroutine to avoid blocking the collect stream
                    if (autoCloseJob?.isActive != true) {
                        autoCloseJob = launch {
                            val autoCloseSeconds = appSettingsRepo.getInt(IntSetting.NotificationSuccessAutoClearSeconds, 0).first()
                            if (autoCloseSeconds > 0) {
                                delay(autoCloseSeconds * 1000L)
                                notifier.cancel()
                                session.close()
                            }
                        }
                    }
                } else {
                    // Abort the auto-close if the app returns to the foreground or state is no longer success
                    autoCloseJob?.cancel()
                }
            }
        }
    }

    override suspend fun onFinish() {
        autoCloseJob?.cancel()
        job?.cancel()
        notifier.cleanup()
    }
}
