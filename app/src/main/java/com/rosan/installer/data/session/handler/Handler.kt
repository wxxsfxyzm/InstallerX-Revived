// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.data.session.handler

import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import kotlinx.coroutines.CoroutineScope

interface Handler {
    val scope: CoroutineScope
    val session: InstallerSessionRepository

    suspend fun onStart()

    suspend fun onFinish()
}
