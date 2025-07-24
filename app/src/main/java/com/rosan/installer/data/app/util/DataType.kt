package com.rosan.installer.data.app.util

import kotlinx.serialization.Serializable


@Serializable
enum class DataType {
    APK,
    APKS,
    APKM,
    XAPK,
    MULTI_APK_ZIP,
    MODULE_ZIP
}
