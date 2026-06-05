// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.util

import android.content.Context
import android.net.Uri

fun Context.readBackupText(uri: Uri): String =
    contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        ?: error("Unable to open backup file.")

fun Context.writeBackupText(uri: Uri, content: String) {
    contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(content) }
        ?: error("Unable to open backup destination.")
}
