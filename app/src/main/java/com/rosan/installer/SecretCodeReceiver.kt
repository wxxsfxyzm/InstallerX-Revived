package com.rosan.installer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rosan.installer.ui.activity.SettingsActivity
import timber.log.Timber

class SecretCodeReceiver : BroadcastReceiver() {
    companion object {
        const val SECRET_CODE_ACTION = "android.provider.Telephony.SECRET_CODE"
    }

    // This method is called when the BroadcastReceiver receives an Intent broadcast.
    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("onReceive: $intent, action: ${intent.action}")

        // --- Check if the received intent's action is the one we expect. ---
        if (intent.action == SECRET_CODE_ACTION) {
            // The action is correct, proceed to launch the activity.
            // Create an intent to launch the SettingsActivity.
            val i = Intent(context, SettingsActivity::class.java).apply {
                // Since we are starting an Activity from a non-Activity context,
                // we must set the FLAG_ACTIVITY_NEW_TASK flag.
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            // Start the activity.
            context.startActivity(i)
        } else {
            // Log a warning if the action does not match.
            Timber.w("Received an intent with unexpected action: ${intent.action}")
        }
    }
}