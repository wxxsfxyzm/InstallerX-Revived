// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.packageupdate.repository

import com.rosan.installer.domain.packageupdate.model.PendingSelfUpdate
import com.rosan.installer.domain.packageupdate.model.PendingSourceDeletion

interface SelfUpdateRecoveryRepository {
    suspend fun arm(update: PendingSelfUpdate)

    suspend fun getPendingUpdate(): PendingSelfUpdate?

    suspend fun updateSourceDeletion(sessionId: String, sourceDeletion: PendingSourceDeletion?)

    suspend fun clear(sessionId: String)

    suspend fun clearAll()

    suspend fun markCompletionNoticePending()

    suspend fun consumeCompletionNotice(): Boolean

    suspend fun getCompletedSourceDeletion(): PendingSourceDeletion?

    suspend fun clearCompletedSourceDeletion()
}
