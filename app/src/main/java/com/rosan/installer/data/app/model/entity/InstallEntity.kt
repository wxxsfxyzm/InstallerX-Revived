package com.rosan.installer.data.app.model.entity

import com.rosan.installer.data.app.util.DataType

data class InstallEntity(
    val name: String,
    val packageName: String,
    val data: DataEntity,
    val containerType: DataType
)
