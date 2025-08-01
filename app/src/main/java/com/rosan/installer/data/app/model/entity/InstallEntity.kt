package com.rosan.installer.data.app.model.entity

data class InstallEntity(
    val name: String,
    val packageName: String,
    val data: DataEntity,
    val containerType: DataType
)
