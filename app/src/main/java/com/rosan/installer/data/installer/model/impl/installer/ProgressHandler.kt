package com.rosan.installer.data.installer.model.impl.installer

import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class ProgressHandler(scope: CoroutineScope, installer: InstallerRepo) : Handler(scope, installer) {

    private var job: Job? = null

    override suspend fun onStart() {
        Timber.d("[id=${installer.id}] onStart: Starting to collect progress.")
        job = scope.launch {
            installer.progress.collect {
                // Log all progress changes for debugging
                Timber.d("[id=${installer.id}] Collected progress: ${it::class.simpleName}")
                when (it) {
                    is ProgressEntity.ResolvedFailed -> onResolved(false)
                    is ProgressEntity.ResolveSuccess -> onResolved(true)
                    is ProgressEntity.AnalysedSuccess -> onAnalysedSuccess()
                    else -> {}
                }
            }
        }
    }

    override suspend fun onFinish() {
        Timber.d("[id=${installer.id}] onFinish: Cancelling job.")
        job?.cancel()
    }

    private fun onResolved(success: Boolean) {
        Timber.d("[id=${installer.id}] onResolved called with success: $success")
        val installMode = installer.config.installMode
        if (installMode == ConfigEntity.InstallMode.Notification || installMode == ConfigEntity.InstallMode.AutoNotification) {
            Timber.d("[id=${installer.id}] onResolved: Notification mode detected. Setting background(true).")
            installer.background(true)
        }
        if (success) {
            Timber.d("[id=${installer.id}] onResolved: Success. Triggering analyse().")
            installer.analyse()
        }
    }

    private fun onAnalysedSuccess() {
        Timber.d("[id=${installer.id}] onAnalysedSuccess called.")
        val installMode = installer.config.installMode
        if (installMode != ConfigEntity.InstallMode.AutoDialog && installMode != ConfigEntity.InstallMode.AutoNotification) {
            Timber
                .d("[id=${installer.id}] onAnalysedSuccess: Not an auto-install mode ($installMode). Doing nothing.")
            return
        }

        val isSinglePackage = installer.entities.filter { it.selected }
            .groupBy { it.app.packageName }.keys.size == 1

        if (!isSinglePackage) {
            Timber.d("[id=${installer.id}] onAnalysedSuccess: Not a single package install. Doing nothing.")
            return
        }

        Timber.d("[id=${installer.id}] onAnalysedSuccess: Auto-install conditions met. Triggering install().")
        installer.install()
    }
}