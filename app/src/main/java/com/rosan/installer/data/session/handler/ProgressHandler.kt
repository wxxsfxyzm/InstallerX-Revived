// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.data.session.handler

import com.rosan.installer.domain.session.model.ProgressEntity
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.domain.settings.model.InstallMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class ProgressHandler(scope: CoroutineScope, session: InstallerSessionRepository) :
    Handler(scope, session) {

    private var job: Job? = null

    override suspend fun onStart() {
        Timber.d("[id=${session.id}] onStart: Starting to collect progress.")
        job = scope.launch {
            session.progress.collect {
                // Log all progress changes for debugging
                Timber.d("[id=${session.id}] Collected progress: ${it::class.simpleName}")
                when (it) {
                    is ProgressEntity.InstallResolvedFailed -> onResolved(false)
                    is ProgressEntity.InstallResolveSuccess -> onResolved(true)
                    is ProgressEntity.InstallAnalysedSuccess -> onAnalysedSuccess()
                    else -> {}
                }
            }
        }
    }

    override suspend fun onFinish() {
        Timber.d("[id=${session.id}] onFinish: Cancelling job.")
        job?.cancel()
    }

    private fun onResolved(success: Boolean) {
        Timber.d("[id=${session.id}] onResolved called with success: $success")
        val installMode = session.config.installMode
        if (installMode == InstallMode.Notification || installMode == InstallMode.AutoNotification) {
            Timber.d("[id=${session.id}] onResolved: Notification mode detected. Setting background(true).")
            session.background(true)
        }
        if (success) {
            Timber.d("[id=${session.id}] onResolved: Success. Triggering analyse().")
            session.analyse()
        }
    }

    private fun onAnalysedSuccess() {
        Timber.d("[id=${session.id}] onAnalysedSuccess called.")
        val installMode = session.config.installMode
        if (installMode != InstallMode.AutoDialog && installMode != InstallMode.AutoNotification) {
            Timber
                .d("[id=${session.id}] onAnalysedSuccess: Not an auto-install mode ($installMode). Doing nothing.")
            return
        }

        val isSinglePackage = session.analysisResults.size == 1

        if (!isSinglePackage) {
            Timber.d("[id=${session.id}] onAnalysedSuccess: Not a single package install. Doing nothing.")
            return
        }

        Timber.d("[id=${session.id}] onAnalysedSuccess: Auto-install conditions met. Triggering install().")
        session.install(true)
    }
}
