package com.rosan.installer.domain.session.model

import android.graphics.Bitmap

data class ConfirmationDetails(
    val sessionId: Int,
    val appLabel: CharSequence,
    val appIcon: Bitmap?,
    val packageName: String = "",
    val isUpdate: Boolean = false,
    val isOwnershipConflict: Boolean = false,
    // A generic label for the related app (either the update owner or the initiator)
    val sourceAppLabel: CharSequence? = null,
    val installerPackageName: String? = null,

    // Flag to determine if this confirmation belongs to our own active installation
    val isSelfSession: Boolean = false,
    // Preserve installing progress for multi install interruption
    val currentProgress: Int = 1,
    val totalProgress: Int = 1
)
