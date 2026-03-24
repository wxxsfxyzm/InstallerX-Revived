package com.rosan.installer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.rosan.installer.ui.activity.SettingsActivity
import timber.log.Timber

class SecretCodeReceiver : BroadcastReceiver() {
    companion object {
        const val SECRET_CODE_ACTION_OLD = "android.provider.Telephony.SECRET_CODE"
        const val SECRET_CODE_ACTION = "android.telephony.action.SECRET_CODE"
    }

    // This method is called when the BroadcastReceiver receives an Intent broadcast.
    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("onReceive: $intent, action: ${intent.action}")

        // --- Check if the received intent's action is the one we expect. ---
        val isSecretCodeAction =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) intent.action == SECRET_CODE_ACTION
            else intent.action == SECRET_CODE_ACTION_OLD
        if (isSecretCodeAction) {
            context.startActivity(SettingsActivity.createLaunchIntent(context))
        } else {
            // Log a warning if the action does not match.
            Timber.w("Received an intent with unexpected action: ${intent.action}")
        }
    }
}
