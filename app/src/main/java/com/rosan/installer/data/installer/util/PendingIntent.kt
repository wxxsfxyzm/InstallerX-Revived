package com.rosan.installer.data.installer.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import timber.log.Timber

private val defaultFlags =
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

/**
 * 创建用于广播的PendingIntent
 * @param context 上下文对象
 * @param requestCode 请求码
 * @param flags PendingIntent标志，默认为FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE
 * @return 创建的PendingIntent对象
 */
fun Intent.pendingBroadcast(
    context: Context,
    requestCode: Int,
    flags: Int = defaultFlags
): PendingIntent {
    Timber.d("pendingBroadcast: Creating broadcast PendingIntent")
    Timber.d("  - Action: ${this.action}")
    Timber.d("  - RequestCode: $requestCode")
    Timber.d("  - Flags: ${flagsToString(flags)}")
    return try {
        val result = PendingIntent.getBroadcast(context, requestCode, this, flags)
        Timber.d("  - Result: $result")
        result
    } catch (e: Exception) {
        Timber.e(e, "  - Error creating broadcast PendingIntent")
        throw e
    }
}

/**
 * 创建用于启动Activity的PendingIntent
 * @param context 上下文对象
 * @param requestCode 请求码
 * @param flags PendingIntent标志，默认为FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE
 * @return 创建的PendingIntent对象
 */
fun Intent.pendingActivity(
    context: Context,
    requestCode: Int,
    flags: Int = defaultFlags
): PendingIntent {
    Timber.d("pendingActivity: Creating activity PendingIntent")
    Timber.d("  - Action: ${this.action}")
    Timber.d("  - Component: ${this.component}")
    Timber.d("  - RequestCode: $requestCode")
    Timber.d("  - Flags: ${flagsToString(flags)}")
    return try {
        val result = PendingIntent.getActivity(context, requestCode, this, flags)
        Timber.d("  - Result: $result")
        result
    } catch (e: Exception) {
        Timber.e(e, "  - Error creating activity PendingIntent")
        throw e
    }
}

/**
 * 将PendingIntent标志转换为可读字符串
 * @param flags PendingIntent标志值
 * @return 可读的标志字符串
 */
private fun flagsToString(flags: Int): String {
    val flagList = mutableListOf<String>()
    if (flags and PendingIntent.FLAG_ONE_SHOT != 0) flagList.add("FLAG_ONE_SHOT")
    if (flags and PendingIntent.FLAG_NO_CREATE != 0) flagList.add("FLAG_NO_CREATE")
    if (flags and PendingIntent.FLAG_CANCEL_CURRENT != 0) flagList.add("FLAG_CANCEL_CURRENT")
    if (flags and PendingIntent.FLAG_UPDATE_CURRENT != 0) flagList.add("FLAG_UPDATE_CURRENT")
    if (flags and PendingIntent.FLAG_IMMUTABLE != 0) flagList.add("FLAG_IMMUTABLE")
    if (flags and PendingIntent.FLAG_MUTABLE != 0) flagList.add("FLAG_MUTABLE")
    
    val hexValue = String.format("0x%08X", flags)
    return if (flagList.isNotEmpty()) {
        "$hexValue (${flagList.joinToString(" | ")})"
    } else {
        hexValue
    }
}
