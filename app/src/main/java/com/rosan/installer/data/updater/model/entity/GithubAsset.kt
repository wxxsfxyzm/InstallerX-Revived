package com.rosan.installer.data.updater.model.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GithubAsset(
    // Resource file name, such as "app-release.apk"
    val name: String,

    @SerialName("browser_download_url")
    val browserDownloadUrl: String
)