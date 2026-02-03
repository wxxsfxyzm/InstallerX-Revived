package com.rosan.installer.data.installer.model.impl.installer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.installer.util.pendingActivity
import com.rosan.installer.data.installer.util.pendingBroadcast
import com.rosan.installer.data.recycle.util.openAppPrivileged
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.ui.activity.InstallerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class BroadcastHandler(scope: CoroutineScope, installer: InstallerRepo) :
    Handler(scope, installer), KoinComponent {
    companion object {
        private const val ACTION = "installer.broadcast.action"
        const val KEY_ID = "installer_id"
        private const val KEY_NAME = "name"

        private fun getRequestCode(installer: InstallerRepo, name: Name) =
            "${installer.id}/$name".hashCode()

        fun openIntent(context: Context, installer: InstallerRepo) =
            Intent(context, InstallerActivity::class.java)
                .putExtra(InstallerActivity.KEY_ID, installer.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .pendingActivity(context, getRequestCode(installer, Name.Open))

        fun launchIntent(context: Context, installer: InstallerRepo, intent: Intent) =
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .pendingActivity(context, getRequestCode(installer, Name.Launch))

        fun namedIntent(context: Context, installer: InstallerRepo, name: Name) =
            Intent(ACTION).setPackage(context.packageName)
                .putExtra(KEY_ID, installer.id)
                .putExtra(KEY_NAME, name.value)
                .pendingBroadcast(context, getRequestCode(installer, name))

        fun privilegedLaunchAndFinishIntent(context: Context, installer: InstallerRepo) =
            Intent(ACTION).setPackage(context.packageName)
                .putExtra(KEY_ID, installer.id)
                .putExtra(KEY_NAME, Name.PrivilegedLaunchAndFinish.value)
                .pendingBroadcast(context, getRequestCode(installer, Name.PrivilegedLaunchAndFinish))
    }

    private val context by inject<Context>()
    private val receiver = Receiver(installer)

    override suspend fun onStart() {
        Timber.d("[id=${installer.id}] onStart: Registering receiver.")
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
        Timber.d("[id=${installer.id}] onFinish: Unregistering receiver.")
        context.unregisterReceiver(receiver)
    }

    private class Receiver(private val installer: InstallerRepo) :
        BroadcastReceiver(), KoinComponent {
        private val appDataStore: AppDataStore by inject()
        private val receiverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            if (intent.action != ACTION) return
            context ?: return // Ensure context is not null

            val receivedId = intent.getStringExtra(KEY_ID)
            Timber
                .d("Receiver onReceive: Expected ID=${installer.id}, Received ID=$receivedId, Action=${intent.action}")

            if (receivedId != installer.id) {
                Timber.w("Receiver ID mismatch. Discarding broadcast.")
                return
            }

            val keyName = intent.getStringExtra(KEY_NAME) ?: return
            val name = Name.revert(keyName)
            Timber.d("[id=${installer.id}] Receiver: Received broadcast for name: $name. Dispatching action.")
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
                Name.Analyse -> installer.analyse()
                Name.Install -> installer.install(true)
                Name.Finish -> installer.close()
                Name.Cancel -> installer.cancel()
                Name.PrivilegedLaunchAndFinish -> handlePrivilegedLaunchAndFinish(context)
                else -> {
                    Timber.d("[id=${installer.id}] Receiver: No action for broadcast name: $name")
                }
            }
        }

        private suspend fun handlePrivilegedLaunchAndFinish(context: Context) {
            val packageName = installer.analysisResults
                .flatMap { it.appEntities }
                .firstOrNull { it.selected }?.app?.packageName

            if (packageName == null) {
                Timber.e("[id=${installer.id}] No selected package found for privileged launch.")
                // If we can't open, at least finish the session to not leave it hanging
                installer.close()
                return
            }

            Timber.d("[id=${installer.id}] Starting privileged launch for $packageName")

            val autoCloseSeconds = appDataStore.getInt(AppDataStore.DIALOG_AUTO_CLOSE_COUNTDOWN, 3).first()

            openAppPrivileged(
                context = context,
                config = installer.config,
                packageName = packageName,
                dhizukuAutoCloseSeconds = autoCloseSeconds,
                onSuccess = {
                    Timber.d("[id=${installer.id}] Privileged launch successful, closing session.")
                    receiverScope.launch {
                        installer.close()
                    }
                }
            )
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