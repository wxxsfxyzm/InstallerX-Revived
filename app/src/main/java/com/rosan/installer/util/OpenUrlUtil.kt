package com.rosan.installer.util

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) { // 可以捕获 ActivityNotFoundException
        e.printStackTrace()
        // 可以在这里添加一些用户友好的提示，例如：
        // Toast.makeText(context, "无法打开链接，请检查URL或安装浏览器", Toast.LENGTH_SHORT).show()
    }
}