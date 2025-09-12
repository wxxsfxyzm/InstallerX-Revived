package com.rosan.installer.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.net.toUri

fun Context.openUrl(url: String) =
    try {
        this.startActivity(
            Intent(Intent.ACTION_VIEW, url.toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: Exception) { // Catch ActivityNotFoundException
        e.printStackTrace()
        // 可以在这里添加一些用户友好的提示，例如：
        // Toast.makeText(context, "无法打开链接，请检查URL或安装浏览器", Toast.LENGTH_SHORT).show()
    }

fun Context.toast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, text, duration).show()
}

fun Context.toast(@StringRes resId: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, resId, duration).show()
}