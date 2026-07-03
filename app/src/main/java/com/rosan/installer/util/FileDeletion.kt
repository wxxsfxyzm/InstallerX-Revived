// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.util

import timber.log.Timber
import java.io.File

private const val DELETE_TAG = "DELETE_PATH"

fun deletePaths(paths: List<String>) {
    for (path in paths) {
        val file = File(path)

        Timber.tag(DELETE_TAG).d("Processing path for deletion: $path")

        try {
            if (file.exists()) {
                if (file.deleteRecursively()) {
                    Timber.tag(DELETE_TAG).d("Successfully deleted: $path")
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
