package com.rosan.installer.ui.util

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import timber.log.Timber

/**
 * Enum to specify the reason for permission denial.
 */
enum class PermissionDenialReason {
    NOTIFICATION,
    STORAGE
}

/**
 * A helper class to manage runtime permissions for the application.
 * It handles essential notification and storage permissions.
 *
 * @param activity The ComponentActivity that is requesting the permissions.
 */
class PermissionManager(private val activity: ComponentActivity) {
    /**
     * Callback triggered before launching a system settings activity.
     * Useful for handling lifecycle events in the parent activity.
     */
    var onBeforeLaunchSettings: (() -> Unit)? = null
    private var onPermissionsGranted: (() -> Unit)? = null
    private var onPermissionsDenied: ((reason: PermissionDenialReason) -> Unit)? = null

    // Launcher for Notification permission (Android 13+).
    private val requestNotificationPermissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Timber.d("Notification permission granted.")
                // Proceed to the essential storage permission check.
                checkStoragePermissionAndProceed()
            } else {
                Timber.w("Notification permission was denied by the user.")
                // Stop the flow and report that notification permission was denied.
                onPermissionsDenied?.invoke(PermissionDenialReason.NOTIFICATION)
            }
        }

    // Launcher for Storage permission (Android 10 and below).
    private val requestStoragePermissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Timber.d("Storage permission GRANTED from user.")
                onPermissionsGranted?.invoke()
            } else {
                Timber.d("Storage permission DENIED from user.")
                onPermissionsDenied?.invoke(PermissionDenialReason.STORAGE)
            }
        }

    // Launcher for Storage permission (Android 11 and above).
    private val requestStoragePermissionLauncherFromSettings =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // After returning from settings, re-check the permission status.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Timber.d("Storage permission GRANTED after returning from settings.")
                    onPermissionsGranted?.invoke()
                } else {
                    Timber.d("Storage permission DENIED after returning from settings.")
                    onPermissionsDenied?.invoke(PermissionDenialReason.STORAGE)
                }
            }
        }

    /**
     * Starts the permission request flow.
     * The final callbacks are triggered based on which permission is granted or denied.
     *
     * @param onGranted Callback invoked when all essential permissions are granted.
     * @param onDenied Callback invoked with a reason if any essential permission is denied.
     */
    fun requestEssentialPermissions(onGranted: () -> Unit, onDenied: (reason: PermissionDenialReason) -> Unit) {
        this.onPermissionsGranted = onGranted
        this.onPermissionsDenied = onDenied
        checkNotificationPermissionAndProceed()
    }

    private fun checkNotificationPermissionAndProceed() {
        // Notification permission is only required on Android 13 (TIRAMISU) and above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Timber.d("Notification permission already granted.")
                checkStoragePermissionAndProceed()
            } else {
                Timber.d("Requesting notification permission.")
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            Timber.d("No notification permission needed for this API level.")
            checkStoragePermissionAndProceed()
        }
    }

    private fun checkStoragePermissionAndProceed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            handleStoragePermissionForAndroid11AndAbove()
        } else {
            // Handle legacy storage permission for Android 10 (Q) and below.
            // On Android 10, this permission is still functional if the app has opted-out
            // of scoped storage by setting `android:requestLegacyExternalStorage="true"` in the manifest.
            // Given the app's wide range of supported APIs, this flag is presumed to be set.
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Timber.d("Storage permission already granted. Proceeding.")
                onPermissionsGranted?.invoke()
            } else {
                Timber.d("Requesting legacy storage permission.")
                requestStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    /**
     * Handles the logic for MANAGE_EXTERNAL_STORAGE permission on Android 11 (R) and above.
     * This method is annotated to inform the lint checker that it's only called on appropriate API levels.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun handleStoragePermissionForAndroid11AndAbove() {
        if (Environment.isExternalStorageManager()) {
            Timber.d("Storage permission already granted. Proceeding.")
            onPermissionsGranted?.invoke()
        } else {
            Timber.d("Requesting storage permission by opening settings.")

            // Start with the specific intent for the app.
            var intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = "package:${activity.packageName}".toUri()
            }

            // If the specific intent can't be resolved, create a fallback to the general settings page.
            if (intent.resolveActivity(activity.packageManager) == null) {
                Timber.w("No activity found for app-specific storage settings. Falling back to general settings.")
                intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            }

            // Final check to see if any activity can handle the determined intent.
            if (intent.resolveActivity(activity.packageManager) != null) {
                try {
                    // Notify that we are about to leave the app
                    onBeforeLaunchSettings?.invoke()
                    requestStoragePermissionLauncherFromSettings.launch(intent)
                } catch (e: Exception) {
                    Timber.e(e, "Error launching storage permission settings.")
                    onPermissionsDenied?.invoke(PermissionDenialReason.STORAGE)
                }
            } else {
                Timber.e("No activity found to handle any storage permission settings.")
                onPermissionsDenied?.invoke(PermissionDenialReason.STORAGE)
            }
        }
    }
}

