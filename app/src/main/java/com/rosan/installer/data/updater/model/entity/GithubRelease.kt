package com.rosan.installer.data.updater.model.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GithubRelease(
    @SerialName("tag_name")
    val tagName: String,
    @SerialName("prerelease")
    val isPrerelease: Boolean = false,
    @SerialName("html_url")
    val htmlUrl: String? = null,
    val assets: List<GithubAsset> = emptyList()
)