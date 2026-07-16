// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.framework.packageupdate

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.os.SystemClock
import androidx.annotation.RequiresApi
import com.rosan.installer.domain.packageupdate.model.PendingSelfUpdate
import com.rosan.installer.domain.packageupdate.repository.SelfUpdateRecoveryRepository
import com.rosan.installer.domain.privileged.model.PostInstallTaskInfo
import com.rosan.installer.domain.privileged.provider.PostInstallTaskProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Coordinates Android 17 self-update state with WM Shell package-update recovery.
 */
class SelfUpdateRecoveryManager(
    context: Context,
    private val recoveryRepository: SelfUpdateRecoveryRepository,
    private val postInstallTaskProvider: PostInstallTaskProvider
) {
    private val appContext = context.applicationContext
    private val sourceDeletionMutex = Mutex()

    suspend fun arm(sessionId: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN) return false

        val packageInfo = runCatching {
            appContext.packageManager.getPackageInfo(appContext.packageName, 0)
        }.getOrElse { error ->
            Timber.w(error, "Unable to read package state before self-update.")
            return false
        }

        val pendingUpdate = PendingSelfUpdate(
            sessionId = sessionId,
            previousUpdateTime = packageInfo.lastUpdateTime,
            armedAtElapsed = SystemClock.elapsedRealtime()
        )
        return try {
            // DataStore edit returns only after the transaction is durably persisted.
            recoveryRepository.arm(pendingUpdate)
            Timber.i("Armed Android 17 self-update recovery for session $sessionId.")
            true
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Timber.w(error, "Failed to persist Android 17 self-update recovery for session $sessionId.")
            false
        }
    }

    suspend fun clear(sessionId: String) {
        try {
            recoveryRepository.clear(sessionId)
            Timber.d("Cleared Android 17 self-update recovery for session $sessionId.")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Timber.w(error, "Failed to clear Android 17 self-update recovery for session $sessionId.")
        }
    }

    suspend fun consumeCompletionNotice(): Boolean = try {
        recoveryRepository.consumeCompletionNotice()
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Timber.w(error, "Failed to consume Android 17 self-update completion notice.")
        false
    }

    suspend fun consumeSystemUiRecovery(
        launchedFromUid: Int,
        platformReferrerPackage: String?,
        intentFlags: Int
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN) return false
        if (intentFlags and Intent.FLAG_ACTIVITY_CLEAR_TASK == 0) {
            Timber.d("Ignoring Android 17 self-update recovery without FLAG_ACTIVITY_CLEAR_TASK.")
            return false
        }

        // Android 17 WM Shell restores the task through a SystemUI PendingIntent without sharing
        // caller identity. launchedFromUid can therefore be unavailable even for a genuine replay;
        // the sanitized platform referrer is the required fallback for that AOSP path.
        val launchedFromPackages = if (launchedFromUid == Process.INVALID_UID) {
            emptyList()
        } else {
            appContext.packageManager.getPackagesForUid(launchedFromUid)?.filterNotNull().orEmpty()
        }
        val launchedFromSystemUi = SYSTEM_UI_PACKAGE_NAME in launchedFromPackages
        val referredBySystemUi = platformReferrerPackage == SYSTEM_UI_PACKAGE_NAME
        if (!launchedFromSystemUi && !referredBySystemUi) {
            Timber.w(
                "Ignoring Android 17 self-update recovery from uid=$launchedFromUid, " +
                        "packages=$launchedFromPackages, platformReferrer=$platformReferrerPackage."
            )
            return false
        }
        Timber.d(
            "Accepted Android 17 self-update recovery source: uid=$launchedFromUid, " +
                    "packages=$launchedFromPackages, platformReferrer=$platformReferrerPackage."
        )

        val pendingUpdate = readPendingUpdate()
            ?: return consumeRecentPackageUpdateExit()
        val now = SystemClock.elapsedRealtime()
        if (pendingUpdate.armedAtElapsed !in 1..now ||
            now - pendingUpdate.armedAtElapsed > RECOVERY_TIMEOUT_MILLIS
        ) {
            clearPendingUpdate()
            Timber.w(
                "Discarded expired Android 17 self-update recovery for session " +
                        "${pendingUpdate.sessionId}."
            )
            return consumeRecentPackageUpdateExit()
        }

        val packageInfo = runCatching {
            appContext.packageManager.getPackageInfo(appContext.packageName, 0)
        }.getOrElse { error ->
            Timber.w(error, "Unable to verify package state after self-update.")
            return false
        }

        if (pendingUpdate.previousUpdateTime <= 0L) {
            clearPendingUpdate()
            return consumeRecentPackageUpdateExit(packageInfo.lastUpdateTime)
        }
        if (packageInfo.lastUpdateTime == pendingUpdate.previousUpdateTime) {
            Timber.w(
                "Ignoring Android 17 self-update recovery because package lastUpdateTime did not change."
            )
            return false
        }

        markRecoveryCompleted()
        Timber.i("Consumed Android 17 self-update restart for session ${pendingUpdate.sessionId}.")
        return true
    }

    suspend fun deleteCompletedSource() = sourceDeletionMutex.withLock {
        val deletion = try {
            recoveryRepository.getCompletedSourceDeletion()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Timber.w(error, "Unable to read completed self-update source deletion.")
            return@withLock
        } ?: return@withLock

        try {
            withContext(Dispatchers.IO) {
                postInstallTaskProvider.executeTasks(
                    authorizer = deletion.authorizer,
                    customizeAuthorizer = deletion.customizeAuthorizer,
                    info = PostInstallTaskInfo(
                        packageName = appContext.packageName,
                        enableAutoDelete = true,
                        deletePaths = deletion.paths
                    )
                )
            }
            Timber.i("Requested deletion of persisted self-update source paths: ${deletion.paths}")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            // Source cleanup is best-effort and must not suppress a verified update recovery.
            Timber.w(error, "Failed to delete persisted self-update source paths.")
        }

        try {
            recoveryRepository.clearCompletedSourceDeletion()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Timber.w(error, "Failed to clear completed self-update source deletion.")
        }
    }

    private suspend fun readPendingUpdate(): PendingSelfUpdate? = try {
        recoveryRepository.getPendingUpdate()
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Timber.w(error, "Unable to read pending Android 17 self-update recovery state.")
        null
    }

    @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    private suspend fun consumeRecentPackageUpdateExit(lastUpdateTime: Long? = null): Boolean {
        val packageUpdateTime = lastUpdateTime ?: runCatching {
            appContext.packageManager.getPackageInfo(appContext.packageName, 0).lastUpdateTime
        }.getOrElse { error ->
            Timber.w(error, "Unable to read package state for Android 17 update recovery fallback.")
            return false
        }

        // Covers a valid SystemUI recovery when the pre-update record was unavailable, while the
        // package-update exit reason and timestamp still prove that replacement completed.
        val exitReasons = runCatching {
            appContext.getSystemService(ActivityManager::class.java)
                .getHistoricalProcessExitReasons(appContext.packageName, 0, MAX_EXIT_REASONS)
        }.getOrElse { error ->
            Timber.w(error, "Unable to read process exit reasons for Android 17 update recovery.")
            return false
        }

        val packageUpdateExit = exitReasons.any { exitInfo ->
            exitInfo.reason == ApplicationExitInfo.REASON_PACKAGE_UPDATED &&
                    packageUpdateTime >= exitInfo.timestamp &&
                    packageUpdateTime - exitInfo.timestamp <= RECOVERY_TIMEOUT_MILLIS
        }
        if (packageUpdateExit) {
            markRecoveryCompleted()
            Timber.i("Consumed Android 17 package-update restart using process exit history.")
        }
        return packageUpdateExit
    }

    private suspend fun markRecoveryCompleted() {
        // The isolated InstallerActivity task is replaced by the normal app task, so keep this
        // notice durable until SettingsActivity consumes it after the task handoff.
        try {
            recoveryRepository.markCompletionNoticePending()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            // Recovery must still intercept the incomplete URI intent even if its toast is lost.
            Timber.w(error, "Failed to persist Android 17 self-update completion notice.")
        }
    }

    private suspend fun clearPendingUpdate() {
        try {
            recoveryRepository.clearAll()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Timber.w(error, "Failed to clear pending Android 17 self-update recovery state.")
        }
    }

    private companion object {
        const val SYSTEM_UI_PACKAGE_NAME = "com.android.systemui"
        const val RECOVERY_TIMEOUT_MILLIS = 30 * 60 * 1000L
        const val MAX_EXIT_REASONS = 5
    }
}
