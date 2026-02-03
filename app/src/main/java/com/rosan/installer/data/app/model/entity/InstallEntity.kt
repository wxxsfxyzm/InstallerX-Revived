package com.rosan.installer.data.app.model.entity

import com.rosan.installer.build.model.entity.Architecture
import com.rosan.installer.data.app.model.enums.DataType

data class InstallEntity(
    val name: String,
    val packageName: String,
    val sharedUserId: String? = null,
    val arch: Architecture? = null,
    val data: DataEntity,
    val sourceType: DataType
)
