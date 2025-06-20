package com.rosan.installer.data.recycle.util

import android.content.ContentResolver
import android.content.Intent
import android.content.IntentFilter
import timber.log.Timber
import java.io.File

fun Array<out String>.delete() {
    for (path in this) {
        Timber.tag("DELETE_PATH").d("path: $path")
        val file = File(
            if (path.startsWith("/mnt/user/0/emulated/0")) {
                path.replace("/mnt/user/0/emulated/0", "/storage/emulated/0")
            } else path
        )
        Timber.tag("DELETE_PATH").d("file: $file")
        if (file.exists()) file.delete()
    }
}

val InstallIntentFilter = IntentFilter().apply {
    addAction(Intent.ACTION_MAIN)
    addAction(Intent.ACTION_VIEW)
    addAction(Intent.ACTION_INSTALL_PACKAGE)
    addCategory(Intent.CATEGORY_DEFAULT)
    addDataScheme(ContentResolver.SCHEME_CONTENT)
    addDataScheme(ContentResolver.SCHEME_FILE)
    addDataType("application/vnd.android.package-archive")
}
