// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.session.resolver

import android.net.Uri
import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.session.model.ProgressEntity
import com.rosan.installer.domain.session.repository.NetworkResolver
import kotlinx.coroutines.flow.MutableSharedFlow

class OfflineNetworkResolver : NetworkResolver {
    override suspend fun resolve(
        uri: Uri,
        cacheDirectory: String,
        progressFlow: MutableSharedFlow<ProgressEntity>
    ): List<DataEntity> {
        throw UnsupportedOperationException("Network resolution is strictly disabled in the offline build.")
    }
}