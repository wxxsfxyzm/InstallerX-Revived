package com.rosan.installer.data.engine.executor

import android.content.Intent
import android.content.pm.PackageInstaller
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PackageManagerUtilTest {
    @Test
    fun `handlePendingUserAction starts launcher for pending status`() = runTest {
        val launched = mutableListOf<Intent>()
        val action = Intent("test.action.INSTALL_CONFIRM")

        val handled = PackageManagerUtil.handlePendingUserAction(
            status = PackageInstaller.STATUS_PENDING_USER_ACTION,
            action = action,
            activityStarter = PackageManagerUtil.ActivityStarter { intent ->
                launched += intent
            }
        )

        assertTrue(handled)
        assertEquals(listOf(action), launched)
    }

    @Test
    fun `handlePendingUserAction ignores non pending status`() = runTest {
        val launched = mutableListOf<Intent>()
        val action = Intent("test.action.INSTALL_CONFIRM")

        val handled = PackageManagerUtil.handlePendingUserAction(
            status = PackageInstaller.STATUS_SUCCESS,
            action = action,
            activityStarter = PackageManagerUtil.ActivityStarter { intent ->
                launched += intent
            }
        )

        assertFalse(handled)
        assertTrue(launched.isEmpty())
    }

    @Test
    fun `handlePendingUserAction ignores missing action intent`() = runTest {
        val launched = mutableListOf<Intent>()

        val handled = PackageManagerUtil.handlePendingUserAction(
            status = PackageInstaller.STATUS_PENDING_USER_ACTION,
            action = null,
            activityStarter = PackageManagerUtil.ActivityStarter { intent ->
                launched += intent
            }
        )

        assertFalse(handled)
        assertTrue(launched.isEmpty())
    }
}
