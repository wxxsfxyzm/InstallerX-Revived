// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.data.app.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.core.content.IntentCompat
import com.rosan.installer.data.app.model.enums.InstallErrorType
import com.rosan.installer.data.app.model.enums.UninstallErrorType
import com.rosan.installer.data.app.model.exception.InstallException
import com.rosan.installer.data.app.model.exception.UninstallException
import com.rosan.installer.data.app.model.impl.appInstaller.IBinderInstallerRepoImpl

object PackageManagerUtil {
    private const val EXTRA_LEGACY_STATUS = "android.content.pm.extra.LEGACY_STATUS"

    /**
     * Flag parameter to indicate keeping the package's data directory.
     */
    const val DELETE_KEEP_DATA = 0x00000001

    /**
     * Flag parameter to indicate deleting the package for all users.
     */
    const val DELETE_ALL_USERS = 0x00000002

    /**
     * Flag parameter to mark the app as uninstalled for the current user only.
     */
    const val DELETE_SYSTEM_APP = 0x00000004

    suspend fun installResultVerify(
        context: Context,
        receiver: IBinderInstallerRepoImpl.LocalIntentReceiver
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

        val legacyStatus = intent.getIntExtra(EXTRA_LEGACY_STATUS, PackageInstaller.STATUS_FAILURE)
        val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        val ecpMsg = "Install Failure $status#$legacyStatus [$msg]"

        // Resolve error type dynamically and throw unified exception
        val errorType = InstallErrorType.fromLegacyCode(legacyStatus)
        throw InstallException(errorType, ecpMsg)
    }

    suspend fun uninstallResultVerify(
        context: Context,
        receiver: IBinderInstallerRepoImpl.LocalIntentReceiver
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

        val legacyStatus = intent.getIntExtra(EXTRA_LEGACY_STATUS, 0)
        val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        val ecpMsg = "Uninstall Failure $status#$legacyStatus [$msg]"

        // Resolve error type dynamically and throw unified exception
        val errorType = UninstallErrorType.fromLegacyCode(legacyStatus)
        throw UninstallException(errorType, ecpMsg)
    }
}