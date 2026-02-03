package com.rosan.installer.ui.page.main.settings.config.apply

data class ApplyViewApp(
    val packageName: String,
    val versionName: String?,
    val versionCode: Long,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val isSystemApp: Boolean,
    val label: String?
)