// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.data.session.handler

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.rosan.installer.core.app.ActivityContracts
import com.rosan.installer.data.session.util.pendingActivity
import com.rosan.installer.data.session.util.pendingBroadcast
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import timber.log.Timber

class BroadcastHandler {
    companion object {
        internal const val ACTION = "installer.broadcast.action"
        internal const val KEY_INSTALLER_ID = com.rosan.installer.core.app.ActivityContracts.KEY_INSTALLER_ID
        internal const val KEY_NAME = "name"

        private fun getRequestCode(installer: InstallerSessionRepository, name: Name) = "${installer.id}/$name".hashCode()

        fun openIntent(context: Context, installer: InstallerSessionRepository) = Intent()
            .setClassName(context.packageName, ActivityContracts.INSTALLER_ACTIVITY)
            .putExtra(ActivityContracts.KEY_INSTALLER_ID, installer.id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .pendingActivity(context, getRequestCode(installer, Name.Open))

        fun launchIntent(context: Context, installer: InstallerSessionRepository, intent: Intent) = intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .pendingActivity(context, getRequestCode(installer, Name.Launch))

        fun namedIntent(
            context: Context,
            installer: InstallerSessionRepository,
            name: Name,
        ): PendingIntent {
            val requestCode = getRequestCode(installer, name)
            Timber.d(
                "[id=${installer.id}] Creating action PendingIntent: name=$name, " +
                    "requestCode=$requestCode, receiver=${InstallerActionReceiver::class.java.name}",
            )
            return Intent(context, InstallerActionReceiver::class.java)
                .setAction(ACTION)
                .putExtra(KEY_INSTALLER_ID, installer.id)
                .putExtra(KEY_NAME, name.value)
                .pendingBroadcast(context, requestCode)
        }

        fun privilegedLaunchAndFinishIntent(
            context: Context,
            installer: InstallerSessionRepository,
        ): PendingIntent {
            val requestCode = getRequestCode(installer, Name.PrivilegedLaunchAndFinish)
            Timber.d(
                "[id=${installer.id}] Creating action PendingIntent: name=${Name.PrivilegedLaunchAndFinish}, " +
                    "requestCode=$requestCode, receiver=${InstallerActionReceiver::class.java.name}",
            )
            return Intent(context, InstallerActionReceiver::class.java)
                .setAction(ACTION)
                .putExtra(KEY_INSTALLER_ID, installer.id)
                .putExtra(KEY_NAME, Name.PrivilegedLaunchAndFinish.value)
                .pendingBroadcast(context, requestCode)
        }
    }

    enum class Name(val value: String) {
        Open("open"),
        Analyse("analyse"),
        Install("install"),
        Finish("finish"),
        Cancel("cancel"),
        Launch("launch"),
        PrivilegedLaunchAndFinish("privileged_launch_and_finish"),
        ;

        companion object {
            fun revert(value: String): Name = entries.first { it.value == value }
        }
    }
}
