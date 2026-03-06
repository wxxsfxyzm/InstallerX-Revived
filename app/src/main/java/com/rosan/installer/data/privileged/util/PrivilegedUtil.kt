// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.data.privileged.util

import android.content.ContentResolver
import android.content.Intent
import android.content.IntentFilter
import com.rosan.installer.domain.settings.model.Authorizer
import timber.log.Timber
import java.io.File

const val SHELL_ROOT = "su"
const val SHELL_SYSTEM = "su 1000"
const val SHELL_SH = "sh"

const val SU_ARGS = "-M"

private const val DELETE_TAG = "DELETE_PATH"


fun deletePaths(paths: Array<out String>) {
    for (path in paths) {
        val file = File(path)

        Timber.tag(DELETE_TAG).d("Processing path for deletion: $path")

        try {
            // Check if the file exists before attempting to delete.
            if (file.exists()) {
                // Call delete() and check its boolean return value.
                if (file.delete()) {
                    // This is the true success case.
                    Timber.tag(DELETE_TAG).d("Successfully deleted file: $path")
                } else {
                    // The file existed, but deletion failed.
                    // This is a critical case to log as a warning.
                    Timber.tag(DELETE_TAG).w("Failed to delete file: $path. Check for permissions or if it is a non-empty directory.")
                }
            } else {
                // If the file doesn't exist, it's already in the desired state. No error needed.
                Timber.tag(DELETE_TAG).d("File does not exist, no action needed: $path")
            }
        } catch (e: SecurityException) {
            // Specifically catch permission errors. This is crucial for debugging.
            Timber.tag(DELETE_TAG).e(e, "SecurityException on deleting $path. Permission denied.")
        } catch (e: Exception) {
            // Catch any other unexpected errors during the process.
            Timber.tag(DELETE_TAG).e(e, "An unexpected error occurred while processing $path")
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
    authorizer: Authorizer,
    specialAuth: String = SHELL_SYSTEM
): (() -> String?)? =
    if (authorizer == Authorizer.Root) {
        { specialAuth }
    } else null
