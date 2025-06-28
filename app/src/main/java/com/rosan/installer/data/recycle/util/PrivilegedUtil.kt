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
        val target = pathUnify(path)
        Log.d("DELETE_PATH", "path: $target")
        println("Deleting path: $target")
        val file = File(target)
        Log.d("DELETE_PATH", "file: $file")
        if (file.exists()) {
            Log.d("DELETE_PATH", "Deleting file: $file")
            println("Deleting file: $file")
            file.delete()
        }
    }
}

fun pathUnify(path: String): String =
    // 处理路径前缀，确保路径统一
    if (path.startsWith("/mnt/user/0")) {
        path.replace("/mnt/user/0", "/storage")
    } else path


val InstallIntentFilter = IntentFilter().apply {
    addAction(Intent.ACTION_MAIN)
    addAction(Intent.ACTION_VIEW)
    addAction(Intent.ACTION_INSTALL_PACKAGE)
    addCategory(Intent.CATEGORY_DEFAULT)
    addDataScheme(ContentResolver.SCHEME_CONTENT)
    addDataScheme(ContentResolver.SCHEME_FILE)
    addDataType("application/vnd.android.package-archive")
}
