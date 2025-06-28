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
        Log.d("DELETE_PATH", "path: $path")
        println("Deleting path: $path")
        val file = File(path)
        Log.d("DELETE_PATH", "file: $file")
        if (file.exists()) {
            Log.d("DELETE_PATH", "Deleting file: $file")
            println("Deleting file: $file")
            file.delete()
        }
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
