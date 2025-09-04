package com.rosan.installer.data.app.model.entity

import com.rosan.installer.build.Architecture

data class InstallEntity(
    val name: String,
    val packageName: String,
    val sharedUserId: String? = null,
    val arch: Architecture? = null,
    val data: DataEntity,
    val containerType: DataType
)
