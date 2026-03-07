// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.data.settings.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "app",
    indices = [
        Index(value = ["package_name"], unique = true),
        Index(value = ["config_id"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = ConfigEntity::class,
            parentColumns = ["id"],
            childColumns = ["config_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AppEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0L,
    @ColumnInfo(name = "package_name") var packageName: String?,
    @ColumnInfo(name = "config_id") var configId: Long,
    @ColumnInfo(name = "created_at") var createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "modified_at") var modifiedAt: Long = System.currentTimeMillis(),
)