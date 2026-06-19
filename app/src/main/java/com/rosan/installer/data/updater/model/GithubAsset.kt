// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.updater.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GithubAsset(
    // Resource file name, such as "app-release.apk"
    val name: String,

    @SerialName("browser_download_url")
    val browserDownloadUrl: String
)
