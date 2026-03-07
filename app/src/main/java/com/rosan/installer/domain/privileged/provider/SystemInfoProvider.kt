// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.privileged.provider

import android.os.Bundle
import com.rosan.installer.domain.settings.model.Authorizer

interface SystemInfoProvider {
    suspend fun getUsers(authorizer: Authorizer): Map<Int, String>
    suspend fun getSessionDetails(authorizer: Authorizer, sessionId: Int): Bundle?
}
