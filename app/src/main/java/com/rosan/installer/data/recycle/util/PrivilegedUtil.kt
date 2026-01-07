package com.rosan.installer.data.recycle.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.rosan.installer.data.recycle.model.impl.PrivilegedManager
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.util.OSUtils
import com.rosan.installer.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.io.File

const val SHELL_ROOT = "su"
const val SHELL_SYSTEM = "su 1000"
const val SHELL_SHELL = "su 2000"
const val SHELL_SH = "sh"

private const val PRIVILEGED_START_TIMEOUT_MS = 2500L

// Privileged Process is not Main Process, so we use Log.d instead of Timber
@Suppress("LogNotTimber")
fun deletePaths(paths: Array<out String>) {
    for (path in paths) {
        val file = File(path)

        Log.d("DELETE_PATH", "Processing path for deletion: $path")

        try {
            // Check if the file exists before attempting to delete.
            if (file.exists()) {
                // Call delete() and check its boolean return value.
                if (file.delete()) {
                    // This is the true success case.
                    Log.d("DELETE_PATH", "Successfully deleted file: $path")
                } else {
                    // The file existed, but deletion failed.
                    // This is a critical case to log as a warning.
                    Log.w(
                        "DELETE_PATH",
                        "Failed to delete file: $path. Check for permissions or if it is a non-empty directory."
                    )
                }
            } else {
                // If the file doesn't exist, it's already in the desired state. No error needed.
                Log.d("DELETE_PATH", "File does not exist, no action needed: $path")
            }
        } catch (e: SecurityException) {
            // Specifically catch permission errors. This is crucial for debugging.
            Log.e("DELETE_PATH", "SecurityException on deleting $path. Permission denied.", e)
        } catch (e: Exception) {
            // Catch any other unexpected errors during the process.
            Log.e("DELETE_PATH", "An unexpected error occurred while processing $path", e)
        }
    }
}

/**
 * Attempts to open an application with privileged rights, falling back to a standard intent if necessary.
 * Includes a timeout mechanism to prevent indefinite hangs.
 *
 * This function is designed to be called from a coroutine scope.
 *
 * @param context The Android context.
 * @param config The installer configuration containing the authorizer type.
 * @param packageName The package name of the app to open.
 * @param dhizukuAutoCloseSeconds The countdown in seconds for auto-closing the dialog when using Dhizuku.
 * @param onSuccess A lambda function to be executed after the app is launched and the calling UI should be closed.
 */
suspend fun openAppPrivileged(
    context: Context,
    config: ConfigEntity,
    packageName: String,
    dhizukuAutoCloseSeconds: Int,
    onSuccess: () -> Unit
) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        ?: return // Exit if no launch intent is found
    Timber.tag("HybridStart").i("Current UID: ${android.os.Process.myUid()}, isSystemApp: ${OSUtils.isSystemApp}")
    Timber.tag("HybridStart").i("Attempting privileged API start for $packageName...")

    var forceStartSuccess = false

    val shouldAttemptPrivileged = config.authorizer == ConfigEntity.Authorizer.Root ||
            config.authorizer == ConfigEntity.Authorizer.Shizuku ||
            (config.authorizer == ConfigEntity.Authorizer.None && OSUtils.isSystemApp)

    // Only attempt privileged start for Root or Shizuku
    if (shouldAttemptPrivileged) {
        // timeoutResult will be Boolean? (true/false on completion, or null on timeout)
        val timeoutResult = withTimeoutOrNull(PRIVILEGED_START_TIMEOUT_MS) {
            PrivilegedManager.startActivityPrivileged(config, intent)
        }

        // Check if the operation timed out.
        if (timeoutResult == null) {
            Timber.tag("HybridStart").w("Privileged API start timed out after ${PRIVILEGED_START_TIMEOUT_MS}ms.")
            context.toast("Privileged API start timed out, falling back...")
            // Explicitly set to false to ensure fallback logic is triggered on timeout.
            forceStartSuccess = false
        } else {
            // The call completed, use its boolean result
            forceStartSuccess = timeoutResult
            Timber.tag("HybridStart").d("startActivityPrivileged returned: $forceStartSuccess")
        }
    }

    if (forceStartSuccess) {
        // API Method succeeded, execute success action
        Timber.tag("HybridStart").i("Privileged API start succeeded for $packageName.")
        onSuccess()
    } else {
        // Use standard Android intent as fallback (also triggered on timeout)
        Timber.tag("HybridStart")
            .w("Privileged API start failed, timed out, or skipped. Falling back to standard Android intent.")
        // Switch to Main dispatcher for UI operations
        withContext(Dispatchers.Main) {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            if (config.authorizer == ConfigEntity.Authorizer.Dhizuku) {
                // Wait for the auto-close countdown only for Dhizuku
                delay(dhizukuAutoCloseSeconds * 1000L)
                Timber.tag("HybridStart").d(
                    "App $packageName not detected in foreground after $dhizukuAutoCloseSeconds seconds. Closing via success callback."
                )
            } else {
                Timber.tag("HybridStart").d("Other Authorizer's fallback fixed to 2.5s")
                delay(PRIVILEGED_START_TIMEOUT_MS)
            }
            onSuccess()
        }
    }
}

val InstallIntentFilter = IntentFilter().apply {
    addAction(Intent.ACTION_MAIN)
    addAction(Intent.ACTION_VIEW)
    @Suppress("Deprecation") addAction(Intent.ACTION_INSTALL_PACKAGE)
    addCategory(Intent.CATEGORY_DEFAULT)
    addDataScheme(ContentResolver.SCHEME_CONTENT)
    addDataScheme(ContentResolver.SCHEME_FILE)
    addDataType("application/vnd.android.package-archive")
}

/**
 * Helper to generate the special auth command (e.g. "su 1000") for Root mode.
 * This ensures different methods reuse the same 'su 1000' service process.
 */
fun getSpecialAuth(
    authorizer: ConfigEntity.Authorizer,
    specialAuth: String = SHELL_SYSTEM
): (() -> String?)? =
    if (authorizer == ConfigEntity.Authorizer.Root) {
        { specialAuth }
    } else null