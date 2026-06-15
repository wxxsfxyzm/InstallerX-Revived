package com.rosan.installer.domain.engine.model.install

import com.rosan.installer.core.device.model.Architecture
import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.engine.model.source.DataType

data class InstallEntity(
    val name: String,
    val packageName: String,
    val sharedUserId: String? = null,
    val arch: Architecture? = null,
    val data: DataEntity,
    val sourceType: DataType,
    val installLocation: Int? = null
)
