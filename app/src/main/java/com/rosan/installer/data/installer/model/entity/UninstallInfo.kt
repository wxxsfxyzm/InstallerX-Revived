package com.rosan.installer.data.installer.model.entity

import android.graphics.drawable.Drawable

data class UninstallInfo(
    val packageName: String,
    val appLabel: String? = null,
    val versionName: String? = null,
    val versionCode: Long? = null,
    val appIcon: Drawable? = null
)