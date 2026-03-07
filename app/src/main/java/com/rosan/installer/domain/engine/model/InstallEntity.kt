package com.rosan.installer.domain.engine.model

import com.rosan.installer.domain.device.model.Architecture

data class InstallEntity(
    val name: String,
    val packageName: String,
    val sharedUserId: String? = null,
    val arch: Architecture? = null,
    val data: DataEntity,
    val sourceType: DataType
)
