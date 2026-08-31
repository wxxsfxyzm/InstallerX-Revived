// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.session.handler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rosan.installer.data.session.manager.InstallerSessionManagerImpl
import com.rosan.installer.data.session.repository.InstallerSessionRepositoryImpl
import com.rosan.installer.domain.privileged.usecase.OpenAppUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * Receives actions from vivo SystemUI's cross-process PendingIntent sender.
 * The session id is checked before any action is dispatched.
 */
class InstallerActionReceiver :
    BroadcastReceiver(),
    KoinComponent {
    private val sessionManager by inject<InstallerSessionManagerImpl>()
    private val openAppUseCase by inject<OpenAppUseCase>()
    private val receiverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context?, intent: Intent?) {
        val receiverContext = context ?: return
        val receivedIntent = intent ?: return
        if (receivedIntent.action != BroadcastHandler.ACTION) return

        val sessionId = receivedIntent.getStringExtra(BroadcastHandler.KEY_INSTALLER_ID)
        val nameValue = receivedIntent.getStringExtra(BroadcastHandler.KEY_NAME)
        Timber.d(
            "Vivo action receiver received: action=${receivedIntent.action}, id=$sessionId, " +
                "name=$nameValue, package=${receivedIntent.`package`}, " +
                "component=${receivedIntent.component}, flags=0x${receivedIntent.flags.toString(16)}",
        )
        if (sessionId == null) {
            Timber.w("Vivo action ignored: missing session id")
            return
        }
        val session = sessionManager.get(sessionId) ?: run {
            Timber.w("[id=$sessionId] Vivo action ignored: session is no longer active, name=$nameValue")
            return
        }
        val name = nameValue?.let { value ->
            runCatching { BroadcastHandler.Name.revert(value) }.getOrNull()
        } ?: run {
            Timber.w("[id=$sessionId] Vivo action ignored: unknown name=$nameValue")
            return
        }

        Timber.d("[id=$sessionId] Vivo action dispatch started: name=$name")
        val pendingResult = goAsync()
        receiverScope.launch {
            try {
                dispatch(receiverContext, session, name)
                Timber.d("[id=$sessionId] Vivo action dispatch finished: name=$name")
            } catch (t: Throwable) {
                Timber.e(t, "[id=$sessionId] Vivo action dispatch failed: name=$name")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun dispatch(
        context: Context,
        session: InstallerSessionRepositoryImpl,
        name: BroadcastHandler.Name,
    ) {
        when (name) {
            BroadcastHandler.Name.Analyse -> session.analyse()

            BroadcastHandler.Name.Install -> session.install(true)

            BroadcastHandler.Name.Finish -> session.close()

            BroadcastHandler.Name.Cancel -> {
                Timber.d("[id=${session.id}] Vivo Cancel accepted: invoking session.cancel()")
                session.cancel()
                Timber.d("[id=${session.id}] Vivo Cancel accepted: invoking session.close()")
                session.close()
            }

            BroadcastHandler.Name.PrivilegedLaunchAndFinish -> {
                val packageName = session.analysisResults
                    .flatMap { it.appEntities }
                    .firstOrNull { it.selected }
                    ?.app
                    ?.packageName
                    ?: return
                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
                val result = openAppUseCase(session.config, launchIntent)
                if (result is OpenAppUseCase.Result.FallbackRequired) {
                    context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                session.close()
            }

            else -> Timber.d("[id=${session.id}] Vivo action receiver ignored: $name")
        }
    }
}
