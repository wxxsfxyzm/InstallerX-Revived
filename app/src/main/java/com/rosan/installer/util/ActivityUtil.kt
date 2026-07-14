// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.util

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle

/**
 * Returns the platform-recorded launch referrer without trusting caller-controlled extras.
 *
 * [Activity.getReferrer] checks intent extras before the framework-provided referrer, so remove
 * those spoofable values temporarily and restore the original intent before returning.
 */
fun Activity.platformLaunchReferrer(): Uri? {
    val originalExtras = intent.extras?.let(::Bundle)

    return try {
        intent.removeExtra(Intent.EXTRA_REFERRER_NAME)
        intent.removeExtra(Intent.EXTRA_REFERRER)
        referrer
    } finally {
        intent.replaceExtras(originalExtras)
    }
}
