package com.rosan.installer.data.settings.model.datastore.entity

import kotlinx.serialization.Serializable

/**
 * 用于表示一个被管理的 Shared User ID。
 *
 * @property uidName 在 AndroidManifest.xml 中定义的实际 sharedUserId 字符串 (例如, "android.uid.system")。
 * @property uidValue 该 uidName 对应的整数 UID 值 (例如, 1000)。
 */
@Serializable
data class SharedUid(
    val uidName: String,
    val uidValue: Int
)