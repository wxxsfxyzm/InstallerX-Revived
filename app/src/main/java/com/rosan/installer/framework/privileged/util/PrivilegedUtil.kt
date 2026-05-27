// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.framework.privileged.util

import com.rosan.installer.domain.settings.model.config.Authorizer
import timber.log.Timber
import java.io.File

const val SHELL_ROOT = "su"
const val SHELL_SYSTEM = "su 1000"
const val SHELL_SH = "sh"

const val SU_ARGS = "-M"

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
