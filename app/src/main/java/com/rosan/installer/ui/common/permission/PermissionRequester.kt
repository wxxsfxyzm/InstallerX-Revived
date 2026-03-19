// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.common.permission

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.rosan.installer.domain.device.model.PermissionType
import com.rosan.installer.domain.device.provider.PermissionChecker
import timber.log.Timber

/**
 * A UI-layer helper class to manage runtime permissions requests.
 * It delegates permission state checks to the domain layer (PermissionChecker)
 * and focuses entirely on handling ActivityResultLaunchers and UI intents.
 *
 * @param activity The ComponentActivity that is requesting the permissions.
 * @param permissionChecker The domain provider for checking permission states.
 */
class PermissionRequester(
    private val activity: ComponentActivity,
    private val permissionChecker: PermissionChecker
) {
    /**
     * Callback triggered before launching a system settings activity.
     */
    var onBeforeLaunchSettings: (() -> Unit)? = null
    private var onPermissionsGranted: (() -> Unit)? = null
    private var onPermissionsDenied: ((type: PermissionType) -> Unit)? = null

    // Launcher for Notification permission.
    private val requestNotificationLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Timber.d("Notification permission granted.")
                checkStoragePermissionAndProceed()
            } else {
                Timber.w("Notification permission was denied by the user.")
                onPermissionsDenied?.invoke(PermissionType.NOTIFICATION)
            }
        }

    // Launcher for Legacy Storage permission.
    private val requestLegacyStorageLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Timber.d("Storage permission GRANTED from user.")
                onPermissionsGranted?.invoke()
            } else {
                Timber.d("Storage permission DENIED from user.")
                onPermissionsDenied?.invoke(PermissionType.STORAGE)
            }
        }

    // Launcher for Scoped Storage settings (Android 11+).
    private val requestStorageSettingsLauncher =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // Delegate the re-check to the pure domain logic.
            if (permissionChecker.hasPermission(PermissionType.STORAGE)) {
                Timber.d("Storage permission GRANTED after returning from settings.")
                onPermissionsGranted?.invoke()
            } else {
                Timber.d("Storage permission DENIED after returning from settings.")
                onPermissionsDenied?.invoke(PermissionType.STORAGE)
            }
        }

    /**
     * Starts the permission request flow.
     */
    fun requestEssentialPermissions(onGranted: () -> Unit, onDenied: (type: PermissionType) -> Unit) {
        this.onPermissionsGranted = onGranted
        this.onPermissionsDenied = onDenied
        checkNotificationPermissionAndProceed()
    }

    private fun checkNotificationPermissionAndProceed() {
        if (permissionChecker.hasPermission(PermissionType.NOTIFICATION)) {
            Timber.d("Notification permission already granted or not required.")
            checkStoragePermissionAndProceed()
        } else {
            // Only request via launcher if the device API requires it (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Timber.d("Requesting notification permission.")
                requestNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                checkStoragePermissionAndProceed()
            }
        }
    }

    private fun checkStoragePermissionAndProceed() {
        if (permissionChecker.hasPermission(PermissionType.STORAGE)) {
            Timber.d("Storage permission already granted. Proceeding.")
            onPermissionsGranted?.invoke()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                handleStoragePermissionForAndroid11AndAbove()
            } else {
                Timber.d("Requesting legacy storage permission.")
                requestLegacyStorageLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun handleStoragePermissionForAndroid11AndAbove() {
        Timber.d("Requesting storage permission by opening settings.")

        var intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = "package:${activity.packageName}".toUri()
        }

        if (intent.resolveActivity(activity.packageManager) == null) {
            Timber.w("No activity found for app-specific storage settings. Falling back to general settings.")
            intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        }

        if (intent.resolveActivity(activity.packageManager) != null) {
            try {
                onBeforeLaunchSettings?.invoke()
                requestStorageSettingsLauncher.launch(intent)
            } catch (e: Exception) {
                Timber.e(e, "Error launching storage permission settings.")
                onPermissionsDenied?.invoke(PermissionType.STORAGE)
            }
        } else {
            Timber.e("No activity found to handle any storage permission settings.")
            onPermissionsDenied?.invoke(PermissionType.STORAGE)
        }
    }
}
