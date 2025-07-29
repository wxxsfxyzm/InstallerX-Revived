package com.rosan.installer.data.recycle.util

import android.content.ContentResolver
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import java.io.File

// Privileged Process is not Main Process, so we use Log.d instead of Timber
@Suppress("LogNotTimber")
fun deletePaths(paths: Array<out String>) {
    for (path in paths) {
        val target = pathUnify(path)
        val file = File(target)

        Log.d("DELETE_PATH", "Processing path for deletion: $target")

        try {
            // Check if the file exists before attempting to delete.
            if (file.exists()) {
                // Call delete() and check its boolean return value.
                if (file.delete()) {
                    // This is the true success case.
                    Log.d("DELETE_PATH", "Successfully deleted file: $target")
                } else {
                    // The file existed, but deletion failed.
                    // This is a critical case to log as a warning.
                    Log.w(
                        "DELETE_PATH",
                        "Failed to delete file: $target. Check for permissions or if it is a non-empty directory."
                    )
                }
            } else {
                // If the file doesn't exist, it's already in the desired state. No error needed.
                Log.d("DELETE_PATH", "File does not exist, no action needed: $target")
            }
        } catch (e: SecurityException) {
            // Specifically catch permission errors. This is crucial for debugging.
            Log.e("DELETE_PATH", "SecurityException on deleting $target. Permission denied.", e)
        } catch (e: Exception) {
            // Catch any other unexpected errors during the process.
            Log.e("DELETE_PATH", "An unexpected error occurred while processing $target", e)
        }
    }
}

fun pathUnify(path: String): String =
    // 处理路径前缀，确保路径统一
    if (path.startsWith("/mnt/user/0"))
        path.replace("/mnt/user/0", "/storage")
    else path


val InstallIntentFilter = IntentFilter().apply {
    addAction(Intent.ACTION_MAIN)
    addAction(Intent.ACTION_VIEW)
    addAction(Intent.ACTION_INSTALL_PACKAGE)
    addCategory(Intent.CATEGORY_DEFAULT)
    addDataScheme(ContentResolver.SCHEME_CONTENT)
    addDataScheme(ContentResolver.SCHEME_FILE)
    addDataType("application/vnd.android.package-archive")
}
