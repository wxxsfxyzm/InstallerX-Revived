package com.rosan.installer.domain.session.model

import android.graphics.Bitmap

data class ConfirmationDetails(
    val sessionId: Int,
    val appLabel: CharSequence,
    val appIcon: Bitmap?
)