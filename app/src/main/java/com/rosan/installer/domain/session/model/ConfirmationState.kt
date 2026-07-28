// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.session.model

import android.os.Process

data class ConfirmationRequest(
    val sessionId: Int,
    val requestType: ConfirmationRequestType,
    val callerUid: Int = Process.INVALID_UID
)

sealed interface ConfirmationState {
    data object Idle : ConfirmationState

    data class Resolving(
        val request: ConfirmationRequest
    ) : ConfirmationState

    data class AwaitingDecision(
        val details: ConfirmationDetails
    ) : ConfirmationState

    data class Submitting(
        val details: ConfirmationDetails,
        val granted: Boolean
    ) : ConfirmationState

    data class Completed(
        val sessionId: Int
    ) : ConfirmationState
}

fun ConfirmationState.sessionIdOrNull(): Int? = when (this) {
    ConfirmationState.Idle -> null
    is ConfirmationState.Resolving -> request.sessionId
    is ConfirmationState.AwaitingDecision -> details.sessionId
    is ConfirmationState.Submitting -> details.sessionId
    is ConfirmationState.Completed -> sessionId
}
