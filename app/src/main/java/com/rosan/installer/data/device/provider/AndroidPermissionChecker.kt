// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.device.provider

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import com.rosan.installer.domain.device.model.PermissionType
import com.rosan.installer.domain.device.provider.PermissionChecker

/**
 * Implementation of PermissionChecker using Android framework APIs.
 */
class AndroidPermissionChecker(
    private val context: Context
) : PermissionChecker {

    override fun hasPermission(type: PermissionType): Boolean {
        return when (type) {
            PermissionType.NOTIFICATION -> checkNotificationPermission()
            PermissionType.STORAGE -> checkStoragePermission()
        }
    }

    private fun checkNotificationPermission(): Boolean {
        // Notification permission is only required on Android 13 (TIRAMISU) and above.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            // Legacy storage permission for Android 10 (Q) and below.
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
