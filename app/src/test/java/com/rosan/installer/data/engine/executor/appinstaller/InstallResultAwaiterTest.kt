// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.executor.appinstaller

import android.content.pm.PackageInstaller
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest

class InstallResultAwaiterTest {
    @Test
    fun `approval waits for the final success callback`() = runTest {
        val results = ArrayDeque(listOf(PackageInstaller.STATUS_PENDING_USER_ACTION, PackageInstaller.STATUS_SUCCESS))
        val events = mutableListOf<String>()
        val result = awaitInstallResult(
            receive = {
                events.add("receive")
                results.removeFirst()
            },
            status = { it },
            onPendingUserAction = { events.add("approve") },
        )
        assertEquals(PackageInstaller.STATUS_SUCCESS, result)
        assertEquals(listOf("receive", "approve", "receive"), events)
    }

    @Test
    fun `failure after approval remains a failure`() = runTest {
        val results = ArrayDeque(listOf(PackageInstaller.STATUS_PENDING_USER_ACTION, PackageInstaller.STATUS_FAILURE_BLOCKED))
        assertEquals(
            PackageInstaller.STATUS_FAILURE_BLOCKED,
            awaitInstallResult({ results.removeFirst() }, { it }, {}),
        )
    }

    @Test
    fun `terminal results never request approval`() = runTest {
        for (status in listOf(PackageInstaller.STATUS_SUCCESS, PackageInstaller.STATUS_FAILURE)) {
            assertEquals(status, awaitInstallResult({ status }, { it }, { error("Unexpected approval") }))
        }
    }

    @Test
    fun `approval errors propagate without waiting for another result`() = runTest {
        var receives = 0
        assertFailsWith<SecurityException> {
            awaitInstallResult(
                receive = {
                    receives++
                    PackageInstaller.STATUS_PENDING_USER_ACTION
                },
                status = { it },
                onPendingUserAction = { throw SecurityException("Approval denied") },
            )
        }
        assertEquals(1, receives)
    }

    @Test
    fun `cancellation while awaiting final result propagates`() = runTest {
        var receives = 0
        assertFailsWith<CancellationException> {
            awaitInstallResult(
                receive = {
                    if (receives++ == 0) {
                        PackageInstaller.STATUS_PENDING_USER_ACTION
                    } else {
                        throw CancellationException("Installation cancelled")
                    }
                },
                status = { it },
                onPendingUserAction = {},
            )
        }
        assertEquals(2, receives)
    }
}
