// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.session.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConfirmationStateTest {
    @Test
    fun `reports the platform session represented by each state`() {
        val request = ConfirmationRequest(
            sessionId = 37,
            requestType = ConfirmationRequestType.INSTALL,
            callerUid = 10001
        )
        val details = ConfirmationDetails(
            sessionId = 37,
            appLabel = "Example",
            appIcon = null
        )

        assertNull(ConfirmationState.Idle.sessionIdOrNull())
        assertEquals(37, ConfirmationState.Resolving(request).sessionIdOrNull())
        assertEquals(37, ConfirmationState.AwaitingDecision(details).sessionIdOrNull())
        assertEquals(37, ConfirmationState.Submitting(details, true).sessionIdOrNull())
        assertEquals(37, ConfirmationState.Completed(37).sessionIdOrNull())
    }
}
