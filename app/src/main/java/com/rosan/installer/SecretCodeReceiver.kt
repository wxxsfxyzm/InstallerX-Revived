package com.rosan.installer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rosan.installer.ui.activity.SettingsActivity
import timber.log.Timber

class SecretCodeReceiver : BroadcastReceiver() {

    // This method is called when the BroadcastReceiver receives an Intent broadcast
    // matching its intent filter.
    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("onReceive: $intent")
        // Create an intent to launch the SettingsActivity.
        // The target activity is explicitly defined.
        val i = Intent(context, SettingsActivity::class.java).apply {
            // Since we are starting an Activity from a BroadcastReceiver context,
            // which is not an Activity context, we must set the FLAG_ACTIVITY_NEW_TASK flag.
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // Start the activity.
        context.startActivity(i)
    }
}