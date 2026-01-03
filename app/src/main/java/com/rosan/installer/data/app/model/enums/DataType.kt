package com.rosan.installer.data.app.model.enums

import kotlinx.serialization.Serializable

@Serializable
enum class DataType {
    APK,
    APKS,
    APKM,
    XAPK,
    MULTI_APK,
    MULTI_APK_ZIP,
    MODULE_ZIP,
    MIXED_MODULE_APK,
    MIXED_MODULE_ZIP,
    NONE
}