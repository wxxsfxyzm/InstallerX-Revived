package com.rosan.installer.data.installer.util

import android.net.Uri
import timber.log.Timber

fun String.pathUnify(): String =
// Handle paths starting with /mnt/user/0
// This is necessary for compatibility with Android 14+ where /storage is used instead of
// /mnt/user/0 for the primary shared storage directory. Particularly, Samsung devices
    // shell id 2000 only has access to /storage/emulated/0
    if (this.startsWith("/mnt/user/0"))
        this.replace("/mnt/user/0", "/storage")
    else this

fun String.getRealPathFromUri(uri: Uri): String {
    if (!this.startsWith("/mnt/appfuse"))
        return this

    val realPath = uri.path

    if (realPath == null) {
        Timber.e("Real path is null for URI: $uri")
        return this
    } else {
        Timber.d("Real Path exists, returning: $realPath")
        return realPath
    }

}