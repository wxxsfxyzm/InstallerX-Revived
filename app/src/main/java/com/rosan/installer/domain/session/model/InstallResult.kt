package com.rosan.installer.domain.session.model

data class InstallResult(
    val entity: SelectInstallEntity,
    val success: Boolean,
    val error: Throwable? = null
)