// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.util.cache

import android.content.Context
import com.rosan.installer.util.timber.FileLoggingTree.Companion.LOG_DIR_NAME
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

class AutoCacheSweeper(private val context: Context) {
    private val processStartTime = System.currentTimeMillis()

    init {
        sweep()
    }

    private fun sweep() {
        CoroutineScope(Dispatchers.IO).launch {
            Timber.d("Starting automatic cache sweep via Koin...")
            try {
                val paths = listOfNotNull(
                    context.cacheDir,
                    context.externalCacheDir
                )

                val blacklistExtensions = listOf(".lck", ".lock")
                val blacklistNames = listOf(LOG_DIR_NAME)

                fun clearFileSafe(file: File): Boolean {
                    if (!file.exists()) return true
                    if (blacklistNames.contains(file.name)) return false
                    if (blacklistExtensions.any { file.name.endsWith(it) }) return false

                    if (file.lastModified() >= processStartTime) {
                        return false
                    }

                    if (file.isDirectory) {
                        val children = file.listFiles()
                        var allChildrenDeleted = true
                        children?.forEach { child ->
                            if (!clearFileSafe(child)) {
                                allChildrenDeleted = false
                            }
                        }
                        if (!allChildrenDeleted) return false
                    }
                    return file.delete()
                }

                var totalCleared = 0
                paths.forEach { root ->
                    root.listFiles()?.forEach { child ->
                        if (clearFileSafe(child)) totalCleared++
                    }
                }
                Timber.d("Automatic cache sweep completed. Cleared items: $totalCleared")
            } catch (e: Exception) {
                Timber.e(e, "Error occurred during automatic cache sweep")
            }
        }
    }
}
