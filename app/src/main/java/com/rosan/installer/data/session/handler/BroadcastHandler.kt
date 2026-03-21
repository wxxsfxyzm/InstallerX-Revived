// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.data.session.handler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.rosan.installer.data.session.util.pendingActivity
import com.rosan.installer.data.session.util.pendingBroadcast
import com.rosan.installer.domain.privileged.usecase.OpenAppUseCase
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.ui.activity.InstallerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class BroadcastHandler(scope: CoroutineScope, session: InstallerSessionRepository) :
    Handler(scope, session), KoinComponent {
    companion object {
        private const val ACTION = "installer.broadcast.action"
        const val KEY_ID = "installer_id"
        private const val KEY_NAME = "name"

        private fun getRequestCode(installer: InstallerSessionRepository, name: Name) =
            "${installer.id}/$name".hashCode()

        fun openIntent(context: Context, installer: InstallerSessionRepository) =
            Intent(context, InstallerActivity::class.java)
                .putExtra(InstallerActivity.KEY_ID, installer.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .pendingActivity(context, getRequestCode(installer, Name.Open))

        fun launchIntent(context: Context, installer: InstallerSessionRepository, intent: Intent) =
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .pendingActivity(context, getRequestCode(installer, Name.Launch))

        fun namedIntent(context: Context, installer: InstallerSessionRepository, name: Name) =
            Intent(ACTION).setPackage(context.packageName)
                .putExtra(KEY_ID, installer.id)
                .putExtra(KEY_NAME, name.value)
                .pendingBroadcast(context, getRequestCode(installer, name))

        fun privilegedLaunchAndFinishIntent(context: Context, installer: InstallerSessionRepository) =
            Intent(ACTION).setPackage(context.packageName)
                .putExtra(KEY_ID, installer.id)
                .putExtra(KEY_NAME, Name.PrivilegedLaunchAndFinish.value)
                .pendingBroadcast(context, getRequestCode(installer, Name.PrivilegedLaunchAndFinish))
    }

    private val context by inject<Context>()
    private val openAppUseCase by inject<OpenAppUseCase>()

    private val receiver = Receiver()

    override suspend fun onStart() {
        Timber.d("[id=${session.id}] onStart: Registering receiver.")
        registerReceiver(receiver)
    }

    private fun registerReceiver(receiver: Receiver) {
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override suspend fun onFinish() {
        Timber.d("[id=${session.id}] onFinish: Unregistering receiver.")
        context.unregisterReceiver(receiver)
    }

    private inner class Receiver : BroadcastReceiver() {
        private val receiverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            if (intent.action != ACTION) return
            context ?: return // Ensure context is not null

            val receivedId = intent.getStringExtra(KEY_ID)
            Timber
                .d("Receiver onReceive: Expected ID=${session.id}, Received ID=$receivedId, Action=${intent.action}")

            if (receivedId != session.id) {
                Timber.w("Receiver ID mismatch. Discarding broadcast.")
                return
            }

            val keyName = intent.getStringExtra(KEY_NAME) ?: return
            val name = Name.revert(keyName)
            Timber.d("[id=${session.id}] Receiver: Received broadcast for name: $name. Dispatching action.")
            val pendingResult = goAsync()
            receiverScope.launch {
                try {
                    doWork(context, name)
                } finally {
                    pendingResult.finish()
                }
            }
        }

        private suspend fun doWork(context: Context, name: Name) {
            when (name) {
                Name.Analyse -> session.analyse()
                Name.Install -> session.install(true)
                Name.Finish -> session.close()
                Name.Cancel -> session.cancel()
                Name.PrivilegedLaunchAndFinish -> handlePrivilegedLaunchAndFinish(context)
                else -> {
                    Timber.d("[id=${session.id}] Receiver: No action for broadcast name: $name")
                }
            }
        }

        private suspend fun handlePrivilegedLaunchAndFinish(context: Context) {
            val packageName = session.analysisResults
                .flatMap { it.appEntities }
                .firstOrNull { it.selected }?.app?.packageName ?: return

            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return

            val result = openAppUseCase(session.config, launchIntent)

            if (result is OpenAppUseCase.Result.FallbackRequired) {
                Timber.d("Privileged launch fallback: starting activity normally.")
                context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }

            Timber.d("Privileged launch result processed, closing session.")
            session.close()
        }
    }

    enum class Name(val value: String) {
        Open("open"),
        Analyse("analyse"),
        Install("install"),
        Finish("finish"),
        Cancel("cancel"),
        Launch("launch"),
        PrivilegedLaunchAndFinish("privileged_launch_and_finish");

        companion object {
            fun revert(value: String): Name = entries.first { it.value == value }
        }
    }
}
