package com.rosan.installer.data.installer.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent

private const val defaultFlags =
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

fun Intent.pendingBroadcast(
    context: Context,
    requestCode: Int,
    flags: Int = defaultFlags
): PendingIntent = PendingIntent.getBroadcast(context, requestCode, this, flags)

fun Intent.pendingActivity(
    context: Context,
    requestCode: Int,
    flags: Int = defaultFlags
): PendingIntent = PendingIntent.getActivity(context, requestCode, this, flags)
