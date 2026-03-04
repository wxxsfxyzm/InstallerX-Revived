package com.rosan.installer.domain.settings.model

import kotlinx.serialization.Serializable

@Serializable
data class NamedPackage(
    val name: String,
    val packageName: String
)