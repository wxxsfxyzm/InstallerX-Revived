package com.rosan.installer.data.installer.model.entity

import android.graphics.Bitmap

data class InstallConfirmInfo(
    val sessionId: Int,
    val appLabel: CharSequence,
    val appIcon: Bitmap?
)