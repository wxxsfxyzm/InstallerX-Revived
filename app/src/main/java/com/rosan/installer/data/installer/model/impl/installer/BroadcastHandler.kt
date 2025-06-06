package com.rosan.installer.data.installer.model.impl.installer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.installer.util.pendingActivity
import com.rosan.installer.data.installer.util.pendingBroadcast
import com.rosan.installer.ui.activity.InstallerActivity
import kotlinx.coroutines.CoroutineScope
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

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
    }

    private val context by inject<Context>()

    private val receiver = Receiver(installer)

    override suspend fun onStart() {
        registerReceiver(receiver)
    }

    private fun registerReceiver(receiver: Receiver) {
        // ContextCompat 会自动处理 API 版本的差异
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    // No longer needed as ContextCompat handles it
    /*@RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun registerReceiverTiramisu(receiver: Receiver) {
        context.registerReceiver(
            receiver,
            IntentFilter(ACTION),
            Context.RECEIVER_NOT_EXPORTED
        )
    }*/

    override suspend fun onFinish() {
        context.unregisterReceiver(receiver)
    }

    private class Receiver(private val installer: InstallerRepo) : BroadcastReceiver(),
        KoinComponent {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            if (intent.action != ACTION) return
            if (intent.getStringExtra(KEY_ID) != installer.id) return
            val keyName = intent.getStringExtra(KEY_NAME) ?: return
            val name = Name.revert(keyName)
            doWork(name)
        }

        private fun doWork(name: Name) {
            when (name) {
                Name.Analyse -> installer.analyse()
                Name.Install -> installer.install()
                Name.Finish -> installer.close()
                else -> {}
            }
        }
    }

    enum class Name(val value: String) {
        Open("open"),
        Analyse("analyse"),
        Install("install"),
        Finish("finish"),
        Launch("launch");

        companion object {
            fun revert(value: String): Name = entries.first { it.value == value }
        }
    }
}