package com.rosan.installer.data.installer.model.entity

import android.content.IntentSender

data class UnarchiveInfo(
    val packageName: String,
    val appLabel: String,
    val installerLabel: String, // 例如 "Google Play"
    val intentSender: IntentSender // 系统传递过来的回调，必须保存
)