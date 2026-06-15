// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.data.engine.executor

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstallerHidden
import androidx.core.content.IntentCompat
import com.rosan.installer.data.engine.executor.appinstaller.LocalIntentReceiver
import com.rosan.installer.domain.engine.exception.InstallException
import com.rosan.installer.domain.engine.exception.UninstallException
import com.rosan.installer.domain.engine.model.error.InstallErrorType
import com.rosan.installer.domain.engine.model.error.UninstallErrorType

object PackageManagerUtil {
    suspend fun installResultVerify(
        context: Context,
        receiver: LocalIntentReceiver
    ) {
        val intent = receiver.getResult()
        val status =
            intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val action =
            IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)

        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION && action != null) {
            context.startActivity(action.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            installResultVerify(context, receiver)
            return
        }

        if (status == PackageInstaller.STATUS_SUCCESS) return

        val legacyStatus = intent.getIntExtra(
            PackageInstallerHidden.EXTRA_LEGACY_STATUS,
            PackageInstaller.STATUS_FAILURE
        )
        val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        val ecpMsg = "Install Failure $status#$legacyStatus [$msg]"

        // Resolve error type dynamically and throw unified exception
        val errorType = InstallErrorType.fromLegacyCode(legacyStatus)
        throw InstallException(errorType, ecpMsg)
    }

    suspend fun uninstallResultVerify(
        context: Context,
        receiver: LocalIntentReceiver
    ) {
        val intent = receiver.getResult()
        val status =
            intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val action =
            IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)

        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION && action != null) {
            context.startActivity(action.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            uninstallResultVerify(context, receiver)
            return
        }

        if (status == PackageInstaller.STATUS_SUCCESS) return

        val legacyStatus = intent.getIntExtra(PackageInstallerHidden.EXTRA_LEGACY_STATUS, 0)
        val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        val ecpMsg = "Uninstall Failure $status#$legacyStatus [$msg]"

        // Resolve error type dynamically and throw unified exception
        val errorType = UninstallErrorType.fromLegacyCode(legacyStatus)
        throw UninstallException(errorType, ecpMsg)
    }
}
