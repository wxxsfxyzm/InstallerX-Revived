package com.rosan.installer.data.app.model.entity

import android.graphics.drawable.Drawable

sealed class AppEntity {
    abstract val packageName: String

    abstract val name: String

    abstract val targetSdk: String?

    abstract val minSdk: String?

    data class BaseEntity(
        override val packageName: String,
        val data: DataEntity,
        val versionCode: Long,
        val versionName: String,
        val label: String?,
        val icon: Drawable?,
        override val targetSdk: String?,
        override val minSdk: String?,
        // Get from AndroidManifest.xml
        val permissions: List<String>? = null,
    ) : AppEntity() {
        override val name = "base.apk"
    }

    data class SplitEntity(
        override val packageName: String,
        val data: DataEntity,
        val splitName: String,
        override val targetSdk: String?,
        override val minSdk: String?,
    ) : AppEntity() {
        override val name = "$splitName.apk"
    }

    data class DexMetadataEntity(
        override val packageName: String,
        val data: DataEntity,
        val dmName: String,
        override val targetSdk: String?,
        override val minSdk: String?,
    ) : AppEntity() {
        override val name = "base.dm"
    }

    data class CollectionEntity(
        val data: DataEntity
    ) : AppEntity() {
        // --- 修正部分开始 ---

        // 覆盖 AppEntity 的抽象属性
        override val packageName: String = "com.rosan.installer.collection.${System.nanoTime()}"
        override val name: String = "collection.zip" // 使用一个通用的名称
        override val targetSdk: String? = null
        override val minSdk: String? = null

        // 为UI提供类似于BaseEntity的属性，但使用硬编码/默认值
        // 这些是 CollectionEntity 自己的属性，不与父类冲突
        val label: String = "安装包集合"
        val versionCode: Long = -1
        val versionName: String = ""
        val icon: Drawable? = null

        // --- 修正部分结束 ---
    }
}