// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.provider

import android.os.Bundle
import com.rosan.installer.domain.settings.model.config.ConfigModel

interface SessionDetailsProvider {
    fun getSessionDetails(sessionId: Int, config: ConfigModel): Bundle?
}
