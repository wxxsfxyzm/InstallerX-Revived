// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.util

import android.content.Context
import android.provider.MediaStore
import java.io.File
import timber.log.Timber

private const val DELETE_TAG = "DELETE_PATH"

fun deletePaths(context: Context, paths: List<String>) {
    for (path in paths) {
        val file = File(path)

        Timber.tag(DELETE_TAG).d("Processing path for deletion: $path")

        try {
            if (file.exists()) {
                if (file.deleteRecursively()) {
                    Timber.tag(DELETE_TAG).d("Successfully deleted: $path")
                    removeFromMediaStore(context, file.absolutePath)
                } else {
                    Timber.tag(DELETE_TAG).w("Failed to delete: $path. Check for permissions or lock issues.")
                }
            } else {
                Timber.tag(DELETE_TAG).d("File/Directory does not exist, no action needed: $path")
            }
        } catch (e: SecurityException) {
            Timber.tag(DELETE_TAG).e(e, "SecurityException on deleting $path. Permission denied.")
        } catch (e: Exception) {
            Timber.tag(DELETE_TAG).e(e, "An unexpected error occurred while processing $path")
        }
    }
}

private fun removeFromMediaStore(context: Context, path: String) {
    try {
        val deletedRows = context.contentResolver.delete(
            MediaStore.Files.getContentUri("external"),
            "_data=?",
            arrayOf(path),
        )
        Timber.tag(DELETE_TAG).d("Removed $deletedRows MediaStore row(s) for: $path")
    } catch (e: SecurityException) {
        // The physical deletion has already succeeded. MediaStore cleanup is best effort because
        // it may be denied for a path owned by another provider or user on some Android builds.
        Timber.tag(DELETE_TAG).w(e, "Unable to remove deleted path from MediaStore: $path")
    } catch (e: Exception) {
        Timber.tag(DELETE_TAG).w(e, "MediaStore cleanup failed for: $path")
    }
}
