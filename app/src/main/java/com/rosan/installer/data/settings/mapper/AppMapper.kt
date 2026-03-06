// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.settings.mapper

import com.rosan.installer.data.settings.local.room.entity.AppEntity
import com.rosan.installer.domain.settings.model.AppModel

// Extension function to map Entity to Domain Model
fun AppEntity.toDomainModel(): AppModel {
    return AppModel(
        id = this.id,
        packageName = this.packageName,
        configId = this.configId,
        createdAt = this.createdAt,
        modifiedAt = this.modifiedAt
    )
}

// Extension function to map Domain Model to Entity
fun AppModel.toEntity(): AppEntity {
    return AppEntity(
        id = this.id,
        packageName = this.packageName,
        configId = this.configId,
        createdAt = this.createdAt,
        modifiedAt = this.modifiedAt
    )
}
