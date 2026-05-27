package com.rosan.installer.domain.engine.model.packageinfo

import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable

data class InstalledAppInfo(
    val packageName: String,
    val icon: Drawable?,
    val label: String,
    val versionCode: Long,
    val versionName: String,
    val applicationInfo: ApplicationInfo?,
    val minSdk: Int?,
    val targetSdk: Int?,
    val signatureHash: String? = null,
    val isSystemApp: Boolean = false,
    val isUninstalled: Boolean = false,
    val isArchived: Boolean = false,
    val packageSize: Long = 0L,
    val sourceDir: String? = null
)
