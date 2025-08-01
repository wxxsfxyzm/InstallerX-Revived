package com.rosan.installer.data.app.model.entity

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
    NONE
}