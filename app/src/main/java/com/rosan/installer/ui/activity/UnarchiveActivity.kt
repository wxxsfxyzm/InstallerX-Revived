// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.activity

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstallerHidden
import android.content.pm.PackageInstallerHidden.ACTION_UNARCHIVE_DIALOG
import android.content.pm.PackageInstallerHidden.ACTION_UNARCHIVE_ERROR_DIALOG
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.core.content.IntentCompat
import com.rosan.installer.domain.archive.model.UnarchiveStatus
import com.rosan.installer.domain.session.model.ProgressEntity
import com.rosan.installer.domain.session.model.UnarchiveErrorInfo
import com.rosan.installer.domain.session.repository.InstallerSessionManager
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
class UnarchiveActivity : ComponentActivity() {
    private val sessionManager: InstallerSessionManager by inject()
    private var session: InstallerSessionRepository? = null
    private var job: Job? = null
    private var sessionClosed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val resolvedSession = sessionManager.getOrCreate(null)
        session = resolvedSession
        collectSession(resolvedSession)

        if (!handleIntent(intent, resolvedSession)) return

        setContent {
            InstallerActivityContent(session = resolvedSession)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
        handleIntent(intent, session ?: sessionManager.getOrCreate(null).also { session = it })
    }

    override fun onStop() {
        super.onStop()

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (!powerManager.isInteractive) {
            Timber.d("onStop: Screen is turned off. Ignoring.")
            return
        }

        if (!isFinishing && !isChangingConfigurations) {
            Timber.d("onStop: User left UnarchiveActivity. Closing repository.")
            closeSession()
        }
    }

    override fun onDestroy() {
        job?.cancel()
        job = null
        Timber.d("UnarchiveActivity is being destroyed.")
        super.onDestroy()
    }

    private fun handleIntent(intent: Intent, session: InstallerSessionRepository): Boolean =
        when (intent.action) {
            ACTION_UNARCHIVE_DIALOG -> resolveUnarchive(intent, session)
            ACTION_UNARCHIVE_ERROR_DIALOG -> resolveUnarchiveError(intent, session)
            else -> {
                Timber.e("Unsupported unarchive action: ${intent.action}")
                finishAndCloseSession()
                false
            }
        }

    private fun resolveUnarchive(intent: Intent, session: InstallerSessionRepository): Boolean {
        val packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME)
        val sender = IntentCompat.getParcelableExtra(
            intent,
            PackageInstallerHidden.EXTRA_UNARCHIVE_INTENT_SENDER,
            IntentSender::class.java
        )
        if (packageName.isNullOrBlank() || sender == null) {
            Timber.e("Unarchive dialog missing package name or intent sender.")
            finishAndCloseSession()
            return false
        }

        session.resolveUnarchive(this, packageName, sender)
        return true
    }

    private fun resolveUnarchiveError(intent: Intent, session: InstallerSessionRepository): Boolean {
        val info = UnarchiveErrorInfo(
            status = UnarchiveStatus.fromPlatformStatus(
                intent.getIntExtra(
                    PackageInstallerHidden.EXTRA_UNARCHIVE_STATUS,
                    UnarchiveStatus.GENERIC_ERROR
                )
            ),
            requiredBytes = intent.getLongExtra(PackageInstallerHidden.EXTRA_UNARCHIVE_REQUIRED_BYTES, 0L),
            pendingIntent = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, PendingIntent::class.java),
            installerPackageName = intent.getStringExtra(PackageInstallerHidden.EXTRA_UNARCHIVE_INSTALLER_PACKAGE_NAME),
            installerLabel = intent.getStringExtra(PackageInstallerHidden.EXTRA_UNARCHIVE_INSTALLER_TITLE)
        )

        session.resolveUnarchiveError(this, info)
        return true
    }

    private fun collectSession(session: InstallerSessionRepository) {
        job?.cancel()
        val scope = CoroutineScope(Dispatchers.Main.immediate)
        job = scope.launch {
            session.progress.collect { progress ->
                if (progress is ProgressEntity.Finish) finishAndCloseSession()
            }
        }
    }

    private fun finishAndCloseSession() {
        closeSession()
        if (!isFinishing) finish()
    }

    private fun closeSession() {
        if (sessionClosed) return
        sessionClosed = true
        session?.close()
    }
}
