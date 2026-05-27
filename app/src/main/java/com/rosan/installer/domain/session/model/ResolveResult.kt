// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.session.model

import com.rosan.installer.domain.engine.model.source.DataEntity

/**
 * Wrapper class to return both stringified URIs and resolved data
 */
data class ResolveResult(
    val uris: List<String>,
    val data: List<DataEntity>
)
