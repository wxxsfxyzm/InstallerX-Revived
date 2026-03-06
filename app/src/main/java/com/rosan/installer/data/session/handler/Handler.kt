// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.data.session.handler

import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import kotlinx.coroutines.CoroutineScope

abstract class Handler(val scope: CoroutineScope, open val installer: InstallerSessionRepository) {
    abstract suspend fun onStart()

    abstract suspend fun onFinish()
}