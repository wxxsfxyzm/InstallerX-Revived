// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.packageupdate.repository

import com.rosan.installer.domain.packageupdate.model.PendingSelfUpdate
import com.rosan.installer.domain.packageupdate.model.PendingSelfUpdateHistory
import com.rosan.installer.domain.packageupdate.model.PendingSourceDeletion
import com.rosan.installer.domain.settings.model.config.Authorizer

interface SelfUpdateRecoveryRepository {
    suspend fun arm(update: PendingSelfUpdate)

    suspend fun getPendingUpdate(): PendingSelfUpdate?

    suspend fun updatePostInstallState(sessionId: String, sourceDeletion: PendingSourceDeletion?, historyAuthorizer: Authorizer)

    suspend fun clear(sessionId: String)

    suspend fun clearAll()

    suspend fun markCompletionNoticePending()

    suspend fun consumeCompletionNotice(): Boolean

    suspend fun getCompletedSourceDeletion(): PendingSourceDeletion?

    suspend fun clearCompletedSourceDeletion()

    suspend fun getCompletedHistory(): PendingSelfUpdateHistory?

    suspend fun clearCompletedHistory()
}
