// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.data.settings.local.room.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "app",
    indices = [
        Index(value = ["package_name"], unique = true),
        Index(value = ["config_id"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = ConfigEntity::class,
            parentColumns = ["id"],
            childColumns = ["config_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
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
