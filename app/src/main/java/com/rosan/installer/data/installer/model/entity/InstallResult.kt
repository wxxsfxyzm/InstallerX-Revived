package com.rosan.installer.data.installer.model.entity

data class InstallResult(
    val entity: SelectInstallEntity,
    val success: Boolean,
    val error: Throwable? = null
)