package com.rosan.installer.data.app.model.entity

import android.graphics.drawable.Drawable
import com.rosan.installer.build.Architecture

sealed class AppEntity {
    abstract val packageName: String
    abstract val name: String
    abstract val data: DataEntity
    abstract val targetSdk: String?
    abstract val minSdk: String?
    abstract val arch: Architecture?

    // each app entity may have a container type, such as a split APK or a collection
    abstract val containerType: DataType?

    data class BaseEntity(
        override val packageName: String,
        val sharedUserId: String?,
        override val data: DataEntity,
        val versionCode: Long,
        val versionName: String,
        val label: String?,
        val icon: Drawable?,
        override val name: String = "base.apk",
        override val targetSdk: String?,
        override val minSdk: String?,
        // Only available for oppo apk
        val minOsdkVersion: String? = null,
        override val arch: Architecture? = null,
        override val containerType: DataType? = null,
        // Get from AndroidManifest.xml
        val permissions: List<String>? = null,
        val signatureHash: String? = null,
        val fileHash: String? = null
    ) : AppEntity()

    data class SplitEntity(
        override val packageName: String,
        override val data: DataEntity,
        val splitName: String,
        override val targetSdk: String?,
        override val minSdk: String?,
        override val arch: Architecture?,
        override val containerType: DataType? = null,
    ) : AppEntity() {
        override val name = "$splitName.apk"
    }

    data class DexMetadataEntity(
        override val packageName: String,
        override val data: DataEntity,
        val dmName: String,
        override val targetSdk: String?,
        override val minSdk: String?,
        override val arch: Architecture? = null,
        override val containerType: DataType? = null,
    ) : AppEntity() {
        override val name = "base.dm"
    }

    data class CollectionEntity(
        override val packageName: String = "com.rosan.installer.collection.${System.nanoTime()}",
        override val data: DataEntity,
        override val targetSdk: String? = null,
        override val minSdk: String? = null,
        override val arch: Architecture? = null,
        override val containerType: DataType? = null,
        val label: String = "Collection of APKs",
        val versionCode: Long = -1,
        val versionName: String = "",
    ) : AppEntity() {
        override val name: String = "collection.zip" // 使用一个通用的名称
    }

    data class ModuleEntity(
        val id: String,
        override val name: String,
        val version: String,
        val versionCode: Long,
        val author: String,
        val description: String,
        override val data: DataEntity,
        override val containerType: DataType? = null
    ) : AppEntity() {
        override val packageName: String
            get() = id
        override val targetSdk: String? = null
        override val minSdk: String? = null
        override val arch: Architecture? = null
    }
}