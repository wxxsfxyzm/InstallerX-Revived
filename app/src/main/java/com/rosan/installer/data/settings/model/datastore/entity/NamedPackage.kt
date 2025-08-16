package com.rosan.installer.data.settings.model.datastore.entity

import kotlinx.serialization.Serializable

@Serializable
data class NamedPackage(
    val name: String,
    val packageName: String
)